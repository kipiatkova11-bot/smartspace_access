package com.smartspace.access.service;

import com.smartspace.access.dto.BookingRequest;
import com.smartspace.access.dto.BookingResponse;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final VisitRepository visitRepository;

    private static final int ROUNDING_MINUTES = 15;

    public boolean isWorkspaceFree(Long workspaceId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) return false;
        if (startTime.isBefore(LocalDateTime.now())) return false;
        return !bookingRepository.isWorkspaceOccupied(workspaceId, startTime, endTime);
    }

    public BigDecimal calculateCost(BigDecimal hourlyRate, LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) return BigDecimal.ZERO;

        long roundedMinutes = (long) Math.ceil(minutes / (double) ROUNDING_MINUTES) * ROUNDING_MINUTES;
        BigDecimal hours = BigDecimal.valueOf(roundedMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        return hours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public BookingResponse createBooking(Long clientId, BookingRequest request) {
        BookingResponse response = new BookingResponse();

        Workspace workspace = workspaceRepository.findByIdAndIsActiveTrue(request.getWorkspaceId())
                .orElse(null);
        if (workspace == null) {
            response.setSuccess(false);
            response.setMessage("Рабочее место не найдено");
            return response;
        }

        if (request.getStartTime().isAfter(request.getEndTime())) {
            response.setSuccess(false);
            response.setMessage("Время окончания должно быть позже времени начала");
            return response;
        }

        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            response.setSuccess(false);
            response.setMessage("Нельзя бронировать прошедшее время");
            return response;
        }

        if (!isWorkspaceFree(request.getWorkspaceId(), request.getStartTime(), request.getEndTime())) {
            response.setSuccess(false);
            response.setMessage("Рабочее место уже занято в указанное время");
            return response;
        }

        BigDecimal totalPrice = calculateCost(workspace.getHourlyRate(),
                request.getStartTime(), request.getEndTime());

        Booking booking = new Booking();
        booking.setClientId(clientId);
        booking.setWorkspaceId(request.getWorkspaceId());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        Visit visit = new Visit();
        visit.setBookingId(saved.getId());
        visitRepository.save(visit);

        response.setSuccess(true);
        response.setBookingId(saved.getId());
        response.setMessage("Бронирование успешно создано");
        response.setTotalPrice(totalPrice);

        long minutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        double hours = Math.ceil(minutes / (double) ROUNDING_MINUTES) * ROUNDING_MINUTES / 60.0;
        response.setTotalHours(hours);

        log.info("Создано бронирование #{} для клиента #{}", saved.getId(), clientId);
        return response;
    }

    @Transactional
    public boolean cancelBooking(Long bookingId, Long clientId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !booking.getClientId().equals(clientId)) {
            return false;
        }

        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
        return true;
    }

    public List<Booking> getClientBookings(Long clientId) {
        return bookingRepository.findByClientIdAndStatus(clientId, BookingStatus.CONFIRMED);
    }

    public List<Workspace> getAllWorkspaces() {
        return workspaceRepository.findAllAvailable();
    }
}