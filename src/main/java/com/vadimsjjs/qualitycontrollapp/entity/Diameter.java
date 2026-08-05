package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "HLP_DIAMETER")
@Getter
@Setter
@NoArgsConstructor
public class Diameter {

    @Id
    @Column(name = "ID_DIAMETER", nullable = false)
    private Long id;

    @Column(name = "DIAMETER", nullable = false, length = 50)
    private String diameter;
}
