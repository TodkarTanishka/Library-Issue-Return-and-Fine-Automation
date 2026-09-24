package model;

public class OverdueRecord {
    private int loanId;
    private String studentUsername;
    private String bookId;
    private String dueDate;
    private double fine;

    public OverdueRecord(int loanId, String studentUsername, String bookId, String dueDate, double fine) {
        this.loanId = loanId;
        this.studentUsername = studentUsername;
        this.bookId = bookId;
        this.dueDate = dueDate;
        this.fine = fine;
    }

    public int getLoanId() { return loanId; }
    public String getStudentUsername() { return studentUsername; }
    public String getBookId() { return bookId; }
    public String getDueDate() { return dueDate; }
    public double getFine() { return fine; }
    public void setFine(double fine) { this.fine = fine; }
}
