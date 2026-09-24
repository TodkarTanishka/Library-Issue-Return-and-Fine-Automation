package database;

import model.WaitlistEntry;
import java.sql.*;
import java.util.*;

public class WaitlistRepository {

    public List<WaitlistEntry> findWaitingByBook(String bookId) throws SQLException {
        List<WaitlistEntry> list = new ArrayList<>();
        String sql = "SELECT id, student_username, book_id, joined_at, position, status, notified_at, fulfilled_at, expires_at " +
                     "FROM waitlist WHERE book_id=? AND status IN ('WAITING', 'NOTIFIED') ORDER BY position ASC, joined_at ASC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, bookId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) list.add(read(rs));
            }
        }
        return list;
    }

    public List<WaitlistEntry> findByUser(String email) throws SQLException {
        List<WaitlistEntry> list = new ArrayList<>();
        String sql = "SELECT id, student_username, book_id, joined_at, position, status, notified_at, fulfilled_at, expires_at " +
                     "FROM waitlist WHERE student_username=? ORDER BY joined_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) list.add(read(rs));
            }
        }
        return list;
    }

    public List<WaitlistEntry> findAll() throws SQLException {
        List<WaitlistEntry> list = new ArrayList<>();
        String sql = "SELECT id, student_username, book_id, joined_at, position, status, notified_at, fulfilled_at, expires_at " +
                     "FROM waitlist ORDER BY joined_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) list.add(read(rs));
        }
        return list;
    }

    public boolean hasActiveWaitlist(String email, String bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM waitlist WHERE student_username=? AND book_id=? AND status IN ('WAITING', 'NOTIFIED')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            p.setString(2, bookId);
            try (ResultSet rs = p.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public int add(String email, String bookId) throws SQLException {
        if (hasActiveWaitlist(email, bookId)) return -1;
        String countSql = "SELECT COUNT(*) FROM waitlist WHERE book_id=? AND status='WAITING'";
        int nextPos = 1;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(countSql)) {
            p.setString(1, bookId);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) nextPos = rs.getInt(1) + 1;
            }
        }

        String sql = "INSERT INTO waitlist(student_username, book_id, position, status) VALUES(?, ?, ?, 'WAITING')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, email);
            p.setString(2, bookId);
            p.setInt(3, nextPos);
            p.executeUpdate();
            int newId = -1;
            try (ResultSet rs = p.getGeneratedKeys()) {
                if (rs.next()) newId = rs.getInt(1);
            }
            recalculatePositionsProcedure(bookId);
            return newId;
        }
    }

    public boolean notifyNextWaitingUser(int id) throws SQLException {
        String sql = "UPDATE waitlist SET status='NOTIFIED', notified_at=CURRENT_TIMESTAMP, expires_at=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 24 HOUR) WHERE id=? AND status='WAITING'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            return p.executeUpdate() == 1;
        }
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE waitlist SET status=?, " +
                     "notified_at = CASE WHEN ? = 'NOTIFIED' THEN CURRENT_TIMESTAMP ELSE notified_at END, " +
                     "expires_at = CASE WHEN ? = 'NOTIFIED' THEN DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 24 HOUR) ELSE expires_at END, " +
                     "fulfilled_at = CASE WHEN ? = 'FULFILLED' THEN CURRENT_TIMESTAMP ELSE fulfilled_at END " +
                     "WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, status);
            p.setString(2, status);
            p.setString(3, status);
            p.setString(4, status);
            p.setInt(5, id);
            return p.executeUpdate() == 1;
        }
    }

    public boolean cancelWaitlist(int id, String email) throws SQLException {
        String bookId = null;
        String findSql = "SELECT book_id FROM waitlist WHERE id=? AND student_username=? AND status IN ('WAITING', 'NOTIFIED')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(findSql)) {
            p.setInt(1, id);
            p.setString(2, email);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) bookId = rs.getString(1);
            }
        }
        if (bookId == null) return false;

        String updateSql = "UPDATE waitlist SET status='CANCELLED' WHERE id=? AND student_username=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(updateSql)) {
            p.setInt(1, id);
            p.setString(2, email);
            boolean ok = p.executeUpdate() == 1;
            if (ok) {
                recalculatePositionsProcedure(bookId);
            }
            return ok;
        }
    }

    public List<WaitlistEntry> expirePastReservations() throws SQLException {
        List<WaitlistEntry> expiredList = new ArrayList<>();
        String findSql = "SELECT id, student_username, book_id, joined_at, position, status, notified_at, fulfilled_at, expires_at " +
                         "FROM waitlist WHERE status='NOTIFIED' AND expires_at < CURRENT_TIMESTAMP()";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(findSql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) expiredList.add(read(rs));
        }

        if (!expiredList.isEmpty()) {
            String updateSql = "UPDATE waitlist SET status='EXPIRED' WHERE status='NOTIFIED' AND expires_at < CURRENT_TIMESTAMP()";
            try (Connection c = DBConnection.getConnection();
                 Statement s = c.createStatement()) {
                s.executeUpdate(updateSql);
            }
            for (WaitlistEntry w : expiredList) {
                recalculatePositionsProcedure(w.getBookId());
            }
        }
        return expiredList;
    }

    public void recalculatePositionsProcedure(String bookId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_recalculate_waitlist_positions(?)}")) {
            cs.setString(1, bookId);
            cs.execute();
        } catch (SQLException e) {
            // Manual fallback if procedure fails
            String selectSql = "SELECT id FROM waitlist WHERE book_id=? AND status='WAITING' ORDER BY joined_at ASC, id ASC";
            String updateSql = "UPDATE waitlist SET position=? WHERE id=?";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement pSel = c.prepareStatement(selectSql);
                 PreparedStatement pUpd = c.prepareStatement(updateSql)) {
                pSel.setString(1, bookId);
                try (ResultSet rs = pSel.executeQuery()) {
                    int pos = 1;
                    while (rs.next()) {
                        pUpd.setInt(1, pos++);
                        pUpd.setInt(2, rs.getInt("id"));
                        pUpd.executeUpdate();
                    }
                }
            }
        }
    }

    private WaitlistEntry read(ResultSet rs) throws SQLException {
        Timestamp joined = rs.getTimestamp("joined_at");
        Timestamp notified = rs.getTimestamp("notified_at");
        Timestamp fulfilled = rs.getTimestamp("fulfilled_at");
        Timestamp expires = rs.getTimestamp("expires_at");
        return new WaitlistEntry(
            rs.getInt("id"),
            rs.getString("student_username"),
            rs.getString("book_id"),
            joined == null ? null : joined.toInstant().toString(),
            rs.getInt("position"),
            rs.getString("status"),
            notified == null ? null : notified.toInstant().toString(),
            fulfilled == null ? null : fulfilled.toInstant().toString(),
            expires == null ? null : expires.toInstant().toString()
        );
    }
}
