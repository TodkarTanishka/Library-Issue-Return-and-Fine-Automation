package server;

import java.util.*;

public class FailureModeSimulator {
    private final LibraryEngine engine;
    public FailureModeSimulator(LibraryEngine engine){this.engine=engine;}

    public Map<String,Object> run(String librarianId) throws Exception {
        List<Map<String,Object>> results=new ArrayList<>();
        results.add(test("INVALID_USER","User sends request with unknown account",
            engine.requestIssue("failure-test-user@invalid.local","MMLIB101")));
        results.add(test("INVALID_BOOK","User requests a book that does not exist",
            engine.requestIssue("failure-test-user@invalid.local","INVALID_BOOK")));
        results.add(test("UNAUTHORIZED_APPROVAL","Non-librarian tries to approve a request",
            engine.approveIssue(999999,librarianId==null?"not-a-librarian":librarianId)));
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("simulation","Failure-mode validation");
        out.put("results",results);
        return out;
    }

    private Map<String,Object> test(String mode,String description,Map<String,Object> response){
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("mode",mode);r.put("description",description);
        r.put("handled",response!=null&&Boolean.FALSE.equals(response.get("success")));
        r.put("message",response==null?"No response":response.get("message"));
        return r;
    }
}
