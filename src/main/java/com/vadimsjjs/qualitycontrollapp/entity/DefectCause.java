package com.vadimsjjs.qualitycontrollapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "HLP_DEFECT_CAUSE")
@Getter
@Setter
@NoArgsConstructor
public class DefectCause {

    @Id
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