package model;

public class Notification {
    private int id;
    private String recipientEmail;
    private String recipientRole;
    private String type;
    private String title;
    private String message;
    private String relatedBookId;
    private Integer relatedLoanId;
    private Integer relatedRequestId;
    private Integer relatedWaitlistId;
    private boolean isRead;
    private String createdAt;

    public Notification(int id, String recipientEmail, String recipientRole, String type, String title,
                        String message, String relatedBookId, Integer relatedLoanId, Integer relatedRequestId,
                        Integer relatedWaitlistId, boolean isRead, String createdAt) {
        this.id = id;
        this.recipientEmail = recipientEmail;
        this.recipientRole = recipientRole;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedBookId = relatedBookId;
        this.relatedLoanId = relatedLoanId;
        this.relatedRequestId = relatedRequestId;
        this.relatedWaitlistId = relatedWaitlistId;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientRole() { return recipientRole; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getRelatedBookId() { return relatedBookId; }
    public Integer getRelatedLoanId() { return relatedLoanId; }
    public Integer getRelatedRequestId() { return relatedRequestId; }
    public Integer getRelatedWaitlistId() { return relatedWaitlistId; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
}
