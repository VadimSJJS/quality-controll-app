package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.entity.AuditLog;
import com.vadimsjjs.qualitycontrollapp.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void log(String eventType, String tableName, Long recordId, Object oldValue, Object newValue) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUserPersonnelNumber(getCurrentUser());
            logEntry.setEventType(eventType);
            logEntry.setTableName(tableName);
            logEntry.setRecordId(recordId);
            logEntry.setOldValue(toJson(oldValue));
            logEntry.setNewValue(toJson(newValue));
            logEntry.setIpAddress(getClientIp());
            logEntry.setUserAgent(getUserAgent());
            logEntry.setEventDate(LocalDateTime.now());

            auditLogRepository.save(logEntry);
            log.debug("Audit log saved: {} on table {} record {}", eventType, tableName, recordId);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    public void logLogin(Long personnelNumber, String ip, String userAgent) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUserPersonnelNumber(personnelNumber);
            logEntry.setEventType("LOGIN");
            logEntry.setTableName("USER");
            logEntry.setRecordId(personnelNumber);
            logEntry.setIpAddress(ip);
            logEntry.setUserAgent(userAgent);
            logEntry.setEventDate(LocalDateTime.now());

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save login audit log: {}", e.getMessage(), e);
        }
    }

    public void logLogout(Long personnelNumber) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUserPersonnelNumber(personnelNumber);
            logEntry.setEventType("LOGOUT");
            logEntry.setTableName("USER");
            logEntry.setRecordId(personnelNumber);
            logEntry.setEventDate(LocalDateTime.now());

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save logout audit log: {}", e.getMessage(), e);
        }
    }

    private Long getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return null;
            HttpServletRequest request = attributes.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return null;
            return attributes.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }
}