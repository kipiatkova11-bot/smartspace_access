package com.smartspace.access.controller;

import com.smartspace.access.dto.LoginRequest;
import com.smartspace.access.dto.RegisterRequest;
import com.smartspace.access.dto.UserResponse;
import com.smartspace.access.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        var userOpt = authService.login(request);
        if (userOpt.isPresent()) {
            session.setAttribute("currentUser", userOpt.get());
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Неверный email или пароль"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        try {
            UserResponse user = authService.register(request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Выход выполнен"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }
}