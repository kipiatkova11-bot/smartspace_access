package com.smartspace.access.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull
    private Long workspaceId;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    @Future
    private LocalDateTime endTime;
}