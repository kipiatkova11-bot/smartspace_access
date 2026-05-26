package com.smartspace.access.controller;

import com.smartspace.access.dto.UserResponse;
import com.smartspace.access.service.VisitService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping("/{bookingId}/checkin")
    public ResponseEntity<Map<String, Object>> checkIn(@PathVariable Long bookingId,
                                                       HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null || !"ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", "Доступ только для администратора"));
        }

        boolean result = visitService.checkIn(bookingId, user.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookingId}/checkout")
    public ResponseEntity<Map<String, Object>> checkOut(@PathVariable Long bookingId,
                                                        HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null || !"ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", "Доступ только для администратора"));
        }

        BigDecimal cost = visitService.checkOut(bookingId, user.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", cost.compareTo(BigDecimal.ZERO) > 0);
        response.put("cost", cost);
        return ResponseEntity.ok(response);
    }
}