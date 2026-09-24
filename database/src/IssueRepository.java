package database;
import model.Issue; import java.sql.*; import java.util.*;
public class IssueRepository {
    public List<Issue> findAll() throws SQLException{List<Issue> r=new ArrayList<>();String q="SELECT i.id,i.student_username,b.book_id,i.issue_date,i.due_date,i.status FROM issued_books i JOIN books b ON b.id=i.book_id ORDER BY i.id";try(Connection c=DBConnection.getConnection();Statement s=c.createStatement();ResultSet rs=s.executeQuery(q)){while(rs.next())r.add(new Issue(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getDate(4).toString(),rs.getDate(5).toString(),rs.getString(6)));}return r;}
    public List<Issue> findByUser(String email)throws SQLException{List<Issue> all=findAll();all.removeIf(i->!i.getStudentUsername().equalsIgnoreCase(email));return all;}
    public int insert(String email, String bookId, String issueDate, String dueDate) throws SQLException {
        int bookDbId = -1;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id FROM books WHERE book_id = ?")) {
            p.setString(1, bookId);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) bookDbId = rs.getInt(1);
            }
        }
        if (bookDbId <= 0) return -1;

        String q = "INSERT INTO issued_books(student_username, book_id, issue_date, due_date, status) VALUES(?, ?, ?, ?, 'Issued')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, email);
            p.setInt(2, bookDbId);
            p.setDate(3, java.sql.Date.valueOf(issueDate));
            p.setDate(4, java.sql.Date.valueOf(dueDate));
            p.executeUpdate();
            try (ResultSet rs = p.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }
    public void updateStatus(int id,String status)throws SQLException{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE issued_books SET status=? WHERE id=?")){p.setString(1,status);p.setInt(2,id);p.executeUpdate();}}
    public Issue find(int id)throws SQLException{for(Issue i:findAll())if(i.getId()==id)return i;return null;}
    public Issue findById(int id)throws SQLException{return find(id);}
}
