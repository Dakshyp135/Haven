package com.example.demo;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {
    private final JdbcTemplate jdbc;

    public RoomController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/rooms")
    public List<Map<String, Object>> rooms() {
        return jdbc.queryForList("""
            SELECT r.room_id, r.room_number, r.floor_number, r.room_status,
                   t.type_name, t.capacity, t.standard_rate
            FROM room r
            JOIN room_type t ON t.room_type_id = r.room_type_id
            ORDER BY r.room_number
            """);
    }
}