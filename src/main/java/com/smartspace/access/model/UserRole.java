package com.smartspace.access.model;

import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

public enum UserRole {
    CLIENT, ADMIN, ACCOUNTANT;
}