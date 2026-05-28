package com.smartspace.access.controller;

import com.smartspace.access.dto.UserResponse;
import com.smartspace.access.model.Invoice;
import com.smartspace.access.model.InvoiceStatus;
import com.smartspace.access.repository.InvoiceRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    /**
     * Получение всех счетов текущего пользователя (для клиента)
     * GET /api/invoices/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<Invoice>> getMyInvoices(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<Invoice> invoices = invoiceRepository.findByClientId(user.getId());
        return ResponseEntity.ok(invoices);
    }

    /**
     * Получение всех счетов (только для ADMIN и ACCOUNTANT)
     * GET /api/invoices/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<Invoice>> getAllInvoices(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // Проверка прав: только ADMIN или ACCOUNTANT
        if (!"ADMIN".equals(user.getRole().name()) && !"ACCOUNTANT".equals(user.getRole().name())) {
            return ResponseEntity.status(403).build();
        }

        List<Invoice> invoices = invoiceRepository.findAll();
        return ResponseEntity.ok(invoices);
    }

    /**
     * Получение счета по ID
     * GET /api/invoices/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id, HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }

        // Проверка прав: владелец счета, ADMIN или ACCOUNTANT
        if (!invoice.getClientId().equals(user.getId())
                && !"ADMIN".equals(user.getRole().name())
                && !"ACCOUNTANT".equals(user.getRole().name())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(invoice);
    }

    /**
     * Подтверждение счета бухгалтером (перевод из DRAFT в PENDING)
     * POST /api/invoices/{id}/review
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<Map<String, Object>> reviewInvoice(@PathVariable Long id, HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }

        // Проверка роли: только ACCOUNTANT или ADMIN
        if (!"ACCOUNTANT".equals(user.getRole().name()) && !"ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ только для бухгалтера"));
        }

        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }

        // Проверка, что счет в статусе DRAFT
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            return ResponseEntity.badRequest().body(Map.of("error", "Счет уже был проверен"));
        }

        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setReviewedBy(user.getId());
        invoice.setReviewedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Счет подтвержден и готов к оплате");
        return ResponseEntity.ok(response);
    }

    /**
     * Отметка об оплате счета (после получения платежа)
     * POST /api/invoices/{id}/pay
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<Map<String, Object>> payInvoice(@PathVariable Long id, HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }

        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }

        // Проверка прав: только владелец счета, ADMIN или ACCOUNTANT
        if (!invoice.getClientId().equals(user.getId())
                && !"ADMIN".equals(user.getRole().name())
                && !"ACCOUNTANT".equals(user.getRole().name())) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен"));
        }

        // Проверка, что счет в статусе PENDING
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Счет не может быть оплачен (текущий статус: " + invoice.getStatus() + ")"));
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Счет успешно оплачен");
        return ResponseEntity.ok(response);
    }

    /**
     * Получение счетов по статусу (только для ADMIN и ACCOUNTANT)
     * GET /api/invoices/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Invoice>> getInvoicesByStatus(@PathVariable String status, HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // Проверка прав: только ADMIN или ACCOUNTANT
        if (!"ADMIN".equals(user.getRole().name()) && !"ACCOUNTANT".equals(user.getRole().name())) {
            return ResponseEntity.status(403).build();
        }

        InvoiceStatus invoiceStatus;
        try {
            invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        List<Invoice> invoices = invoiceRepository.findByStatus(invoiceStatus);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Получение счетов клиента по ID (для администратора и бухгалтера)
     * GET /api/invoices/client/{clientId}
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Invoice>> getInvoicesByClientId(@PathVariable Long clientId, HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // Проверка прав: ADMIN, ACCOUNTANT или сам клиент
        if (!clientId.equals(user.getId())
                && !"ADMIN".equals(user.getRole().name())
                && !"ACCOUNTANT".equals(user.getRole().name())) {
            return ResponseEntity.status(403).build();
        }

        List<Invoice> invoices = invoiceRepository.findByClientId(clientId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Статистика по счетам (для ADMIN и ACCOUNTANT)
     * GET /api/invoices/stats/summary
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getInvoiceStats(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // Проверка прав: только ADMIN или ACCOUNTANT
        if (!"ADMIN".equals(user.getRole().name()) && !"ACCOUNTANT".equals(user.getRole().name())) {
            return ResponseEntity.status(403).build();
        }

        List<Invoice> allInvoices = invoiceRepository.findAll();

        long draftCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.DRAFT).count();
        long pendingCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PENDING).count();
        long paidCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();

        double totalAmount = allInvoices.stream()
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();

        double paidAmount = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();

        double pendingAmount = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING)
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", allInvoices.size());
        stats.put("draftCount", draftCount);
        stats.put("pendingCount", pendingCount);
        stats.put("paidCount", paidCount);
        stats.put("totalAmount", totalAmount);
        stats.put("paidAmount", paidAmount);
        stats.put("pendingAmount", pendingAmount);

        return ResponseEntity.ok(stats);
    }
}