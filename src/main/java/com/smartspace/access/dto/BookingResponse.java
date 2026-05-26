package com.smartspace.access.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BookingResponse {
    private Long bookingId;
    private String message;
    private boolean success;
    private BigDecimal totalPrice;
    private Double totalHours;
}