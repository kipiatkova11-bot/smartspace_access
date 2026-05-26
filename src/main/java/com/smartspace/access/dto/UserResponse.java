package com.smartspace.access.dto;

import lombok.Data;
import com.smartspace.access.model.UserRole;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
}