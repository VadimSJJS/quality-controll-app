package com.vadimsjjs.qualitycontrollapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Справочник причин несоответствия. Иерархический: подпричина ссылается на причину
 * через ID_PARENT_CAUSE (в сущности — поле parentCause).
 */
@Entity
@Table(name = "HLP_DEFECT_CAUSE")
@Getter
@Setter
@NoArgsConstructor
public class DefectCause {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hlp_defect_cause_seq")
    @SequenceGenerator(name = "hlp_defect_cause_seq", sequenceName = "SEQ_HLP_DEFECT_CAUSE", allocationSize = 1)
    @Column(name = "ID_DEFECT_CAUSE", nullable = false)
    private Long id;

    @Column(name = "CAUSE_CODE", nullable = false, length = 20)
    private String causeCode;

    @Column(name = "CAUSE_NAME", nullable = false, length = 100)
    private String causeName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PARENT_CAUSE")
    private DefectCause parentCause;
}