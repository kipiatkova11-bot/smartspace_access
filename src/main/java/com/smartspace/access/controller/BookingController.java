package com.smartspace.access.controller;

import com.smartspace.access.dto.BookingRequest;
import com.smartspace.access.dto.BookingResponse;
import com.smartspace.access.dto.UserResponse;
import com.smartspace.access.model.Booking;
import com.smartspace.access.model.Workspace;
import com.smartspace.access.service.BookingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/workspaces")
    public ResponseEntity<List<Workspace>> getWorkspaces() {
        return ResponseEntity.ok(bookingService.getAllWorkspaces());
    }

    @PostMapping("/create")
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request,
                                                         HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("currentUser");
        if (currentUser == null) {
            BookingResponse error = new BookingResponse();
            error.setSuccess(false);
            error.setMessage("Необходимо авторизоваться");
            return ResponseEntity.status(401).body(error);
        }

        BookingResponse response = bookingService.createBooking(currentUser.getId(), request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long bookingId,
                                                             HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false));
        }

        boolean result = bookingService.cancelBooking(bookingId, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings(HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(bookingService.getClientBookings(currentUser.getId()));
    }
}