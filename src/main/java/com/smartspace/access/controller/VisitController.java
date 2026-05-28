package com.smartspace.access.controller;

import com.smartspace.access.dto.UserResponse;
import com.smartspace.access.model.Booking;
import com.smartspace.access.model.BookingStatus;
import com.smartspace.access.model.Visit;
import com.smartspace.access.model.Workspace;
import com.smartspace.access.repository.BookingRepository;
import com.smartspace.access.repository.VisitRepository;
import com.smartspace.access.repository.WorkspaceRepository;
import com.smartspace.access.service.InvoiceService;
import com.smartspace.access.service.VisitService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
@Slf4j
public class VisitController {

    private final VisitService visitService;
    private final VisitRepository visitRepository;
    private final BookingRepository bookingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final InvoiceService invoiceService;

    private static final int ROUNDING_MINUTES = 15;

    /**
     * Отметка прихода клиента (только для ADMIN)
     * POST /api/visits/{bookingId}/checkin
     */
    @PostMapping("/{bookingId}/checkin")
    public ResponseEntity<Map<String, Object>> checkIn(
            @PathVariable Long bookingId,
            HttpSession session) {

        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Необходимо авторизоваться"
            ));
        }

        if (!"ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Доступ только для администратора"
            ));
        }

        boolean result = visitService.checkIn(bookingId, user.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);

        if (!result) {
            response.put("error", "Не удалось отметить приход. Проверьте, что бронирование существует и не началось слишком рано.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Отметка ухода клиента с автоматическим расчётом стоимости (только для ADMIN)
     * POST /api/visits/{bookingId}/checkout
     */
    @PostMapping("/{bookingId}/checkout")
    public ResponseEntity<Map<String, Object>> checkOut(
            @PathVariable Long bookingId,
            HttpSession session) {

        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Необходимо авторизоваться"
            ));
        }

        if (!"ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Доступ только для администратора"
            ));
        }

        BigDecimal cost = visitService.checkOut(bookingId, user.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", cost.compareTo(BigDecimal.ZERO) > 0);
        response.put("cost", cost);

        if (cost.compareTo(BigDecimal.ZERO) <= 0) {
            response.put("error", "Не удалось отметить уход. Проверьте, что приход был отмечен.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Получение информации о посещении по ID бронирования
     * GET /api/visits/booking/{bookingId}
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getVisitByBookingId(@PathVariable Long bookingId) {
        Visit visit = visitRepository.findByBookingId(bookingId).orElse(null);
        if (visit == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Посещение не найдено"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", visit.getId());
        response.put("bookingId", visit.getBookingId());
        response.put("checkInTime", visit.getCheckInTime());
        response.put("checkOutTime", visit.getCheckOutTime());
        response.put("totalHours", visit.getTotalHours());
        response.put("calculatedCost", visit.getCalculatedCost());

        return ResponseEntity.ok(response);
    }

    /**
     * Получение всех посещений (только для ADMIN)
     * GET /api/visits/all
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllVisits(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null || !"ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещён"));
        }

        List<Visit> visits = visitRepository.findAll();
        return ResponseEntity.ok(visits);
    }

    /**
     * ТЕСТОВЫЙ МЕТОД: Принудительный расчёт стоимости без отметок администратора
     * Использует время начала и окончания из бронирования
     * POST /api/visits/{bookingId}/calculate-from-booking
     */
    @PostMapping("/{bookingId}/calculate-from-booking")
    public ResponseEntity<Map<String, Object>> calculateFromBooking(
            @PathVariable Long bookingId,
            HttpSession session) {

        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Необходимо авторизоваться"
            ));
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Бронирование не найдено"
            ));
        }

        // Проверяем, что пользователь владелец бронирования или ADMIN
        if (!"ADMIN".equals(user.getRole().name()) && !booking.getClientId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Доступ запрещён"
            ));
        }

        Visit visit = visitRepository.findByBookingId(bookingId).orElse(null);
        if (visit == null) {
            visit = new Visit();
            visit.setBookingId(bookingId);
        }

        // Устанавливаем время прихода = время начала бронирования
        visit.setCheckInTime(booking.getStartTime());

        // Устанавливаем время ухода = время окончания бронирования
        visit.setCheckOutTime(booking.getEndTime());

        // Расчёт стоимости
        Workspace workspace = workspaceRepository.findById(booking.getWorkspaceId()).orElse(null);
        if (workspace == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Рабочее место не найдено"
            ));
        }

        // Вычисляем длительность в минутах
        long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();

        // Округление до 15 минут вверх (коворкинг-стандарт)
        long roundedMinutes = (long) Math.ceil(minutes / (double) ROUNDING_MINUTES) * ROUNDING_MINUTES;

        // Переводим в часы
        double hours = roundedMinutes / 60.0;
        BigDecimal totalHours = BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);

        // Рассчитываем стоимость
        BigDecimal calculatedCost = totalHours.multiply(workspace.getHourlyRate())
                .setScale(2, RoundingMode.HALF_UP);

        visit.setTotalHours(totalHours);
        visit.setCalculatedCost(calculatedCost);

        visitRepository.save(visit);

        // Обновляем статус бронирования
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        // Создаём счёт
        invoiceService.createInvoice(visit.getId(), booking.getClientId(), calculatedCost);

        log.info("Расчёт для бронирования #{}. Часов: {}, Стоимость: {} руб.",
                bookingId, totalHours, calculatedCost);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("bookingId", bookingId);
        response.put("totalHours", totalHours);
        response.put("cost", calculatedCost);
        response.put("roundedMinutes", roundedMinutes);

        return ResponseEntity.ok(response);
    }

    /**
     * Получение всех бронирований, требующих отметки прихода (для ADMIN)
     * GET /api/visits/pending-checkins
     */
    @GetMapping("/pending-checkins")
    public ResponseEntity<?> getPendingCheckins(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null || !"ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещён"));
        }

        // Находим все подтверждённые бронирования, у которых нет отметки о приходе
        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        List<Map<String, Object>> result = confirmedBookings.stream()
                .filter(booking -> {
                    Visit visit = visitRepository.findByBookingId(booking.getId()).orElse(null);
                    return visit == null || visit.getCheckInTime() == null;
                })
                .map(booking -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("bookingId", booking.getId());
                    item.put("clientId", booking.getClientId());
                    item.put("workspaceId", booking.getWorkspaceId());
                    item.put("startTime", booking.getStartTime());
                    item.put("endTime", booking.getEndTime());
                    return item;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Получение всех активных посещений (отмечен приход, но нет ухода) для ADMIN
     * GET /api/visits/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveVisits(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null || !"ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещён"));
        }

        List<Visit> activeVisits = visitRepository.findAll().stream()
                .filter(v -> v.getCheckInTime() != null && v.getCheckOutTime() == null)
                .toList();

        List<Map<String, Object>> result = activeVisits.stream().map(visit -> {
            Map<String, Object> item = new HashMap<>();
            item.put("visitId", visit.getId());
            item.put("bookingId", visit.getBookingId());
            item.put("checkInTime", visit.getCheckInTime());

            Booking booking = bookingRepository.findById(visit.getBookingId()).orElse(null);
            if (booking != null) {
                item.put("clientId", booking.getClientId());
                item.put("workspaceId", booking.getWorkspaceId());
                item.put("startTime", booking.getStartTime());
                item.put("endTime", booking.getEndTime());
            }
            return item;
        }).toList();

        return ResponseEntity.ok(result);
    }
}