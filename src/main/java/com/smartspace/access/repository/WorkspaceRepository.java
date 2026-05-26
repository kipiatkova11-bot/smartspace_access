package com.smartspace.access.repository;

import com.smartspace.access.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByIsActiveTrue();
    Optional<Workspace> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT w FROM Workspace w WHERE w.isActive = true")
    List<Workspace> findAllAvailable();
}