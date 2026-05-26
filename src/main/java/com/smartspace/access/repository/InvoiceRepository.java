package com.smartspace.access.repository;

import com.smartspace.access.model.Invoice;
import com.smartspace.access.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByClientId(Long clientId);
    List<Invoice> findByStatus(InvoiceStatus status);
    Optional<Invoice> findByVisitId(Long visitId);
}