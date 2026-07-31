package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "HLP_STEEL_GRADE")
@Getter
@Setter
@NoArgsConstructor
public class SteelGrade {

    @Id
    @Column(name = "ID_STEEL_GRADE", nullable = false)
    private Long id;

    @Column(name = "STEEL_GRADE", nullable = false, length = 200)
    private String steelGrade;

    @Column(name = "ID_GROUP_STEEL")
    private Long groupSteelId;
}