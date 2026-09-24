package datastructure.issue;
import model.*; import java.time.LocalDate; import java.util.*;
public class IssueManager {
    private static final int STUDENT_LIMIT=3,FACULTY_LIMIT=3,LOAN_PERIOD_DAYS=14;
    private User[] users=new User[100]; private Book[] books=new Book[100]; private Issue[] issues=new Issue[100]; private int userCount,bookCount,issueCount; private final WaitingListManager waitingListManager=new WaitingListManager();
    public synchronized void addUser(User u){if(userCount==users.length)users=Arrays.copyOf(users,users.length*2);users[userCount++]=u;}
    public synchronized void addBook(Book b){if(bookCount==books.length)books=Arrays.copyOf(books,books.length*2);books[bookCount++]=b;}
    public synchronized void setIssues(List<Issue> list){issues=new Issue[Math.max(100,list.size()+10)];issueCount=0;for(Issue i:list)issues[issueCount++]=i;}
    private User findUser(String email){for(int i=0;i<userCount;i++)if(users[i].getEmail().equalsIgnoreCase(email))return users[i];return null;}
    public synchronized Book findBook(String id){for(int i=0;i<bookCount;i++)if(books[i].getBookId().equalsIgnoreCase(id))return books[i];return null;}
    public synchronized Issue findActiveIssue(String email,String bookId){for(int i=0;i<issueCount;i++){Issue x=issues[i];if(x.getStudentUsername().equalsIgnoreCase(email)&&x.getBookId().equalsIgnoreCase(bookId)&&(x.getStatus().equalsIgnoreCase("Issued")||x.getStatus().equalsIgnoreCase("Overdue")))return x;}return null;}
    public synchronized boolean issueBook(String email,String bookId){User u=findUser(email);Book b=findBook(bookId);if(u==null||b==null)return false;int limit=getBorrowingLimit(u);if(getUserActiveIssueCount(email)>=limit)return false;if(findActiveIssue(email,bookId)!=null)return false;if(b.getAvailableQuantity()<=0){waitingListManager.addToWaitingList(email,bookId);return false;}LocalDate d=LocalDate.now();Issue i=new Issue(issueCount+1,email,bookId,d.toString(),d.plusDays(LOAN_PERIOD_DAYS).toString(),"Issued");if(issueCount==issues.length)issues=Arrays.copyOf(issues,issues.length*2);issues[issueCount++]=i;b.setAvailableQuantity(b.getAvailableQuantity()-1);return true;}
    public synchronized boolean returnBook(String email,String bookId){
        Book b=findBook(bookId); Issue i=findActiveIssue(email,bookId);
        if(b==null||i==null)return false;
        i.setStatus("Returned");
        b.setAvailableQuantity(b.getAvailableQuantity()+1);
        return true;
    }
    private int getBorrowingLimit(User u){return "faculty".equalsIgnoreCase(u.getRole())?FACULTY_LIMIT:STUDENT_LIMIT;}
    public synchronized int getUserActiveIssueCount(String email){int n=0;for(int i=0;i<issueCount;i++)if(issues[i].getStudentUsername().equalsIgnoreCase(email)&&(issues[i].getStatus().equalsIgnoreCase("Issued")||issues[i].getStatus().equalsIgnoreCase("Overdue")))n++;return n;}
    public synchronized void addIssue(Issue i){
        if(i==null)return;
        if(issueCount==issues.length)issues=Arrays.copyOf(issues,issues.length*2);
        issues[issueCount++]=i;
    }
    public synchronized void removeLastIssue(){
        if(issueCount>0){issues[--issueCount]=null;}
    }
    public synchronized List<Issue> getIssues(){return new ArrayList<>(Arrays.asList(Arrays.copyOf(issues,issueCount)));}
    public synchronized WaitingListManager getWaitingListManager(){return waitingListManager;}
}
