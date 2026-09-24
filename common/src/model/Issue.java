package model;
public class Issue {
    private int id; private String studentUsername; private String bookId; private String issueDate; private String dueDate; private String status;
    public Issue(int id,String studentUsername,String bookId,String issueDate,String dueDate,String status){this.id=id;this.studentUsername=studentUsername;this.bookId=bookId;this.issueDate=issueDate;this.dueDate=dueDate;this.status=status;}
    public int getId(){return id;} public String getStudentUsername(){return studentUsername;} public String getBookId(){return bookId;} public String getIssueDate(){return issueDate;} public String getDueDate(){return dueDate;} public String getStatus(){return status;} public void setStatus(String s){status=s;}
}
