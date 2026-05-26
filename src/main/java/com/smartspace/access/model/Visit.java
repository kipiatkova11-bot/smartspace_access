package com.smartspace.access.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "visits")
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "total_hours", precision = 5, scale = 2)
    private BigDecimal totalHours = BigDecimal.ZERO;

    @Column(name = "calculated_cost", precision = 10, scale = 2)
    private BigDecimal calculatedCost = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ========== КОНСТРУКТОРЫ ==========
    public Visit() {}

    public Visit(Long bookingId) {
        this.bookingId = bookingId;
    }

    // ========== ГЕТТЕРЫ ==========
    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public BigDecimal getCalculatedCost() {
        return calculatedCost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ========== СЕТТЕРЫ ==========
    public void setId(Long id) {
        this.id = id;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public void setCalculatedCost(BigDecimal calculatedCost) {
        this.calculatedCost = calculatedCost;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}