package com.smartspace.access.service;

import com.smartspace.access.model.*;
import com.smartspace.access.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitService {

    private final VisitRepository visitRepository;
    private final BookingRepository bookingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final InvoiceService invoiceService;

    private static final int ROUNDING_MINUTES = 15;

    @Transactional
    public boolean checkIn(Long bookingId, Long adminId) {
        Visit visit = visitRepository.findByBookingId(bookingId).orElse(null);
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (visit == null || booking == null) {
            return false;
        }

        if (booking.getStartTime().isAfter(LocalDateTime.now().plusMinutes(30))) {
            return false;
        }

        visit.setCheckInTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.CHECKED_IN);

        visitRepository.save(visit);
        bookingRepository.save(booking);

        log.info("Зафиксирован приход по бронированию #{}", bookingId);
        return true;
    }

    @Transactional
    public BigDecimal checkOut(Long bookingId, Long adminId) {
        // Получаем данные
        Visit visit = visitRepository.findByBookingId(bookingId).orElse(null);
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (visit == null || booking == null) {
            log.warn("Бронирование #{} не найдено", bookingId);
            return BigDecimal.ZERO;
        }

        if (visit.getCheckInTime() == null) {
            log.warn("Нет отметки о приходе по бронированию #{}", bookingId);
            return BigDecimal.ZERO;
        }

        Workspace workspace = workspaceRepository.findById(booking.getWorkspaceId()).orElse(null);
        if (workspace == null) {
            log.warn("Рабочее место #{} не найдено", booking.getWorkspaceId());
            return BigDecimal.ZERO;
        }

        // Фиксируем время ухода
        LocalDateTime checkOutTime = LocalDateTime.now();
        visit.setCheckOutTime(checkOutTime);

        // Расчет минут пребывания (правильно: уход - приход)
        long minutes = Duration.between(visit.getCheckInTime(), checkOutTime).toMinutes();

        // Округление до 15 минут вверх (как в триггере из курсовой)
        long roundedMinutes = (long) Math.ceil(minutes / (double) ROUNDING_MINUTES) * ROUNDING_MINUTES;

        // Переводим в часы
        BigDecimal totalHours = BigDecimal.valueOf(roundedMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // Расчет стоимости
        BigDecimal calculatedCost = totalHours.multiply(workspace.getHourlyRate())
                .setScale(2, RoundingMode.HALF_UP);

        // Сохраняем результаты
        visit.setTotalHours(totalHours);
        visit.setCalculatedCost(calculatedCost);

        // Обновляем статус бронирования
        booking.setStatus(BookingStatus.COMPLETED);

        // Сохраняем в БД
        visitRepository.save(visit);
        bookingRepository.save(booking);

        // Создаем счет (нужна сумма, а не bookingId!)
        invoiceService.createInvoice(visit.getId(), booking.getClientId(), calculatedCost);

        log.info("Зафиксирован уход по бронированию #{} | Часов: {} | Сумма: {} руб.",
                bookingId, totalHours, calculatedCost);

        return calculatedCost;
    }
}