package gateway;

import com.sun.net.httpserver.*;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;

public class HTTPGateway {
    private static final int PORT=8080, TCP_PORT=9000; private static final String TCP_HOST="localhost";
    private final Path frontendRoot;
    public HTTPGateway(Path root){frontendRoot=root.toAbsolutePath().normalize();}
    public void start() throws Exception{
        HttpServer s=HttpServer.create(new InetSocketAddress("0.0.0.0",PORT),0);s.createContext("/api",this::api);s.createContext("/",this::staticFile);s.setExecutor(Executors.newCachedThreadPool());System.out.println("HTTP Gateway: http://0.0.0.0:"+PORT);s.start();
        Thread.currentThread().join();
    }
    private void api(HttpExchange ex)throws IOException{addCors(ex);if("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())){ex.sendResponseHeaders(204,-1);return;}try{String path=ex.getRequestURI().getPath();String cmd;String body="";Map<String,String> q=query(ex.getRequestURI().getRawQuery());
            if(path.equals("/api/auth/login")){body="LOGIN|"+enc(q.getOrDefault("userId",""))+"|"+enc(q.getOrDefault("password",""))+"|"+enc(q.getOrDefault("role",""))+"|"+enc(q.getOrDefault("loginType","user"));}
            else if(path.equals("/api/books")){body="BOOKS|"+enc(q.getOrDefault("search",""))+"|"+enc(q.getOrDefault("department","all"));}
            else if(path.equals("/api/loans")){body="LOANS|"+enc(q.getOrDefault("userId",""));}
            else if(path.equals("/api/stats")){body="STATS";}
            else if(path.equals("/api/failure-sim")){body="FAILURE_SIM|"+enc(q.getOrDefault("librarianId",""));}
            else if(path.equals("/api/waitlists")){body="WAITLIST|"+enc(q.getOrDefault("bookId",""));}
            else if(path.equals("/api/issue")){body="DIRECT_ISSUE|"+enc(q.getOrDefault("userId",""))+"|"+enc(q.getOrDefault("bookId",""));}
            else if(path.equals("/api/issue-request")){body="ISSUE_REQUEST|"+enc(q.getOrDefault("userId",""))+"|"+enc(q.getOrDefault("bookId",""));}
            else if(path.equals("/api/issue-requests")){body="ISSUE_REQUESTS|"+enc(q.getOrDefault("userId",""))+"|"+enc(q.getOrDefault("bookIds",""));}
            else if(path.equals("/api/my-requests")){body="MY_REQUESTS|"+enc(q.getOrDefault("userId",""));}
            else if(path.equals("/api/pending-requests")){body="PENDING_REQUESTS|"+enc(q.getOrDefault("librarianId",""));}
            else if(path.equals("/api/users")){body="USERS|"+enc(q.getOrDefault("adminId",""));}
            else if(path.equals("/api/approve-request")){body="APPROVE_REQUEST|"+enc(q.getOrDefault("requestId",""))+"|"+enc(q.getOrDefault("librarianId",""));}
            else if(path.equals("/api/reject-request")){body="REJECT_REQUEST|"+enc(q.getOrDefault("requestId",""))+"|"+enc(q.getOrDefault("librarianId",""))+"|"+enc(q.getOrDefault("reason","Rejected by librarian."));}
            else if(path.equals("/api/return")){body="RETURN|"+enc(q.getOrDefault("loanId",""))+"|"+enc(q.getOrDefault("userId",""));}
            else if(path.equals("/api/approve-return")){body="APPROVE_RETURN|"+enc(q.getOrDefault("loanId",""))+"|"+enc(q.getOrDefault("librarianId",""));}
            else if(path.equals("/api/reject-return")){body="REJECT_RETURN|"+enc(q.getOrDefault("loanId",""))+"|"+enc(q.getOrDefault("librarianId",""))+"|"+enc(q.getOrDefault("reason","Rejected by librarian."));}
            else if(path.equals("/api/pending-returns")){body="PENDING_RETURNS|"+enc(q.getOrDefault("librarianId",""));}
            else if(path.equals("/api/join-waitlist")){body="JOIN_WAITLIST|"+enc(q.getOrDefault("userId",""))+"|"+enc(q.getOrDefault("bookId",""));}
            else if(path.equals("/api/leave-waitlist")){body="LEAVE_WAITLIST|"+enc(q.getOrDefault("waitlistId",""))+"|"+enc(q.getOrDefault("userId",""));}
            else if(path.equals("/api/my-waitlist")){body="MY_WAITLISTS|"+enc(q.getOrDefault("userId",""));}
            else if(path.equals("/api/notifications")){body="NOTIFICATIONS|"+enc(q.getOrDefault("userId",""))+"|"+enc(q.getOrDefault("role","user"));}
            else if(path.equals("/api/notifications/unread-count")){body="UNREAD_NOTIFICATIONS_COUNT|"+enc(q.getOrDefault("userId",""))+"|"+enc(q.getOrDefault("role","user"));}
            else if(path.equals("/api/notifications/read")){body="MARK_NOTIFICATION_READ|"+enc(q.getOrDefault("id",""))+"|"+enc(q.getOrDefault("userId",""));}
            else if(path.equals("/api/notifications/read-all")){body="MARK_ALL_NOTIFICATIONS_READ|"+enc(q.getOrDefault("userId",""))+"|"+enc(q.getOrDefault("role","user"));}
            else if(path.equals("/api/my-fines")){body="MY_FINES|"+enc(q.getOrDefault("userId",""));}
            else if(path.equals("/api/fines")){body="ALL_FINES|"+enc(q.getOrDefault("librarianId",""));}
            else if(path.equals("/api/settle-fine")){body="SETTLE_FINE|"+enc(q.getOrDefault("fineId",""))+"|"+enc(q.getOrDefault("amountPaid",""))+"|"+enc(q.getOrDefault("paymentMethod","CASH"))+"|"+enc(q.getOrDefault("paymentRef",""))+"|"+enc(q.getOrDefault("librarianId",""));}
            else if(path.equals("/api/add-book")){body="ADD_BOOK|"+enc(q.getOrDefault("role",""))+"|"+enc(q.getOrDefault("librarianId",""))+"|"+enc(q.getOrDefault("isbn",""))+"|"+enc(q.getOrDefault("title",""))+"|"+enc(q.getOrDefault("author",""))+"|"+enc(q.getOrDefault("category","General"))+"|"+enc(q.getOrDefault("department","Computer Engineering"))+"|"+enc(q.getOrDefault("copies","1"));}
            else if(path.equals("/api/update-book")){body="UPDATE_BOOK|"+enc(q.getOrDefault("role",""))+"|"+enc(q.getOrDefault("librarianId",""))+"|"+enc(q.getOrDefault("bookId",""))+"|"+enc(q.getOrDefault("title",""))+"|"+enc(q.getOrDefault("author",""))+"|"+enc(q.getOrDefault("isbn",""))+"|"+enc(q.getOrDefault("category","General"))+"|"+enc(q.getOrDefault("department","Computer Engineering"))+"|"+enc(q.getOrDefault("copies","1"));}
            else if(path.equals("/api/health")){body="HEALTH";}
            else if(path.equals("/api/books/suggest")){body="SUGGEST_BOOKS|"+enc(q.getOrDefault("q",""))+"|"+enc(q.getOrDefault("limit","8"));}
            else if(path.equals("/api/books/detailed")){body="BOOKS_DETAILED|"+enc(q.getOrDefault("search",""))+"|"+enc(q.getOrDefault("department","all"));}
            else if(path.equals("/api/overdue/urgent")){body="URGENT_OVERDUE|"+enc(q.getOrDefault("limit","10"));}
            else if(path.equals("/api/admin/time-travel")){String d=q.getOrDefault("days","0");body="reset".equalsIgnoreCase(d)||"0".equals(d)?"RESET_TIME_TRAVEL":"TIME_TRAVEL|"+enc(d);}
            else if(path.equals("/api/admin/kpi-history")){body="KPI_HISTORY";}
            else if(path.equals("/api/admin/benchmark")){body="RUN_BENCHMARK";}
            else if(path.equals("/api/dsa/state")){body="DSA_STATE";}
            else if(path.equals("/api/receipt")){body="GENERATE_RECEIPT|"+enc(q.getOrDefault("loanId","0"));}
            else if(path.equals("/api/events")){handleSse(ex);return;}
            else {send(ex,404,"{\"success\":false,\"message\":\"API endpoint not found\"}");return;}
            String response=sendTcp(body);
            if(response==null) response="{\"success\":false,\"message\":\"Internal server error.\"}";
            send(ex,200,response);
        }catch(Exception e){send(ex,500,"{\"success\":false,\"message\":\""+esc(e.getMessage())+"\"}");}}
    private void handleSse(HttpExchange ex) throws IOException {
        addCors(ex);
        ex.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.getResponseHeaders().set("Connection", "keep-alive");
        ex.sendResponseHeaders(200, 0);
        long lastEventId = 0;
        try (OutputStream os = ex.getResponseBody();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            while (true) {
                try {
                    String tcpRes = sendTcp("GET_EVENTS|" + enc(String.valueOf(lastEventId)));
                    if (tcpRes != null && tcpRes.contains("\"events\":[")) {
                        writer.write("data: " + tcpRes + "\n\n");
                        writer.flush();
                    }
                    Thread.sleep(2000);
                } catch (InterruptedException e) { break; }
                catch (Exception e) { break; }
            }
        }
    }
    private void staticFile(HttpExchange ex)throws IOException{addCors(ex);String p=ex.getRequestURI().getPath();if(p.equals("/"))p="/landing.html";Path file=frontendRoot.resolve(p.substring(1)).normalize();if(!file.startsWith(frontendRoot)||!Files.exists(file)||Files.isDirectory(file)){send(ex,404,"Not found");return;}String ct=contentType(file);byte[] data=Files.readAllBytes(file);ex.getResponseHeaders().set("Content-Type",ct);ex.sendResponseHeaders(200,data.length);try(OutputStream o=ex.getResponseBody()){o.write(data);}}
    private String sendTcp(String msg)throws Exception{try(Socket s=new Socket(TCP_HOST,TCP_PORT);BufferedWriter out=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8));BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8))){out.write(msg);out.newLine();out.flush();return in.readLine();}}
    private Map<String,String> query(String raw){Map<String,String>m=new HashMap<>();if(raw==null)return m;for(String part:raw.split("&")){int i=part.indexOf('=');String k=i<0?part:part.substring(0,i);String v=i<0?"":part.substring(i+1);m.put(dec(k),dec(v));}return m;}
    private static String enc(String s){return Base64.getEncoder().encodeToString((s==null?"":s).getBytes(StandardCharsets.UTF_8));} private static String dec(String s){try{return URLDecoder.decode(s,StandardCharsets.UTF_8);}catch(Exception e){return s;}}
    private static String esc(String s){return String.valueOf(s).replace("\\","\\\\").replace("\"","\\\"");}
    private static String contentType(Path p){String n=p.getFileName().toString().toLowerCase();if(n.endsWith(".html"))return"text/html; charset=UTF-8";if(n.endsWith(".js"))return"application/javascript; charset=UTF-8";if(n.endsWith(".css"))return"text/css; charset=UTF-8";if(n.endsWith(".jpg")||n.endsWith(".jpeg"))return"image/jpeg";if(n.endsWith(".png"))return"image/png";return"application/octet-stream";}
    private static void addCors(HttpExchange e){e.getResponseHeaders().set("Access-Control-Allow-Origin","*");e.getResponseHeaders().set("Access-Control-Allow-Methods","GET,OPTIONS");e.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");}
    private static void send(HttpExchange e,int code,String body)throws IOException{byte[]d=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");e.sendResponseHeaders(code,d.length);try(OutputStream o=e.getResponseBody()){o.write(d);}}
    public static void main(String[]args)throws Exception{new HTTPGateway(Paths.get(args.length>0?args[0]:"frontend")).start();}
}
