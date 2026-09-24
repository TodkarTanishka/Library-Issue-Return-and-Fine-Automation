package kpi;
import model.*; import java.util.*;
public class KPIManager {
    private final List<Issue> issues=new ArrayList<>(); private final List<Book> books=new ArrayList<>(); private final List<User> users=new ArrayList<>();
    public void setData(List<Issue> i,List<Book> b,List<User> u){issues.clear();books.clear();users.clear();issues.addAll(i);books.addAll(b);users.addAll(u);}
    public int getTotalBooksIssued(){return issues.size();}
    public int getActiveIssues(){int n=0;for(Issue i:issues)if(i.getStatus().equalsIgnoreCase("Issued")||i.getStatus().equalsIgnoreCase("Overdue"))n++;return n;}
    public int getOverdueBooks(){int n=0;for(Issue i:issues)if(i.getStatus().equalsIgnoreCase("Overdue"))n++;return n;}
    public int getZeroCopyBooks(){int n=0;for(Book b:books)if(b.getAvailableQuantity()==0)n++;return n;}
    public int getBookIssueCount(String id){int n=0;for(Issue i:issues)if(i.getBookId().equalsIgnoreCase(id))n++;return n;}
    public int getUserActiveIssues(String email){int n=0;for(Issue i:issues)if(i.getStudentUsername().equalsIgnoreCase(email)&&(i.getStatus().equalsIgnoreCase("Issued")||i.getStatus().equalsIgnoreCase("Overdue")))n++;return n;}
    public int getTotalMembers(){return users.size();}
}
