"""
MMCOE Library KPI validator.
Run while TCP server + HTTP gateway are running:
    python tools/kpi_validate.py

It measures:
- HTTP/TCP-facing response latency
- concurrent request throughput
- failure rate
- peak concurrency reported by server
- issue-request workflow KPIs returned by /api/stats

This is a validation/load test, not a production benchmark.
"""
import concurrent.futures, json, statistics, time, urllib.parse, urllib.request

BASE="http://localhost:8080"

def get(path, params=None):
    url=BASE+path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    t=time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            body=r.read().decode()
            return (time.perf_counter()-t)*1000, r.status, json.loads(body)
    except Exception as e:
        return (time.perf_counter()-t)*1000, 0, {"success":False,"error":str(e)}

def main():
    print("\n=== MMCOE LIBRARY KPI VALIDATION ===")
    samples=50
    workers=10
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as ex:
        rows=list(ex.map(lambda _: get("/api/books", {"search":"Java","department":"all"}), range(samples)))
    lat=[r[0] for r in rows]
    ok=sum(1 for r in rows if r[1]==200 and r[2].get("success",False))
    failed=samples-ok
    lat_sorted=sorted(lat)
    p95=lat_sorted[max(0,int(samples*.95)-1)]
    print(f"Requests tested       : {samples}")
    print(f"Workers               : {workers}")
    print(f"Successful            : {ok}")
    print(f"Failed                : {failed}")
    print(f"Failure rate          : {failed/samples*100:.2f}%")
    print(f"Average latency       : {statistics.mean(lat):.2f} ms")
    print(f"P95 latency           : {p95:.2f} ms")
    print(f"Min / Max latency     : {min(lat):.2f} / {max(lat):.2f} ms")
    print(f"Throughput            : {samples/(sum(lat)/1000):.2f} req/s (client-side approximation)")

    status=get("/api/stats")[2]
    s=status.get("stats",{})
    print("\n--- SERVER KPI OUTPUT ---")
    for k in [
        "totalTcpRequests","failedTcpRequests","activeConnections","peakConcurrentUsers",
        "averageResponseMs","maxResponseMs","issueRequests","pendingIssueRequests",
        "approvedRequests","rejectedRequests","approvalRatePercent",
        "rejectionRatePercent","averageApprovalDecisionMs","activeLoans","overdueCount",
        "bookLimitPerUser"
    ]:
        if k in s: print(f"{k:28}: {s[k]}")

    print("\n--- INTERPRETATION ---")
    print("Latency: compare average/P95 across repeated runs.")
    print("Throughput: increase workers gradually and record where latency/failures rise.")
    print("Reliability: target zero unexpected failures for normal requests.")
    print("Two-book rule: bookLimitPerUser must be 2; third active/pending request must be rejected.")
    print("Workflow: a user request must stay PENDING until a librarian approves it.")
    print("Failure modes: use /api/failure-sim?librarianId=<librarian-email>.")

if __name__=="__main__":
    main()
