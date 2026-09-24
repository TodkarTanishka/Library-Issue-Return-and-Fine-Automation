package server;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ServerMetrics {
    private final AtomicLong totalRequests=new AtomicLong();
    private final AtomicLong failedRequests=new AtomicLong();
    private final AtomicInteger activeConnections=new AtomicInteger();
    private final AtomicInteger peakConcurrentUsers=new AtomicInteger();
    private final AtomicLong totalLatencyMs=new AtomicLong();
    private final AtomicLong maxLatencyMs=new AtomicLong();
    private final ConcurrentLinkedQueue<Long> latencySamples=new ConcurrentLinkedQueue<>();
    private final AtomicLong lastPrintedRequests=new AtomicLong();

    public long beginRequest(){
        totalRequests.incrementAndGet();
        int active=activeConnections.incrementAndGet();
        peakConcurrentUsers.accumulateAndGet(active, Math::max);
        return System.nanoTime();
    }
    public void endRequest(long start, boolean failed){
        long ms=Math.max(0,(System.nanoTime()-start)/1_000_000L);
        totalLatencyMs.addAndGet(ms);
        maxLatencyMs.accumulateAndGet(ms, Math::max);
        latencySamples.add(ms);
        while(latencySamples.size()>10000) latencySamples.poll();
        if(failed) failedRequests.incrementAndGet();
        activeConnections.decrementAndGet();
        long n=totalRequests.get();
        if(n>0 && n%25==0 && lastPrintedRequests.getAndSet(n)!=n) printReport("LIVE");
    }
    public long getTotalRequests(){return totalRequests.get();}
    public long getFailedRequests(){return failedRequests.get();}
    public int getActiveConnections(){return activeConnections.get();}
    public int getPeakConcurrentUsers(){return peakConcurrentUsers.get();}
    public double getAverageLatencyMs(){long n=totalRequests.get();return n==0?0.0:(double)totalLatencyMs.get()/n;}
    public long getMaxLatencyMs(){return maxLatencyMs.get();}
    public double getP95LatencyMs(){
        List<Long> a=new ArrayList<>(latencySamples); if(a.isEmpty()) return 0.0;
        Collections.sort(a); int idx=(int)Math.ceil(a.size()*0.95)-1; return a.get(Math.max(0,idx));
    }
    public void printReport(String reason){
        long n=totalRequests.get(); long failed=failedRequests.get();
        double failure=n==0?0.0:(failed*100.0/n);
        System.out.println("\n========== MMCOE LIBRARY KPI (SERVER) ["+reason+"] ==========");
        System.out.println("Total TCP Requests       : "+n);
        System.out.println("Failed TCP Requests      : "+failed);
        System.out.printf ("Failure Rate             : %.2f%%\n",failure);
        System.out.printf ("Average Response Time    : %.2f ms\n",getAverageLatencyMs());
        System.out.printf ("P95 Response Time        : %.2f ms\n",getP95LatencyMs());
        System.out.println("Maximum Response Time    : "+getMaxLatencyMs()+" ms");
        System.out.println("Active Connections       : "+getActiveConnections());
        System.out.println("Peak Concurrent Users    : "+getPeakConcurrentUsers());
        System.out.println("===========================================================\n");
    }

    public Map<String, Object> getMetricsSummaryMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalRequests", getTotalRequests());
        m.put("failedRequests", getFailedRequests());
        m.put("activeConnections", getActiveConnections());
        m.put("peakConcurrentUsers", getPeakConcurrentUsers());
        m.put("avgLatencyMs", Math.round(getAverageLatencyMs() * 100.0) / 100.0);
        m.put("p95LatencyMs", Math.round(getP95LatencyMs() * 100.0) / 100.0);
        m.put("maxLatencyMs", getMaxLatencyMs());
        return m;
    }
}
