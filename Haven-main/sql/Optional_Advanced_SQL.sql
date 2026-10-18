-- OPTIONAL SQL PRACTICE
-- These objects use the SAME hotel_management database as the main script.
USE hotel_management;

-- A view is a saved SELECT query. Read it like a table.
CREATE OR REPLACE VIEW reservation_details AS
SELECT r.reservation_id, p.full_name AS guest_name, rm.room_number,
       r.check_in_date, r.check_out_date, r.reservation_status
FROM reservation AS r
JOIN guest AS g ON r.guest_id = g.person_id
JOIN person AS p ON g.person_id = p.person_id
JOIN room AS rm ON r.room_id = rm.room_id;

SELECT * FROM reservation_details;

-- An index can help MySQL search reservation rows by room and status.
CREATE INDEX idx_reservation_status
ON reservation (room_id, reservation_status);

EXPLAIN SELECT reservation_id FROM reservation
WHERE room_id = 2 AND reservation_status = 'BOOKED';

-- A trigger automatically inserts an activity row after reservation creation.
DELIMITER //
CREATE TRIGGER after_reservation_insert
AFTER INSERT ON reservation
FOR EACH ROW
BEGIN
    INSERT INTO activity_log (staff_id, action_description)
    VALUES (NEW.staff_id, CONCAT('Created reservation ', NEW.reservation_id));
END//
DELIMITER ;

-- A stored procedure is a named SQL task that can be called later.
DELIMITER //
CREATE PROCEDURE find_room_conflict(
    IN selected_room INT,
    IN wanted_check_in DATE,
    IN wanted_check_out DATE
)
BEGIN
    SELECT reservation_id
    FROM reservation
    WHERE room_id = selected_room
      AND reservation_status IN ('BOOKED', 'CHECKED_IN')
      AND wanted_check_in < check_out_date
      AND wanted_check_out > check_in_date;
END//
DELIMITER ;

-- Example call:
-- CALL find_room_conflict(2, '2026-11-10', '2026-11-13');

-- Transaction example: COMMIT saves a change; ROLLBACK cancels it.
START TRANSACTION;
UPDATE guest SET loyalty_points = loyalty_points + 5 WHERE person_id = 1;
ROLLBACK;

-- Locking example: run inside a transaction, then COMMIT or ROLLBACK.
START TRANSACTION;
SELECT room_id FROM room WHERE room_id = 1 FOR UPDATE;
ROLLBACK;

-- Deadlock classroom demonstration (run statements in two Workbench tabs).
-- Tab A: START TRANSACTION; UPDATE room SET room_status='MAINTENANCE' WHERE room_id=1;
-- Tab B: START TRANSACTION; UPDATE room SET room_status='MAINTENANCE' WHERE room_id=2;
-- Tab A: UPDATE room SET room_status='AVAILABLE' WHERE room_id=2;
-- Tab B: UPDATE room SET room_status='AVAILABLE' WHERE room_id=1;
-- MySQL detects the deadlock and rolls back one transaction.
