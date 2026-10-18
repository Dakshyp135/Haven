-- HOTEL MANAGEMENT SYSTEM - MAIN SETUP SCRIPT
-- Run this whole file once in MySQL Workbench.
-- All project tables are created inside the same database: hotel_management.

CREATE DATABASE IF NOT EXISTS hotel_management;
USE hotel_management;

-- 1. PEOPLE: shared information for guests and hotel staff.
CREATE TABLE IF NOT EXISTS person (
    person_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(25),
    email VARCHAR(150)
);

-- 2. GUEST: person_id is both the primary key and a link to person.
CREATE TABLE IF NOT EXISTS guest (
    person_id INT PRIMARY KEY,
    guest_type ENUM('REGULAR', 'LOYALTY') NOT NULL DEFAULT 'REGULAR',
    loyalty_points INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_guest_person FOREIGN KEY (person_id)
        REFERENCES person(person_id),
    CONSTRAINT chk_loyalty_points CHECK (loyalty_points >= 0)
);

-- 3. STAFF: employees who create bookings and record payments.
CREATE TABLE IF NOT EXISTS staff (
    person_id INT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    job_title VARCHAR(60) NOT NULL,
    CONSTRAINT fk_staff_person FOREIGN KEY (person_id)
        REFERENCES person(person_id)
);

-- 4. ROOM TYPE: category, capacity, and nightly rate.
CREATE TABLE IF NOT EXISTS room_type (
    room_type_id INT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    standard_rate DECIMAL(10, 2) NOT NULL,
    CONSTRAINT chk_room_capacity CHECK (capacity > 0),
    CONSTRAINT chk_room_rate CHECK (standard_rate >= 0)
);

-- 5. ROOM: each room belongs to one category.
CREATE TABLE IF NOT EXISTS room (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(15) NOT NULL UNIQUE,
    floor_number INT NOT NULL,
    room_status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE')
        NOT NULL DEFAULT 'AVAILABLE',
    room_type_id INT NOT NULL,
    CONSTRAINT fk_room_type FOREIGN KEY (room_type_id)
        REFERENCES room_type(room_type_id)
);

-- 6. RESERVATION: planned room dates for one guest.
CREATE TABLE IF NOT EXISTS reservation (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id INT NOT NULL,
    room_id INT NOT NULL,
    staff_id INT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    reservation_status ENUM('BOOKED', 'CANCELLED', 'CHECKED_IN', 'COMPLETED')
        NOT NULL DEFAULT 'BOOKED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_guest FOREIGN KEY (guest_id)
        REFERENCES guest(person_id),
    CONSTRAINT fk_reservation_room FOREIGN KEY (room_id)
        REFERENCES room(room_id),
    CONSTRAINT fk_reservation_staff FOREIGN KEY (staff_id)
        REFERENCES staff(person_id),
    CONSTRAINT chk_reservation_dates CHECK (check_out_date > check_in_date),
    INDEX idx_room_dates (room_id, reservation_status, check_in_date, check_out_date)
);

-- 7. STAY: actual arrival and departure times.
CREATE TABLE IF NOT EXISTS stay (
    stay_id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL UNIQUE,
    actual_check_in DATETIME,
    actual_check_out DATETIME,
    CONSTRAINT fk_stay_reservation FOREIGN KEY (reservation_id)
        REFERENCES reservation(reservation_id)
);

-- 8. PAYMENT: one reservation may have multiple payments.
CREATE TABLE IF NOT EXISTS payment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL,
    staff_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method ENUM('CASH', 'CARD', 'BANK_TRANSFER', 'ONLINE') NOT NULL,
    payment_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_status ENUM('SUCCESSFUL', 'REFUNDED', 'FAILED')
        NOT NULL DEFAULT 'SUCCESSFUL',
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id)
        REFERENCES reservation(reservation_id),
    CONSTRAINT fk_payment_staff FOREIGN KEY (staff_id)
        REFERENCES staff(person_id),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

-- 9. ACTIVITY LOG: records important actions.
CREATE TABLE IF NOT EXISTS activity_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NULL,
    action_description VARCHAR(255) NOT NULL,
    action_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_staff FOREIGN KEY (staff_id)
        REFERENCES staff(person_id)
);

-- SAMPLE DATA
-- These inserts are for a fresh database. Run this sample section only once.
INSERT INTO person (full_name, phone, email) VALUES
('Ananya Rao', '9000000001', 'ananya@example.com'),
('Rohan Mehta', '9000000002', 'rohan@example.com'),
('Kavita Singh', '9000000003', 'kavita@example.com');

INSERT INTO guest (person_id, guest_type, loyalty_points) VALUES
(1, 'LOYALTY', 120),
(2, 'REGULAR', 0);

-- The value below is a placeholder, not a usable password hash.
INSERT INTO staff (person_id, username, password_hash, job_title) VALUES
(3, 'kavita.staff', 'DEMO_ONLY_REPLACE_FOR_REAL_LOGIN', 'Front Desk');

INSERT INTO room_type (type_name, capacity, standard_rate) VALUES
('Single', 1, 2500.00),
('Double', 2, 4000.00),
('Suite', 4, 8500.00);

INSERT INTO room (room_number, floor_number, room_status, room_type_id) VALUES
('101', 1, 'AVAILABLE', 1),
('102', 1, 'AVAILABLE', 2),
('201', 2, 'AVAILABLE', 2),
('301', 3, 'AVAILABLE', 3);

INSERT INTO reservation
    (guest_id, room_id, staff_id, check_in_date, check_out_date)
VALUES (1, 2, 3, '2026-11-10', '2026-11-13');
SET @sample_reservation_id = LAST_INSERT_ID();
INSERT INTO stay (reservation_id) VALUES (@sample_reservation_id);
INSERT INTO payment (reservation_id, staff_id, amount, payment_method)
VALUES (@sample_reservation_id, 3, 2000.00, 'CARD');

-- BASIC QUERIES TO TRY
-- A. List available rooms with their category and price.
SELECT room.room_id, room.room_number, room.floor_number,
       room_type.type_name, room_type.standard_rate
FROM room
JOIN room_type ON room.room_type_id = room_type.room_type_id
WHERE room.room_status = 'AVAILABLE';

-- B. Show reservations with guest and room information.
SELECT reservation.reservation_id, person.full_name,
       room.room_number, reservation.check_in_date,
       reservation.check_out_date, reservation.reservation_status
FROM reservation
JOIN guest ON reservation.guest_id = guest.person_id
JOIN person ON guest.person_id = person.person_id
JOIN room ON reservation.room_id = room.room_id;

-- C. Count reservations for each room (LEFT JOIN includes rooms with none).
SELECT room.room_number, COUNT(reservation.reservation_id) AS booking_count
FROM room
LEFT JOIN reservation ON room.room_id = reservation.room_id
GROUP BY room.room_id, room.room_number;

-- D. Show successful payment total for each reservation.
SELECT reservation_id, SUM(amount) AS amount_paid
FROM payment
WHERE payment_status = 'SUCCESSFUL'
GROUP BY reservation_id;

-- E. Find guests who have at least one reservation.
SELECT person.full_name
FROM person
JOIN guest ON person.person_id = guest.person_id
WHERE EXISTS (
    SELECT 1 FROM reservation
    WHERE reservation.guest_id = guest.person_id
);

-- F. Read the room/category join using an alias.
SELECT r.room_number, t.type_name
FROM room AS r
JOIN room_type AS t ON r.room_type_id = t.room_type_id;
