package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class TCPServer {
    public static final int PORT=9000;
    private final LibraryEngine engine;
    private final ServerMetrics metrics;
    private final ExecutorService pool = new ThreadPoolExecutor(
        10, 50, 60L, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public TCPServer(LibraryEngine e){engine=e;metrics=e.metrics();}
    public void start() throws Exception{
        Runtime.getRuntime().addShutdownHook(new Thread(() -> metrics.printReport("SHUTDOWN")));
        try(ServerSocket server=new ServerSocket(PORT)){
            System.out.println("TCP Server listening on "+PORT);
            while(true){Socket s=server.accept();pool.submit(new ClientHandler(s,engine,metrics));}
        }
    }

    static class ClientHandler implements Runnable{
        private final Socket socket; private final LibraryEngine engine; private final ServerMetrics metrics;
        ClientHandler(Socket s,LibraryEngine e,ServerMetrics m){socket=s;engine=e;metrics=m;}
        public void run(){
            long start=metrics.beginRequest(); boolean failed=false;
            try(socket;
                BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                BufferedWriter out=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8))){
                String line=in.readLine(); String response=handle(line);
                failed=response.contains("\"success\":false");
                out.write(response);out.newLine();out.flush();
            }catch(Exception e){e.printStackTrace();failed=true;try{socket.close();}catch(Exception ignored){}}
            finally{metrics.endRequest(start,failed);}
        }

        private String handle(String line)throws Exception{
            if(line==null)return "{\"success\":false,\"message\":\"Empty request\"}";
            if("PING".equals(line.trim())) return "PONG";
            String[]p=line.split("\\|",-1);String cmd=p[0];
            switch(cmd){
                case"LOGIN":{
                    var u=engine.login(dec(p,1),dec(p,2),dec(p,3),dec(p,4));Map<String,Object>r=new LinkedHashMap<>();
                    r.put("success",u!=null);r.put("message",u!=null?"Login successful!":"Invalid credentials.");
                    if(u!=null){
                        var session = engine.sessionManager().createSession(u.getEmail(), u.getEmail(), u.getRole());
                        Map<String,Object>x=new LinkedHashMap<>();
                        x.put("userId",u.getEmail());x.put("name",u.getFullName());x.put("role",u.getRole());x.put("department",u.getDepartment());
                        x.put("token", session.getToken());
                        r.put("user",x);
                        r.put("token", session.getToken());
                    }
                    return Json.object(r);
                }
                case"SUGGEST_BOOKS":return Json.object(engine.autocompleteSuggestions(dec(p,1),parseInt(dec(p,2,"8"),8)));
                case"BOOKS_DETAILED":return Json.object(engine.searchBooksDetailed(dec(p,1),dec(p,2)));
                case"URGENT_OVERDUE":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("urgent",engine.urgentOverdue(parseInt(dec(p,1,"10"),10)));return Json.object(r);}
                case"HEALTH":{
                    Map<String,Object>r=new LinkedHashMap<>();
                    r.put("success",true);
                    r.put("tcpStatus","HEALTHY");
                    r.put("dbActiveConnections",database.DBConnection.getActiveConnections());
                    r.put("dbIdleConnections",database.DBConnection.getIdleConnections());
                    r.put("dbCapacity",database.DBConnection.getPoolCapacity());
                    r.put("activeSessions",engine.sessionManager().getActiveSessionCount());
                    return Json.object(r);
                }
                case"BOOKS":{
                    var books=engine.searchBooks(dec(p,1),dec(p,2));List<Map<String,Object>>a=new ArrayList<>();
                    for(var b:books){Map<String,Object>x=new LinkedHashMap<>();x.put("id",b.getId());x.put("book_id",b.getBookId());x.put("title",b.getTitle());x.put("author",b.getAuthor());x.put("isbn",b.getIsbn());x.put("category",b.getCategory());x.put("department",b.getDepartment());x.put("rack_number",b.getRackNumber());x.put("total_copies",b.getTotalQuantity());x.put("available_copies",b.getAvailableQuantity());a.add(x);}
                    Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("books",a);return Json.object(r);
                }
                case"LOANS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("loans",engine.loans(dec(p,1)));return Json.object(r);}
                case"STATS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("stats",engine.stats());return Json.object(r);}
                case"FAILURE_SIM":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("simulation",engine.failureSimulation(dec(p,1)));return Json.object(r);}
                case"WAITLIST":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("waitlists",engine.waitlists(dec(p,1)));return Json.object(r);}
                case"DIRECT_ISSUE":return Json.object(engine.directIssue(dec(p,1),dec(p,2)));
                case"ISSUE_REQUEST":return Json.object(engine.requestIssue(dec(p,1),dec(p,2)));
                case"ISSUE_REQUESTS":{
                    List<String> ids=new ArrayList<>();
                    for(String id:dec(p,2).split(",")) if(!id.isBlank()) ids.add(id.trim());
                    return Json.object(engine.requestIssues(dec(p,1),ids));
                }
                case"MY_REQUESTS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("requests",engine.myRequests(dec(p,1)));return Json.object(r);}
                case"PENDING_REQUESTS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("requests",engine.pendingRequests(dec(p,1)));return Json.object(r);}
                case"USERS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("users",engine.usersDirectory(dec(p,1)));return Json.object(r);}
                case"APPROVE_REQUEST":return Json.object(engine.approveIssue(parseInt(dec(p,1),0),dec(p,2)));
                case"REJECT_REQUEST":return Json.object(engine.rejectIssue(parseInt(dec(p,1),0),dec(p,2),dec(p,3)));
                case"RETURN":return Json.object(engine.returnBook(parseInt(dec(p,1),0),dec(p,2)));
                case"APPROVE_RETURN":return Json.object(engine.approveReturn(parseInt(dec(p,1),0),dec(p,2)));
                case"REJECT_RETURN":return Json.object(engine.rejectReturn(parseInt(dec(p,1),0),dec(p,2),dec(p,3)));
                case"PENDING_RETURNS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("returns",engine.pendingReturns(dec(p,1)));return Json.object(r);}
                case"JOIN_WAITLIST":return Json.object(engine.joinWaitlist(dec(p,1),dec(p,2)));
                case"LEAVE_WAITLIST":return Json.object(engine.leaveWaitlist(parseInt(dec(p,1),0),dec(p,2)));
                case"MY_WAITLISTS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("waitlists",engine.myWaitlists(dec(p,1)));return Json.object(r);}
                case"NOTIFICATIONS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("notifications",engine.getNotifications(dec(p,1),dec(p,2,"user")));return Json.object(r);}
                case"UNREAD_NOTIFICATIONS_COUNT":return Json.object(engine.countUnreadNotifications(dec(p,1),dec(p,2,"user")));
                case"MARK_NOTIFICATION_READ":return Json.object(engine.markNotificationRead(parseInt(dec(p,1),0),dec(p,2)));
                case"MARK_ALL_NOTIFICATIONS_READ":return Json.object(engine.markAllNotificationsRead(dec(p,1),dec(p,2,"user")));
                case"MY_FINES":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("fines",engine.myFines(dec(p,1)));return Json.object(r);}
                case"ALL_FINES":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("fines",engine.allFines(dec(p,1)));return Json.object(r);}
                case"SETTLE_FINE":return Json.object(engine.settleFine(parseInt(dec(p,1),0),parseDouble(dec(p,2),0.0),dec(p,3),dec(p,4),dec(p,5)));
                case"ADD_BOOK":return Json.object(engine.addBook(dec(p,1),dec(p,2),dec(p,3),dec(p,4),dec(p,5),dec(p,6),dec(p,7),parseInt(dec(p,8,"1"),1)));
                case"UPDATE_BOOK":return Json.object(engine.updateBook(dec(p,1),dec(p,2),dec(p,3),dec(p,4),dec(p,5),dec(p,6),dec(p,7),dec(p,8),parseInt(dec(p,9,"1"),1)));
                case"DELETE_BOOK":return Json.object(engine.deleteBook(dec(p,1),dec(p,2),dec(p,3)));
                case"TIME_TRAVEL":return Json.object(engine.automationScheduler().timeTravel(parseInt(dec(p,1),0)));
                case"RESET_TIME_TRAVEL":return Json.object(engine.automationScheduler().resetTimeTravel());
                case"GET_EVENTS":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("events",engine.automationScheduler().getEvents(parseLong(dec(p,1,"0"),0L)));return Json.object(r);}
                case"KPI_HISTORY":{Map<String,Object>r=new LinkedHashMap<>();r.put("success",true);r.put("history",engine.automationScheduler().getKpiHistory());return Json.object(r);}
                case"RUN_BENCHMARK":return Json.object(engine.runBenchmarks());
                case"DSA_STATE":return Json.object(engine.getDsaState());
                case"GENERATE_RECEIPT":return Json.object(engine.generateReceipt(parseInt(dec(p,1),0)));
                default:return "{\"success\":false,\"message\":\"Unknown command\"}";
            }
        }
        private String dec(String[]p,int i){return i<p.length?new String(Base64.getDecoder().decode(p[i]),StandardCharsets.UTF_8):"";}
        private String dec(String[]p,int i,String defVal){String val=dec(p,i);return val.isBlank()?defVal:val;}
        private static int parseInt(String s,int def){try{return Integer.parseInt(s.trim());}catch(Exception e){return def;}}
        private static long parseLong(String s,long def){try{return Long.parseLong(s.trim());}catch(Exception e){return def;}}
        private static double parseDouble(String s,double def){try{return Double.parseDouble(s.trim());}catch(Exception e){return def;}}
    }
    public static void main(String[]a)throws Exception{LibraryEngine e=new LibraryEngine();e.initialize();new TCPServer(e).start();}
}
