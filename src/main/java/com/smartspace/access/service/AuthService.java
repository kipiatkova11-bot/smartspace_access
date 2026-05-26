package com.smartspace.access.service;

import com.smartspace.access.dto.LoginRequest;
import com.smartspace.access.dto.RegisterRequest;
import com.smartspace.access.dto.UserResponse;
import com.smartspace.access.model.User;
import com.smartspace.access.model.UserRole;
import com.smartspace.access.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    public Optional<UserResponse> login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPasswordHash().equals(request.getPassword())) {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);

                UserResponse response = new UserResponse();
                response.setId(user.getId());
                response.setEmail(user.getEmail());
                response.setFullName(user.getFullName());
                response.setPhone(user.getPhone());
                response.setRole(user.getRole());
                return Optional.of(response);
            }
        }
        return Optional.empty();
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email уже зарегистрирован");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(request.getPassword());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.CLIENT);
        user.setIsActive(true);

        User saved = userRepository.save(user);
        log.info("Зарегистрирован новый пользователь: {}", saved.getEmail());

        UserResponse response = new UserResponse();
        response.setId(saved.getId());
        response.setEmail(saved.getEmail());
        response.setFullName(saved.getFullName());
        response.setPhone(saved.getPhone());
        response.setRole(saved.getRole());
        return response;
    }
}