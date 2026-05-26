package com.smartspace.access.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class VisitRequest {
    @NotNull
    private Long bookingId;
}