package database;
import java.sql.*;

public class DBConnection {
    private static final String URL =
        "jdbc:mysql://localhost:3306/mmcoe_library?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = System.getenv().getOrDefault("MMCOE_DB_PASSWORD", "12345");

    private static final int POOL_SIZE = 10;
    private static final java.util.concurrent.BlockingQueue<Connection> pool = new java.util.concurrent.ArrayBlockingQueue<>(POOL_SIZE);
    private static final java.util.concurrent.atomic.AtomicInteger activeConnections = new java.util.concurrent.atomic.AtomicInteger(0);
    private static boolean isInitialized = false;

    private static synchronized void initPool() {
        if (isInitialized) return;
        try {
            for (int i = 0; i < POOL_SIZE; i++) {
                Connection c = createRawConnection();
                pool.offer(c);
            }
            isInitialized = true;
        } catch (Exception e) {
            System.err.println("Connection pool initialization error: " + e.getMessage());
        }
    }

    private static Connection createRawConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static Connection getConnection() throws SQLException {
        if (!isInitialized) initPool();
        try {
            Connection raw = pool.poll(2, java.util.concurrent.TimeUnit.SECONDS);
            if (raw == null || raw.isClosed()) {
                raw = createRawConnection();
            }
            activeConnections.incrementAndGet();
            final Connection target = raw;
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                DBConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        releaseConnection(target);
                        return null;
                    }
                    return method.invoke(target, args);
                }
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return createRawConnection();
        }
    }

    public static void releaseConnection(Connection c) {
        if (c == null) return;
        activeConnections.decrementAndGet();
        try {
            if (!c.isClosed() && pool.size() < POOL_SIZE) {
                if (!c.getAutoCommit()) c.setAutoCommit(true);
                pool.offer(c);
            } else {
                c.close();
            }
        } catch (Exception ignored) {
            try { c.close(); } catch (Exception ignored2) {}
        }
    }

    public static int getActiveConnections() { return Math.max(0, activeConnections.get()); }
    public static int getIdleConnections() { return pool.size(); }
    public static int getPoolCapacity() { return POOL_SIZE; }

    public static void ensureSchema() throws SQLException {
        try(Connection c=getConnection(); Statement s=c.createStatement()){
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS issue_requests ("+
                "id INT AUTO_INCREMENT PRIMARY KEY,"+
                "user_email VARCHAR(150) NOT NULL,"+
                "book_id VARCHAR(20) NOT NULL,"+
                "requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"+
                "status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',"+
                "decided_at TIMESTAMP NULL,"+
                "librarian_id VARCHAR(150) NULL,"+
                "decision_reason VARCHAR(500) NULL,"+
                "INDEX idx_request_status(status), INDEX idx_request_user(user_email), INDEX idx_request_book(book_id)"+
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS fines ("+
                "id INT AUTO_INCREMENT PRIMARY KEY,"+
                "loan_id INT NOT NULL,"+
                "student_username VARCHAR(150) NOT NULL,"+
                "book_id VARCHAR(20) NOT NULL,"+
                "due_date DATE NOT NULL,"+
                "return_date DATE NULL,"+
                "overdue_days INT NOT NULL DEFAULT 0,"+
                "fine_rate_per_day DECIMAL(10,2) NOT NULL DEFAULT 6.00,"+
                "fine_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,"+
                "paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,"+
                "remaining_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,"+
                "status ENUM('UNPAID','PARTIALLY_PAID','PAID') NOT NULL DEFAULT 'UNPAID',"+
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"+
                "paid_at TIMESTAMP NULL,"+
                "payment_method VARCHAR(50) NULL,"+
                "payment_reference VARCHAR(100) NULL,"+
                "notes VARCHAR(255) NULL,"+
                "INDEX idx_fine_user(student_username), INDEX idx_fine_status(status), INDEX idx_fine_loan(loan_id)"+
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS waitlist ("+
                "id INT AUTO_INCREMENT PRIMARY KEY,"+
                "student_username VARCHAR(150) NOT NULL,"+
                "book_id VARCHAR(20) NOT NULL,"+
                "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"+
                "position INT NOT NULL,"+
                "status ENUM('WAITING','NOTIFIED','FULFILLED','CANCELLED','EXPIRED') NOT NULL DEFAULT 'WAITING',"+
                "notified_at TIMESTAMP NULL,"+
                "fulfilled_at TIMESTAMP NULL,"+
                "expires_at TIMESTAMP NULL,"+
                "INDEX idx_waitlist_book(book_id), INDEX idx_waitlist_user(student_username), INDEX idx_waitlist_status(status)"+
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS notifications ("+
                "id INT AUTO_INCREMENT PRIMARY KEY,"+
                "recipient_email VARCHAR(150) NOT NULL,"+
                "recipient_role VARCHAR(30) NOT NULL DEFAULT 'user',"+
                "type VARCHAR(50) NOT NULL,"+
                "title VARCHAR(255) NOT NULL,"+
                "message TEXT NOT NULL,"+
                "related_book_id VARCHAR(20) NULL,"+
                "related_loan_id INT NULL,"+
                "related_request_id INT NULL,"+
                "related_waitlist_id INT NULL,"+
                "is_read BOOLEAN NOT NULL DEFAULT FALSE,"+
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"+
                "INDEX idx_notif_user(recipient_email), INDEX idx_notif_read(recipient_email, is_read)"+
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            try {
                s.executeUpdate("ALTER TABLE books ADD COLUMN department VARCHAR(100) DEFAULT 'Computer Engineering'");
            } catch (SQLException ignored) {
                // Column already exists
            }

            // Create Triggers
            s.executeUpdate("DROP TRIGGER IF EXISTS trg_after_issue_insert");
            s.executeUpdate(
                "CREATE TRIGGER trg_after_issue_insert "+
                "AFTER INSERT ON issued_books FOR EACH ROW "+
                "BEGIN "+
                "UPDATE books SET available_quantity = GREATEST(0, available_quantity - 1) WHERE id = NEW.book_id; "+
                "END"
            );

            s.executeUpdate("DROP TRIGGER IF EXISTS trg_after_issue_update_return");
            s.executeUpdate(
                "CREATE TRIGGER trg_after_issue_update_return "+
                "AFTER UPDATE ON issued_books FOR EACH ROW "+
                "BEGIN "+
                "IF NEW.status = 'Returned' AND OLD.status != 'Returned' THEN "+
                "UPDATE books SET available_quantity = LEAST(total_quantity, available_quantity + 1) WHERE id = NEW.book_id; "+
                "END IF; "+
                "END"
            );

            // Create Stored Procedure with CURSOR for processing overdue fines
            s.executeUpdate("DROP PROCEDURE IF EXISTS sp_process_overdue_fines");
            s.executeUpdate(
                "CREATE PROCEDURE sp_process_overdue_fines() "+
                "BEGIN "+
                "  DECLARE v_done INT DEFAULT FALSE; "+
                "  DECLARE v_loan_id INT; "+
                "  DECLARE v_student VARCHAR(150); "+
                "  DECLARE v_book_db_id INT; "+
                "  DECLARE v_book_str_id VARCHAR(20); "+
                "  DECLARE v_due_date DATE; "+
                "  DECLARE v_overdue_days INT; "+
                "  DECLARE v_fine_amount DECIMAL(10,2); "+
                "  DECLARE cur_overdue CURSOR FOR "+
                "    SELECT i.id, i.student_username, i.book_id, b.book_id, i.due_date "+
                "    FROM issued_books i JOIN books b ON b.id = i.book_id "+
                "    WHERE i.status = 'Issued' AND i.due_date < CURRENT_DATE(); "+
                "  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE; "+
                "  OPEN cur_overdue; "+
                "  read_loop: LOOP "+
                "    FETCH cur_overdue INTO v_loan_id, v_student, v_book_db_id, v_book_str_id, v_due_date; "+
                "    IF v_done THEN LEAVE read_loop; END IF; "+
                "    UPDATE issued_books SET status = 'Overdue' WHERE id = v_loan_id; "+
                "    SET v_overdue_days = DATEDIFF(CURRENT_DATE(), v_due_date); "+
                "    SET v_fine_amount = v_overdue_days * 6.00; "+
                "    IF EXISTS (SELECT 1 FROM fines WHERE loan_id = v_loan_id) THEN "+
                "      UPDATE fines SET overdue_days = v_overdue_days, fine_amount = v_fine_amount, remaining_amount = GREATEST(0.00, v_fine_amount - paid_amount) WHERE loan_id = v_loan_id; "+
                "    ELSE "+
                "      INSERT INTO fines (loan_id, student_username, book_id, due_date, overdue_days, fine_rate_per_day, fine_amount, paid_amount, remaining_amount, status) "+
                "      VALUES (v_loan_id, v_student, v_book_str_id, v_due_date, v_overdue_days, 6.00, v_fine_amount, 0.00, v_fine_amount, 'UNPAID'); "+
                "    END IF; "+
                "  END LOOP; "+
                "  CLOSE cur_overdue; "+
                "END"
            );

            // Create Stored Procedure with CURSOR for waitlist position recalculation
            s.executeUpdate("DROP PROCEDURE IF EXISTS sp_recalculate_waitlist_positions");
            s.executeUpdate(
                "CREATE PROCEDURE sp_recalculate_waitlist_positions(IN p_book_id VARCHAR(20)) "+
                "BEGIN "+
                "  DECLARE v_done INT DEFAULT FALSE; "+
                "  DECLARE v_waitlist_id INT; "+
                "  DECLARE v_pos INT DEFAULT 1; "+
                "  DECLARE cur_waitlist CURSOR FOR "+
                "    SELECT id FROM waitlist WHERE book_id = p_book_id AND status = 'WAITING' ORDER BY joined_at ASC; "+
                "  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE; "+
                "  OPEN cur_waitlist; "+
                "  w_loop: LOOP "+
                "    FETCH cur_waitlist INTO v_waitlist_id; "+
                "    IF v_done THEN LEAVE w_loop; END IF; "+
                "    UPDATE waitlist SET position = v_pos WHERE id = v_waitlist_id; "+
                "    SET v_pos = v_pos + 1; "+
                "  END LOOP; "+
                "  CLOSE cur_waitlist; "+
                "END"
            );

            // Create Stored Procedure for Add Book
            s.executeUpdate("DROP PROCEDURE IF EXISTS sp_add_book");
            s.executeUpdate(
                "CREATE PROCEDURE sp_add_book( "+
                "  IN p_isbn VARCHAR(20), IN p_title VARCHAR(255), IN p_author VARCHAR(255), "+
                "  IN p_category VARCHAR(100), IN p_department VARCHAR(100), IN p_total INT, "+
                "  OUT p_success BOOLEAN, OUT p_message VARCHAR(255), OUT p_book_id VARCHAR(20) "+
                ") "+
                "BEGIN "+
                "  DECLARE v_exists INT DEFAULT 0; "+
                "  SET p_book_id = CONCAT('MMLIB', FLOOR(100 + (RAND() * 900))); "+
                "  SELECT COUNT(*) INTO v_exists FROM books WHERE isbn = p_isbn AND p_isbn != ''; "+
                "  IF v_exists > 0 THEN "+
                "    SET p_success = FALSE; "+
                "    SET p_message = CONCAT('A book with ISBN ', p_isbn, ' already exists in catalog.'); "+
                "  ELSE "+
                "    INSERT INTO books (book_id, title, author, isbn, category, department, total_quantity, available_quantity) "+
                "    VALUES (p_book_id, p_title, p_author, p_isbn, p_category, p_department, p_total, p_total); "+
                "    SET p_success = TRUE; "+
                "    SET p_message = CONCAT('Book ''', p_title, ''' added successfully with ID: ', p_book_id); "+
                "  END IF; "+
                "END"
            );

            // Create Stored Procedure for Update Book
            s.executeUpdate("DROP PROCEDURE IF EXISTS sp_update_book");
            s.executeUpdate(
                "CREATE PROCEDURE sp_update_book( "+
                "  IN p_book_id VARCHAR(20), IN p_title VARCHAR(255), IN p_author VARCHAR(255), "+
                "  IN p_isbn VARCHAR(20), IN p_category VARCHAR(100), IN p_department VARCHAR(100), "+
                "  IN p_new_total INT, OUT p_success BOOLEAN, OUT p_message VARCHAR(255) "+
                ") "+
                "BEGIN "+
                "  DECLARE v_curr_total INT DEFAULT 0; "+
                "  DECLARE v_curr_avail INT DEFAULT 0; "+
                "  DECLARE v_issued INT DEFAULT 0; "+
                "  DECLARE v_new_avail INT DEFAULT 0; "+
                "  SELECT total_quantity, available_quantity INTO v_curr_total, v_curr_avail FROM books WHERE book_id = p_book_id; "+
                "  IF v_curr_total IS NULL THEN "+
                "    SET p_success = FALSE; SET p_message = 'Book record not found.'; "+
                "  ELSE "+
                "    SET v_issued = v_curr_total - v_curr_avail; "+
                "    IF p_new_total < v_issued THEN "+
                "      SET p_success = FALSE; SET p_message = CONCAT('Cannot set total copies to ', p_new_total, '. There are currently ', v_issued, ' copy(ies) issued on loan.'); "+
                "    ELSE "+
                "      SET v_new_avail = p_new_total - v_issued; "+
                "      UPDATE books SET title = p_title, author = p_author, isbn = p_isbn, category = p_category, department = p_department, total_quantity = p_new_total, available_quantity = v_new_avail WHERE book_id = p_book_id; "+
                "      SET p_success = TRUE; SET p_message = 'Book updated successfully.'; "+
                "    END IF; "+
                "  END IF; "+
                "END"
            );

            // Create Stored Procedure with CURSOR for safe Delete Book
            s.executeUpdate("DROP PROCEDURE IF EXISTS sp_delete_book");
            s.executeUpdate(
                "CREATE PROCEDURE sp_delete_book(IN p_book_id VARCHAR(20), OUT p_success BOOLEAN, OUT p_message VARCHAR(255)) "+
                "BEGIN "+
                "  DECLARE v_done INT DEFAULT FALSE; "+
                "  DECLARE v_db_id INT DEFAULT 0; "+
                "  DECLARE v_active_loans INT DEFAULT 0; "+
                "  DECLARE v_pending_req INT DEFAULT 0; "+
                "  DECLARE cur_check CURSOR FOR "+
                "    SELECT b.id, "+
                "           (SELECT COUNT(*) FROM issued_books i WHERE i.book_id = b.id AND i.status IN ('Issued', 'Overdue')), "+
                "           (SELECT COUNT(*) FROM issue_requests r WHERE r.book_id = b.book_id AND r.status = 'PENDING') "+
                "    FROM books b WHERE b.book_id = p_book_id; "+
                "  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE; "+
                "  SET p_success = FALSE; SET p_message = 'Book record not found.'; "+
                "  OPEN cur_check; "+
                "  FETCH cur_check INTO v_db_id, v_active_loans, v_pending_req; "+
                "  IF NOT v_done THEN "+
                "    IF v_active_loans > 0 THEN "+
                "      SET p_success = FALSE; SET p_message = CONCAT('Cannot delete book: ', v_active_loans, ' active loan(s) currently issued to students.'); "+
                "    ELSEIF v_pending_req > 0 THEN "+
                "      SET p_success = FALSE; SET p_message = CONCAT('Cannot delete book: ', v_pending_req, ' pending issue request(s) exist.'); "+
                "    ELSE "+
                "      DELETE FROM books WHERE id = v_db_id; "+
                "      SET p_success = TRUE; SET p_message = 'Book deleted successfully from inventory.'; "+
                "    END IF; "+
                "  END IF; "+
                "  CLOSE cur_check; "+
                "END"
            );
        }
        System.out.println("MySQL connection successful. All schemas, triggers, and inventory stored procedures (sp_add_book, sp_update_book, sp_delete_book with CURSOR) ready.");
    }
}
