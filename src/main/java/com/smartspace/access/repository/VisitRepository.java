package com.smartspace.access.repository;

import com.smartspace.access.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {
    Optional<Visit> findByBookingId(Long bookingId);
}