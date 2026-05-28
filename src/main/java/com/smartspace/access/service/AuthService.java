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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    /**
     * Аутентификация пользователя
     * @param request данные для входа (email, password)
     * @return Optional с данными пользователя при успешном входе
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> login(LoginRequest request) {
        log.debug("Попытка входа пользователя: {}", request.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Проверка, активен ли пользователь
            if (user.getIsActive() == null || !user.getIsActive()) {
                log.warn("Попытка входа в заблокированную учётную запись: {}", request.getEmail());
                return Optional.empty();
            }

            // Проверка пароля (в production использовать BCryptPasswordEncoder)
            if (user.getPasswordHash().equals(request.getPassword())) {
                // Обновляем время последнего входа
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);

                log.info("Успешный вход пользователя: {}", request.getEmail());

                // Формируем ответ
                UserResponse response = new UserResponse();
                response.setId(user.getId());
                response.setEmail(user.getEmail());
                response.setFullName(user.getFullName());
                response.setPhone(user.getPhone());
                response.setRole(user.getRole());
                return Optional.of(response);
            } else {
                log.warn("Неверный пароль для пользователя: {}", request.getEmail());
            }
        } else {
            log.warn("Пользователь не найден: {}", request.getEmail());
        }

        return Optional.empty();
    }

    /**
     * Регистрация нового пользователя
     * @param request данные для регистрации (email, fullName, phone, password)
     * @return данные созданного пользователя
     * @throws RuntimeException если пользователь с таким email уже существует
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.debug("Попытка регистрации пользователя: {}", request.getEmail());

        // Проверка на существующего пользователя
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Попытка регистрации с существующим email: {}", request.getEmail());
            throw new RuntimeException("Пользователь с таким email уже зарегистрирован");
        }

        // Создание нового пользователя
        User user = new User();
        user.setEmail(request.getEmail());

        // В production использовать BCryptPasswordEncoder:
        // user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPasswordHash(request.getPassword()); // Для демо-режима
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.CLIENT);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("Успешная регистрация пользователя: {} (ID: {})", saved.getEmail(), saved.getId());

        // Формируем ответ
        UserResponse response = new UserResponse();
        response.setId(saved.getId());
        response.setEmail(saved.getEmail());
        response.setFullName(saved.getFullName());
        response.setPhone(saved.getPhone());
        response.setRole(saved.getRole());
        return response;
    }

    /**
     * Получение информации о пользователе по ID
     * @param userId идентификатор пользователя
     * @return Optional с данными пользователя
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::mapToResponse);
    }

    /**
     * Получение информации о пользователе по email
     * @param email электронная почта пользователя
     * @return Optional с данными пользователя
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToResponse);
    }

    /**
     * Обновление профиля пользователя
     * @param userId идентификатор пользователя
     * @param fullName новое ФИО
     * @param phone новый номер телефона
     * @return обновлённые данные пользователя
     */
    @Transactional
    public Optional<UserResponse> updateProfile(Long userId, String fullName, String phone) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (fullName != null && !fullName.trim().isEmpty()) {
                user.setFullName(fullName);
            }
            if (phone != null && !phone.trim().isEmpty()) {
                user.setPhone(phone);
            }
            user.setUpdatedAt(LocalDateTime.now());

            User saved = userRepository.save(user);
            log.info("Обновлён профиль пользователя: {} (ID: {})", saved.getEmail(), saved.getId());

            return Optional.of(mapToResponse(saved));
        }

        return Optional.empty();
    }

    /**
     * Смена пароля пользователя
     * @param userId идентификатор пользователя
     * @param oldPassword старый пароль
     * @param newPassword новый пароль
     * @return true если пароль успешно изменён, false если старый пароль неверен
     */
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Проверка старого пароля
            if (!user.getPasswordHash().equals(oldPassword)) {
                log.warn("Неверный старый пароль при смене для пользователя: {}", user.getEmail());
                return false;
            }

            // Установка нового пароля (в production использовать BCrypt)
            user.setPasswordHash(newPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Пароль успешно изменён для пользователя: {}", user.getEmail());
            return true;
        }

        return false;
    }

    /**
     * Блокировка/разблокировка пользователя (только для ADMIN)
     * @param userId идентификатор пользователя
     * @param isActive статус активности
     * @return true если операция успешна
     */
    @Transactional
    public boolean setUserActiveStatus(Long userId, boolean isActive) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(isActive);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Статус активности пользователя {} изменён на: {}", user.getEmail(), isActive);
            return true;
        }

        return false;
    }

    /**
     * Изменение роли пользователя (только для ADMIN)
     * @param userId идентификатор пользователя
     * @param role новая роль
     * @return true если операция успешна
     */
    @Transactional
    public boolean changeUserRole(Long userId, UserRole role) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRole(role);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Роль пользователя {} изменена на: {}", user.getEmail(), role);
            return true;
        }

        return false;
    }

    /**
     * Преобразование сущности User в UserResponse
     * @param user сущность пользователя
     * @return DTO для ответа
     */
    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        return response;
    }
}