package com.smartspace.access.repository;

import com.smartspace.access.model.Booking;
import com.smartspace.access.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByClientId(Long clientId);
    List<Booking> findByWorkspaceId(Long workspaceId);
    List<Booking> findByClientIdAndStatus(Long clientId, BookingStatus status);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE " +
            "b.workspaceId = :workspaceId AND " +
            "b.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
            "b.startTime < :endTime AND " +
            "b.endTime > :startTime")
    boolean isWorkspaceOccupied(@Param("workspaceId") Long workspaceId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}