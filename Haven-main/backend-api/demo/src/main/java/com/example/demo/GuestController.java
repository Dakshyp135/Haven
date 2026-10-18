package com.example.demo;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class GuestController {
    private final JdbcTemplate jdbc;

    public GuestController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/guests")
    public List<Map<String, Object>> guests() {
        return jdbc.queryForList("""
            SELECT p.person_id, p.full_name, p.phone, p.email, g.guest_type
            FROM guest g
            JOIN person p ON p.person_id = g.person_id
            ORDER BY p.full_name
            """);
    }

    @PostMapping("/api/guests")
    @Transactional
    public Map<String, Object> createGuest(@RequestBody GuestRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest name is required.");
        }

        String guestType;
        if ("LOYALTY".equalsIgnoreCase(request.type())) {
            guestType = "LOYALTY";
        } else if ("REGULAR".equalsIgnoreCase(request.type())) {
            guestType = "REGULAR";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose Regular or Loyalty.");
        }

        String name = request.name().trim();
        jdbc.update(
            "INSERT INTO person (full_name, phone, email) VALUES (?, ?, ?)",
            name, request.phone(), request.email());

        Integer personId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

        jdbc.update(
            "INSERT INTO guest (person_id, guest_type) VALUES (?, ?)",
            personId, guestType);

        return Map.of("personId", personId);
    }

    public record GuestRequest(String name, String email, String phone, String type) {}
}