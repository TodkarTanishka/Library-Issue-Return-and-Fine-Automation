package model;

public class FineRecord {
    private int id;
    private int loanId;
    private String studentUsername;
    private String bookId;
    private String dueDate;
    private String returnDate;
    private int overdueDays;
    private double fineRatePerDay;
    private double fineAmount;
    private double paidAmount;
    private double remainingAmount;
    private String status;
    private String createdAt;
    private String paidAt;
    private String paymentMethod;
    private String paymentReference;
    private String notes;

    public FineRecord(int id, int loanId, String studentUsername, String bookId, String dueDate,
                      String returnDate, int overdueDays, double fineRatePerDay, double fineAmount,
                      double paidAmount, double remainingAmount, String status, String createdAt,
                      String paidAt, String paymentMethod, String paymentReference, String notes) {
        this.id = id;
        this.loanId = loanId;
        this.studentUsername = studentUsername;
        this.bookId = bookId;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.overdueDays = overdueDays;
        this.fineRatePerDay = fineRatePerDay;
        this.fineAmount = fineAmount;
        this.paidAmount = paidAmount;
        this.remainingAmount = remainingAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.notes = notes;
    }

    public int getId() { return id; }
    public int getLoanId() { return loanId; }
    public String getStudentUsername() { return studentUsername; }
    public String getBookId() { return bookId; }
    public String getDueDate() { return dueDate; }
    public String getReturnDate() { return returnDate; }
    public int getOverdueDays() { return overdueDays; }
    public double getFineRatePerDay() { return fineRatePerDay; }
    public double getFineAmount() { return fineAmount; }
    public double getAmount() { return fineAmount; }
    public double getFine() { return fineAmount; }
    public double getPaidAmount() { return paidAmount; }
    public double getRemainingAmount() { return remainingAmount; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getPaidAt() { return paidAt; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentReference() { return paymentReference; }
    public String getNotes() { return notes; }
}
