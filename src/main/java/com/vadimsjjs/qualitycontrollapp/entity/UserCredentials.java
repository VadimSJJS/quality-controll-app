package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USER_CREDENTIALS")
@Getter
@Setter
@NoArgsConstructor
public class UserCredentials {

    @Id
    @Column(name = "PERSONAL_NO", nullable = false)
    private Long personalNo;

    @Column(name = "PASSWORD", length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID")
    private UserRole role;
}
