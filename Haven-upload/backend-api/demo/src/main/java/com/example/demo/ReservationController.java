package com.example.demo;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final JdbcTemplate jdbc;

    public ReservationController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody ReservationRequest request) {
        if (request.guestId() == null || request.roomId() == null
                || request.checkIn() == null || request.checkOut() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete all reservation fields.");
        }

        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-out must be after check-in.");
        }

        Integer guestExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM guest WHERE person_id = ?",
                Integer.class, request.guestId());
        if (guestExists == null || guestExists == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest not found.");
        }

        Integer roomCanBeBooked = jdbc.queryForObject(
                "SELECT COUNT(*) FROM room WHERE room_id = ? AND room_status <> 'MAINTENANCE'",
                Integer.class, request.roomId());
        if (roomCanBeBooked == null || roomCanBeBooked == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room not found or under maintenance.");
        }

        Integer overlaps = jdbc.queryForObject("""
                SELECT COUNT(*) FROM reservation
                WHERE room_id = ?
                  AND reservation_status IN ('BOOKED', 'CHECKED_IN')
                  AND ? < check_out_date AND ? > check_in_date
                """, Integer.class, request.roomId(),
                Date.valueOf(request.checkIn()), Date.valueOf(request.checkOut()));

        if (overlaps != null && overlaps > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already booked for those dates.");
        }

        jdbc.update("""
                INSERT INTO reservation
                    (guest_id, room_id, staff_id, check_in_date, check_out_date)
                VALUES (?, ?, 3, ?, ?)
                """, request.guestId(), request.roomId(),
                Date.valueOf(request.checkIn()), Date.valueOf(request.checkOut()));

        Integer reservationId = jdbc.queryForObject(
                "SELECT LAST_INSERT_ID()", Integer.class);

        jdbc.update("INSERT INTO stay (reservation_id) VALUES (?)", reservationId);

        return Map.of("reservationId", reservationId, "status", "BOOKED");
    }

    public record ReservationRequest(
            Integer guestId,
            Integer roomId,
            LocalDate checkIn,
            LocalDate checkOut) {}
}