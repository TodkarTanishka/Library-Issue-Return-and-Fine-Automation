package database;

import model.FineRecord;
import model.Issue;
import java.sql.*;
import java.util.*;

public class FineRepository {

    public List<FineRecord> findAll() throws SQLException {
        List<FineRecord> list = new ArrayList<>();
        String sql = "SELECT id, loan_id, student_username, book_id, due_date, return_date, overdue_days, " +
                     "fine_rate_per_day, fine_amount, paid_amount, remaining_amount, status, created_at, " +
                     "paid_at, payment_method, payment_reference, notes FROM fines ORDER BY id DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                list.add(read(rs));
            }
        }
        return list;
    }

    public List<FineRecord> findByUser(String email) throws SQLException {
        List<FineRecord> list = new ArrayList<>();
        String sql = "SELECT id, loan_id, student_username, book_id, due_date, return_date, overdue_days, " +
                     "fine_rate_per_day, fine_amount, paid_amount, remaining_amount, status, created_at, " +
                     "paid_at, payment_method, payment_reference, notes FROM fines WHERE student_username=? ORDER BY id DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(read(rs));
                }
            }
        }
        return list;
    }

    public FineRecord findByLoanId(int loanId) throws SQLException {
        String sql = "SELECT id, loan_id, student_username, book_id, due_date, return_date, overdue_days, " +
                     "fine_rate_per_day, fine_amount, paid_amount, remaining_amount, status, created_at, " +
                     "paid_at, payment_method, payment_reference, notes FROM fines WHERE loan_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, loanId);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return read(rs);
            }
        }
        return null;
    }

    public FineRecord findById(int id) throws SQLException {
        String sql = "SELECT id, loan_id, student_username, book_id, due_date, return_date, overdue_days, " +
                     "fine_rate_per_day, fine_amount, paid_amount, remaining_amount, status, created_at, " +
                     "paid_at, payment_method, payment_reference, notes FROM fines WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return read(rs);
            }
        }
        return null;
    }

    public int createOrUpdateFine(int loanId, String studentUsername, String bookId, String dueDate,
                                  String returnDate, int overdueDays, double fineAmount) throws SQLException {
        FineRecord existing = findByLoanId(loanId);
        if (existing == null) {
            String sql = "INSERT INTO fines(loan_id, student_username, book_id, due_date, return_date, " +
                         "overdue_days, fine_rate_per_day, fine_amount, paid_amount, remaining_amount, status) " +
                         "VALUES(?, ?, ?, ?, ?, ?, 6.00, ?, 0.00, ?, ?)";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, loanId);
                p.setString(2, studentUsername);
                p.setString(3, bookId);
                p.setDate(4, java.sql.Date.valueOf(dueDate));
                p.setDate(5, returnDate != null ? java.sql.Date.valueOf(returnDate) : null);
                p.setInt(6, overdueDays);
                p.setDouble(7, fineAmount);
                p.setDouble(8, fineAmount);
                p.setString(9, fineAmount <= 0 ? "PAID" : "UNPAID");
                p.executeUpdate();
                try (ResultSet rs = p.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } else {
            double newRemaining = fineAmount - existing.getPaidAmount();
            String newStatus = newRemaining <= 0 ? "PAID" : (existing.getPaidAmount() > 0 ? "PARTIALLY_PAID" : "UNPAID");
            String sql = "UPDATE fines SET return_date=?, overdue_days=?, fine_amount=?, remaining_amount=?, status=? WHERE id=?";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement p = c.prepareStatement(sql)) {
                p.setDate(1, returnDate != null ? java.sql.Date.valueOf(returnDate) : null);
                p.setInt(2, overdueDays);
                p.setDouble(3, fineAmount);
                p.setDouble(4, Math.max(0.0, newRemaining));
                p.setString(5, newStatus);
                p.setInt(6, existing.getId());
                p.executeUpdate();
                return existing.getId();
            }
        }
        return -1;
    }

    public int upsertFine(int loanId, String studentUsername, double fineAmount, String status) throws SQLException {
        FineRecord existing = findByLoanId(loanId);
        if (existing == null) {
            Issue issue = new IssueRepository().find(loanId);
            String bookId = issue != null ? issue.getBookId() : "";
            String dueDate = issue != null ? issue.getDueDate() : model.VirtualClock.getToday().toString();
            return createOrUpdateFine(loanId, studentUsername, bookId, dueDate, null, 1, fineAmount);
        } else {
            return createOrUpdateFine(loanId, studentUsername, existing.getBookId(), existing.getDueDate(), existing.getReturnDate(), existing.getOverdueDays(), fineAmount);
        }
    }

    public boolean recordPayment(int fineId, double amountPaid, String paymentMethod, String paymentRef, String librarianId) throws SQLException {
        FineRecord f = findById(fineId);
        if (f == null) return false;
        double newPaid = f.getPaidAmount() + amountPaid;
        double newRemaining = Math.max(0.0, f.getFineAmount() - newPaid);
        String newStatus = newRemaining <= 0 ? "PAID" : "PARTIALLY_PAID";

        String sql = "UPDATE fines SET paid_amount=?, remaining_amount=?, status=?, paid_at=CURRENT_TIMESTAMP, " +
                     "payment_method=?, payment_reference=?, notes=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setDouble(1, newPaid);
            p.setDouble(2, newRemaining);
            p.setString(3, newStatus);
            p.setString(4, paymentMethod);
            p.setString(5, paymentRef);
            p.setString(6, "Settled by " + librarianId);
            p.setInt(7, fineId);
            return p.executeUpdate() == 1;
        }
    }

    public void processOverdueFinesProcedure() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_process_overdue_fines()}")) {
            cs.execute();
        }
    }

    private FineRecord read(ResultSet rs) throws SQLException {
        java.sql.Date due = rs.getDate("due_date");
        java.sql.Date ret = rs.getDate("return_date");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp paidAt = rs.getTimestamp("paid_at");
        return new FineRecord(
            rs.getInt("id"),
            rs.getInt("loan_id"),
            rs.getString("student_username"),
            rs.getString("book_id"),
            due == null ? null : due.toString(),
            ret == null ? null : ret.toString(),
            rs.getInt("overdue_days"),
            rs.getDouble("fine_rate_per_day"),
            rs.getDouble("fine_amount"),
            rs.getDouble("paid_amount"),
            rs.getDouble("remaining_amount"),
            rs.getString("status"),
            created == null ? null : created.toInstant().toString(),
            paidAt == null ? null : paidAt.toInstant().toString(),
            rs.getString("payment_method"),
            rs.getString("payment_reference"),
            rs.getString("notes")
        );
    }
}
