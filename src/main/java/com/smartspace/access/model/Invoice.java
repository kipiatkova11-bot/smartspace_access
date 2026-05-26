package com.smartspace.access.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "pdf_link")
    private String pdfLink;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "sent_to_bank_at")
    private LocalDateTime sentToBankAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Конструкторы
    public Invoice() {}

    public Invoice(Long visitId, Long clientId, BigDecimal amount) {
        this.visitId = visitId;
        this.clientId = clientId;
        this.amount = amount;
    }

    // Getters
    public Long getId() { return id; }
    public Long getVisitId() { return visitId; }
    public Long getClientId() { return clientId; }
    public BigDecimal getAmount() { return amount; }
    public InvoiceStatus getStatus() { return status; }
    public String getPdfLink() { return pdfLink; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getSentToBankAt() { return sentToBankAt; }
    public LocalDateTime getPaidAt() { return paidAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public void setPdfLink(String pdfLink) { this.pdfLink = pdfLink; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setSentToBankAt(LocalDateTime sentToBankAt) { this.sentToBankAt = sentToBankAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}