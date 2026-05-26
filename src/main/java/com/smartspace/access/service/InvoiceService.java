package com.smartspace.access.service;

import com.smartspace.access.model.Invoice;
import com.smartspace.access.model.InvoiceStatus;
import com.smartspace.access.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Создание счета после завершения посещения
     * Аналог создания квитанции из курсовой работы
     */
    @Transactional
    public Invoice createInvoice(Long visitId, Long clientId, BigDecimal amount) {
        Invoice invoice = new Invoice();
        invoice.setVisitId(visitId);
        invoice.setClientId(clientId);
        invoice.setAmount(amount);
        invoice.setStatus(InvoiceStatus.DRAFT);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Создан счет #{} для клиента #{} на сумму {} руб.", saved.getId(), clientId, amount);
        return saved;
    }

    /**
     * Проверка счета бухгалтером (требование FR.09 из курсовой)
     */
    @Transactional
    public boolean reviewInvoice(Long invoiceId, Long accountantId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            log.warn("Счет #{} не найден для проверки", invoiceId);
            return false;
        }

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            log.warn("Счет #{} уже был проверен или оплачен", invoiceId);
            return false;
        }

        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setReviewedBy(accountantId);
        invoice.setReviewedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        log.info("Счет #{} проверен бухгалтером #{}", invoiceId, accountantId);
        return true;
    }

    /**
     * Отправка счета в банк (интеграция с платежной системой)
     */
    @Transactional
    public boolean sendToBank(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            log.warn("Счет #{} не найден для отправки в банк", invoiceId);
            return false;
        }

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            log.warn("Счет #{} не готов к отправке в банк (статус: {})", invoiceId, invoice.getStatus());
            return false;
        }

        invoice.setStatus(InvoiceStatus.SENT_TO_BANK);
        invoice.setSentToBankAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        log.info("Счет #{} отправлен в банк", invoiceId);
        return true;
    }

    /**
     * Отметка об оплате счета (после получения платежа)
     */
    @Transactional
    public boolean markAsPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            log.warn("Счет #{} не найден для отметки оплаты", invoiceId);
            return false;
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        log.info("Счет #{} оплачен клиентом #{}", invoiceId, invoice.getClientId());
        return true;
    }

    /**
     * Получение счета по ID
     */
    public Invoice getInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId).orElse(null);
    }

    /**
     * Получение всех счетов клиента
     */
    public List<Invoice> getInvoicesByClient(Long clientId) {
        return invoiceRepository.findByClientId(clientId);
    }

    /**
     * Получение всех счетов со статусом DRAFT (ожидают проверки бухгалтера)
     */
    public List<Invoice> getDraftInvoices() {
        return invoiceRepository.findByStatus(InvoiceStatus.DRAFT);
    }

    /**
     * Получение всех счетов со статусом PENDING (ожидают оплаты)
     */
    public List<Invoice> getPendingInvoices() {
        return invoiceRepository.findByStatus(InvoiceStatus.PENDING);
    }

    /**
     * Получение счета по ID посещения
     */
    public Invoice getInvoiceByVisitId(Long visitId) {
        return invoiceRepository.findByVisitId(visitId).orElse(null);
    }

    /**
     * Формирование PDF-квитанции (заглушка - в реальном проекте интеграция с PDF-генератором)
     * Требование FR.04 из курсовой работы
     */
    public String generatePdfLink(Long invoiceId) {
        Invoice invoice = getInvoice(invoiceId);
        if (invoice == null) {
            return null;
        }

        // В реальном проекте здесь будет генерация PDF
        String pdfLink = "/invoices/invoice_" + invoiceId + ".pdf";
        invoice.setPdfLink(pdfLink);
        invoiceRepository.save(invoice);

        log.info("Сгенерирован PDF для счета #{}: {}", invoiceId, pdfLink);
        return pdfLink;
    }
}