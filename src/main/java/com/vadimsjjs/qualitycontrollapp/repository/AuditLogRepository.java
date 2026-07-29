package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE a.userPersonnelNumber = :userId ORDER BY a.eventDate DESC")
    List<AuditLog> findByUser(@Param("userId") Long userId);

    @Query("SELECT a FROM AuditLog a WHERE a.tableName = :tableName AND a.recordId = :recordId ORDER BY a.eventDate DESC")
    List<AuditLog> findByRecord(@Param("tableName") String tableName, @Param("recordId") Long recordId);

    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType ORDER BY a.eventDate DESC")
    List<AuditLog> findByEventType(@Param("eventType") String eventType);

    @Query("SELECT a FROM AuditLog a WHERE a.eventDate BETWEEN :start AND :end ORDER BY a.eventDate DESC")
    List<AuditLog> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}