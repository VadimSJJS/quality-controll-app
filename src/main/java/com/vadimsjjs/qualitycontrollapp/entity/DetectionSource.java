package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "HLP_DETECTION_SOURCE")
@Getter
@Setter
@NoArgsConstructor
public class DetectionSource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hlp_detection_source_seq")
    @SequenceGenerator(name = "hlp_detection_source_seq", sequenceName = "SEQ_HLP_DETECTION_SOURCE", allocationSize = 1)
    @Column(name = "ID_DETECTION_SOURCE", nullable = false)
    private Long id;

    @Column(name = "SOURCE_CODE", nullable = false, length = 20)
    private String sourceCode;

    @Column(name = "SOURCE_NAME", nullable = false, length = 100)
    private String sourceName;
}
