package database;

import model.Notification;
import java.sql.*;
import java.util.*;

public class NotificationRepository {

    public int create(String recipientEmail, String recipientRole, String type, String title, String message,
                      String relatedBookId, Integer relatedLoanId, Integer relatedRequestId, Integer relatedWaitlistId) throws SQLException {
        String sql = "INSERT INTO notifications (recipient_email, recipient_role, type, title, message, related_book_id, related_loan_id, related_request_id, related_waitlist_id, is_read) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, recipientEmail);
            p.setString(2, recipientRole == null || recipientRole.isBlank() ? "user" : recipientRole);
            p.setString(3, type);
            p.setString(4, title);
            p.setString(5, message);
            if (relatedBookId != null && !relatedBookId.isBlank()) p.setString(6, relatedBookId); else p.setNull(6, Types.VARCHAR);
            if (relatedLoanId != null && relatedLoanId > 0) p.setInt(7, relatedLoanId); else p.setNull(7, Types.INTEGER);
            if (relatedRequestId != null && relatedRequestId > 0) p.setInt(8, relatedRequestId); else p.setNull(8, Types.INTEGER);
            if (relatedWaitlistId != null && relatedWaitlistId > 0) p.setInt(9, relatedWaitlistId); else p.setNull(9, Types.INTEGER);
            
            p.executeUpdate();
            try (ResultSet rs = p.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public List<Notification> findByUser(String email, String role) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT id, recipient_email, recipient_role, type, title, message, related_book_id, related_loan_id, related_request_id, related_waitlist_id, is_read, created_at " +
                     "FROM notifications WHERE recipient_email = ? OR recipient_role = 'ALL' OR recipient_role = ? ORDER BY created_at DESC LIMIT 50";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email == null ? "" : email);
            p.setString(2, role == null ? "user" : role);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(read(rs));
                }
            }
        }
        return list;
    }

    public int countUnread(String email, String role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE (recipient_email = ? OR recipient_role = 'ALL' OR recipient_role = ?) AND is_read = FALSE";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email == null ? "" : email);
            p.setString(2, role == null ? "user" : role);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public boolean markAsRead(int notificationId, String email) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND (recipient_email = ? OR recipient_role IN ('ALL', 'librarian', 'admin'))";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, notificationId);
            p.setString(2, email == null ? "" : email);
            return p.executeUpdate() > 0;
        }
    }

    public boolean markAllAsRead(String email, String role) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE recipient_email = ? OR recipient_role = 'ALL' OR recipient_role = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email == null ? "" : email);
            p.setString(2, role == null ? "user" : role);
            return p.executeUpdate() > 0;
        }
    }

    private Notification read(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        int loanId = rs.getInt("related_loan_id");
        int reqId = rs.getInt("related_request_id");
        int waitId = rs.getInt("related_waitlist_id");

        return new Notification(
            rs.getInt("id"),
            rs.getString("recipient_email"),
            rs.getString("recipient_role"),
            rs.getString("type"),
            rs.getString("title"),
            rs.getString("message"),
            rs.getString("related_book_id"),
            rs.wasNull() ? null : loanId,
            rs.wasNull() ? null : reqId,
            rs.wasNull() ? null : waitId,
            rs.getBoolean("is_read"),
            ts == null ? null : ts.toInstant().toString()
        );
    }
}
