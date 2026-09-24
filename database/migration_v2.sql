-- Migration V2: Schema updates for Notifications, Waitlist Expiration, and Procedures
USE `mmcoe_library`;

CREATE TABLE IF NOT EXISTS notifications (
  id INT AUTO_INCREMENT PRIMARY KEY,
  recipient_email VARCHAR(150) NOT NULL,
  recipient_role VARCHAR(30) NOT NULL DEFAULT 'user',
  type VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  related_book_id VARCHAR(20) NULL,
  related_loan_id INT NULL,
  related_request_id INT NULL,
  related_waitlist_id INT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notif_user (recipient_email),
  INDEX idx_notif_read (recipient_email, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ensure issued_books status column can store RETURN_REQUESTED
ALTER TABLE issued_books MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'Issued';

-- Stored Procedure for waitlist recalculation
DROP PROCEDURE IF EXISTS sp_recalculate_waitlist_positions;
DELIMITER //
CREATE PROCEDURE sp_recalculate_waitlist_positions(IN p_book_id VARCHAR(20))
BEGIN
  DECLARE v_done INT DEFAULT FALSE;
  DECLARE v_waitlist_id INT;
  DECLARE v_pos INT DEFAULT 1;
  DECLARE cur_waitlist CURSOR FOR
    SELECT id FROM waitlist WHERE book_id = p_book_id AND status = 'WAITING' ORDER BY joined_at ASC, id ASC;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
  OPEN cur_waitlist;
  w_loop: LOOP
    FETCH cur_waitlist INTO v_waitlist_id;
    IF v_done THEN LEAVE w_loop; END IF;
    UPDATE waitlist SET position = v_pos WHERE id = v_waitlist_id;
    SET v_pos = v_pos + 1;
  END LOOP;
  CLOSE cur_waitlist;
END //
DELIMITER ;
