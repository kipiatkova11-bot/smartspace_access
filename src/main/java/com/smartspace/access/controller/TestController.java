package com.smartspace.access.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/db")
    public Map<String, Object> testConnection() {
        try {
            String result = jdbcTemplate.queryForObject(
                    "SELECT '✅ Подключение к БД успешно!' as message",
                    String.class
            );
            return Map.of(
                    "status", "success",
                    "message", result,
                    "database", "smartspace_access"
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }
}