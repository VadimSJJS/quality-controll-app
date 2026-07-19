package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOG")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "SEQ_AUDIT_LOG", allocationSize = 1)
    @Column(name = "ID_AUDIT_LOG", nullable = false)
    private Long id;

    @Column(name = "USER_PERSONNEL_NUMBER", nullable = false)
    private Long userPersonnelNumber;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType;

    @Column(name = "TABLE_NAME", length = 100)
    private String tableName;

    @Column(name = "RECORD_ID")
    private Long recordId;

    @Lob
    @Column(name = "OLD_VALUE")
    private String oldValue;

    @Lob
    @Column(name = "NEW_VALUE")
    private String newValue;

    @Column(name = "IP_ADDRESS", length = 45)
    private String ipAddress;

    @Column(name = "USER_AGENT", length = 255)
    private String userAgent;

    @Column(name = "EVENT_DATE")
    private LocalDateTime eventDate;
}
