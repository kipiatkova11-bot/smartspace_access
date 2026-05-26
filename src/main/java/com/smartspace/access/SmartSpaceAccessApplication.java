package com.smartspace.access;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartSpaceAccessApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartSpaceAccessApplication.class, args);
        System.out.println("=== SmartSpace Access запущен! ===");
        System.out.println("Доступно по адресу: http://localhost:8080");
    }
}