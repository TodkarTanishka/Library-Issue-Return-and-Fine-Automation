-- Migration V3: Additive Indexing and Audit Logging Table
USE `mmcoe_library`;

CREATE TABLE IF NOT EXISTS audit_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  actor_id VARCHAR(150) NOT NULL,
  actor_role VARCHAR(30) NOT NULL DEFAULT 'user',
  action_type VARCHAR(50) NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id VARCHAR(100) NULL,
  details TEXT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_actor (actor_id),
  INDEX idx_audit_action (action_type),
  INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Performance Indexes (Additive, Ignored if already existing)
SET @dbname = DATABASE();
SET @tablename = 'issued_books';
SET @indexname = 'idx_issued_status_due';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = @dbname
      AND table_name = @tablename
      AND index_name = @indexname
  ) > 0,
  'SELECT 1',
  'CREATE INDEX idx_issued_status_due ON issued_books(status, due_date)'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @tablename = 'issued_books';
SET @indexname = 'idx_issued_user_status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = @dbname
      AND table_name = @tablename
      AND index_name = @indexname
  ) > 0,
  'SELECT 1',
  'CREATE INDEX idx_issued_user_status ON issued_books(student_username, status)'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @tablename = 'fines';
SET @indexname = 'idx_fines_status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = @dbname
      AND table_name = @tablename
      AND index_name = @indexname
  ) > 0,
  'SELECT 1',
  'CREATE INDEX idx_fines_status ON fines(status)'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @tablename = 'waitlist';
SET @indexname = 'idx_waitlist_book_status_joined';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = @dbname
      AND table_name = @tablename
      AND index_name = @indexname
  ) > 0,
  'SELECT 1',
  'CREATE INDEX idx_waitlist_book_status_joined ON waitlist(book_id, status, joined_at)'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
