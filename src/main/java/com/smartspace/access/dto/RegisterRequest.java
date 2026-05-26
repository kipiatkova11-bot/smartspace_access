package com.smartspace.access.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank
    private String phone;

    @NotBlank
    @Size(min = 6)
    private String password;
}