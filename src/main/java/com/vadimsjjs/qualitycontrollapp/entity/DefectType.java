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
@Table(name = "HLP_DEFECT_TYPE")
@Getter
@Setter
@NoArgsConstructor
public class DefectType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hlp_defect_type_seq")
    @SequenceGenerator(name = "hlp_defect_type_seq", sequenceName = "SEQ_HLP_DEFECT_TYPE", allocationSize = 1)
    @Column(name = "ID_DEFECT_TYPE", nullable = false)
    private Long id;

    @Column(name = "DEFECT_CODE", nullable = false, length = 20)
    private String defectCode;

    @Column(name = "DEFECT_NAME", nullable = false, length = 100)
    private String defectName;

    @Column(name = "IS_REWORKABLE")
    private Boolean reworkable;
}
