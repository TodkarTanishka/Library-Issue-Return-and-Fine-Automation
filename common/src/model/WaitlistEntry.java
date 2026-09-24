package model;

public class WaitlistEntry {
    private int id;
    private String studentUsername;
    private String bookId;
    private String joinedAt;
    private int position;
    private String status;
    private String notifiedAt;
    private String fulfilledAt;
    private String expiresAt;

    public WaitlistEntry(int id, String studentUsername, String bookId, String joinedAt,
                         int position, String status, String notifiedAt, String fulfilledAt, String expiresAt) {
        this.id = id;
        this.studentUsername = studentUsername;
        this.bookId = bookId;
        this.joinedAt = joinedAt;
        this.position = position;
        this.status = status;
        this.notifiedAt = notifiedAt;
        this.fulfilledAt = fulfilledAt;
        this.expiresAt = expiresAt;
    }

    public int getId() { return id; }
    public String getStudentUsername() { return studentUsername; }
    public String getBookId() { return bookId; }
    public String getJoinedAt() { return joinedAt; }
    public int getPosition() { return position; }
    public String getStatus() { return status; }
    public String getNotifiedAt() { return notifiedAt; }
    public String getFulfilledAt() { return fulfilledAt; }
    public String getExpiresAt() { return expiresAt; }
}
