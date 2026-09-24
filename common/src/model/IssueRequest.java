package model;

public class IssueRequest {
    private int id;
    private String userEmail;
    private String bookId;
    private String requestedAt;
    private String status;
    private String decidedAt;
    private String librarianId;
    private String decisionReason;

    public IssueRequest(int id, String userEmail, String bookId, String requestedAt,
                        String status, String decidedAt, String librarianId, String decisionReason) {
        this.id=id; this.userEmail=userEmail; this.bookId=bookId; this.requestedAt=requestedAt;
        this.status=status; this.decidedAt=decidedAt; this.librarianId=librarianId; this.decisionReason=decisionReason;
    }
    public int getId(){return id;}
    public String getUserEmail(){return userEmail;}
    public String getBookId(){return bookId;}
    public String getRequestedAt(){return requestedAt;}
    public String getStatus(){return status;}
    public String getDecidedAt(){return decidedAt;}
    public String getLibrarianId(){return librarianId;}
    public String getDecisionReason(){return decisionReason;}
}
