package database;

import model.IssueRequest;
import java.sql.*;
import java.util.*;

public class IssueRequestRepository {
    public List<IssueRequest> findAll() throws SQLException {
        List<IssueRequest> out=new ArrayList<>();
        String q="SELECT id,user_email,book_id,requested_at,status,decided_at,librarian_id,decision_reason FROM issue_requests ORDER BY requested_at,id";
        try(Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(q); ResultSet rs=p.executeQuery()){
            while(rs.next()) out.add(read(rs));
        }
        return out;
    }

    public List<IssueRequest> findPending() throws SQLException {
        List<IssueRequest> out=new ArrayList<>();
        String q="SELECT id,user_email,book_id,requested_at,status,decided_at,librarian_id,decision_reason FROM issue_requests WHERE status='PENDING' ORDER BY requested_at,id";
        try(Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(q); ResultSet rs=p.executeQuery()){
            while(rs.next()) out.add(read(rs));
        }
        return out;
    }

    public IssueRequest find(int id) throws SQLException {
        String q="SELECT id,user_email,book_id,requested_at,status,decided_at,librarian_id,decision_reason FROM issue_requests WHERE id=?";
        try(Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(q)){
            p.setInt(1,id);
            try(ResultSet rs=p.executeQuery()){ if(rs.next()) return read(rs); }
        }
        return null;
    }

    public boolean hasPending(String email,String bookId) throws SQLException {
        String q="SELECT COUNT(*) FROM issue_requests WHERE user_email=? AND book_id=? AND status='PENDING'";
        try(Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(q)){
            p.setString(1,email); p.setString(2,bookId);
            try(ResultSet rs=p.executeQuery()){rs.next(); return rs.getInt(1)>0;}
        }
    }

    public int countActiveOrPending(String email) throws SQLException {
        String q="SELECT COUNT(*) FROM issue_requests WHERE user_email=? AND status='PENDING'";
        try(Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(q)){
            p.setString(1,email);
            try(ResultSet rs=p.executeQuery()){rs.next(); return rs.getInt(1);}
        }
    }

    public int create(String email,String bookId) throws SQLException {
        String q="INSERT INTO issue_requests(user_email,book_id,status) VALUES(?,?,'PENDING')";
        try(Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(q,Statement.RETURN_GENERATED_KEYS)){
            p.setString(1,email); p.setString(2,bookId); p.executeUpdate();
            try(ResultSet rs=p.getGeneratedKeys()){if(rs.next()) return rs.getInt(1);}
        }
        return -1;
    }

    public List<Integer> createBatch(String email, List<String> bookIds) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String q = "INSERT INTO issue_requests(user_email,book_id,status) VALUES(?,?,'PENDING')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            c.setAutoCommit(false);
            try {
                for (String bookId : bookIds) {
                    p.setString(1, email); p.setString(2, bookId); p.addBatch();
                }
                p.executeBatch();
                try (ResultSet rs = p.getGeneratedKeys()) { while (rs.next()) ids.add(rs.getInt(1)); }
                if (ids.size() != bookIds.size()) throw new SQLException("Unable to create all issue requests.");
                c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
        return ids;
    }

    public boolean decide(int id,String status,String librarianId,String reason) throws SQLException {
        String q="UPDATE issue_requests SET status=?,decided_at=CURRENT_TIMESTAMP,librarian_id=?,decision_reason=? WHERE id=? AND status='PENDING'";
        try(Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(q)){
            p.setString(1,status); p.setString(2,librarianId); p.setString(3,reason); p.setInt(4,id);
            return p.executeUpdate()==1;
        }
    }

    private IssueRequest read(ResultSet rs)throws SQLException{
        Timestamp requested=rs.getTimestamp("requested_at"), decided=rs.getTimestamp("decided_at");
        return new IssueRequest(rs.getInt("id"),rs.getString("user_email"),rs.getString("book_id"),
            requested==null?null:requested.toInstant().toString(),rs.getString("status"),
            decided==null?null:decided.toInstant().toString(),rs.getString("librarian_id"),rs.getString("decision_reason"));
    }
}
