package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "V_PERSONAL_STPC2")
@Getter
@Setter
@NoArgsConstructor
public class Personal {

    private static final String DEFAULT_ROLE = "VIEWER";

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "PERSONAL_NO")
    private Long personalNo;

    @Column(name = "FIO")
    private String fio;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "DIV_NAME")
    private String divName;

    @Column(name = "DIV_NO")
    private Long divNo;

    @Column(name = "CEH_NO")
    private Long cehNo;

    @Column(name = "CEH_NAME")
    private String cehName;

    @Column(name = "CEH_EXTERNAL_CODE")
    private String cehExternalCode;

    @Column(name = "PERSONAL_CODE")
    private String personalCode;

    @Column(name = "SHORT_FIO")
    private String shortFio;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @Column(name = "PRIZN_END_DATE")
    private Long priznEndDate;

    @Column(name = "CATEGORY_NO")
    private Long categoryNo;

    @Column(name = "CATEGORY_NAME")
    private String categoryName;

    @Column(name = "PROFESSION_NO")
    private Long professionNo;

    @Column(name = "PROFESSION_NAME")
    private String professionName;

    @Column(name = "SHORT_PROFESSION")
    private String shortProfession;

    @Column(name = "ROLE_NAME")
    private String roleName;

    public boolean isActive() {
        return endDate == null || !endDate.isBefore(LocalDate.now());
    }

    public String resolveRoleName() {
        if (roleName == null || roleName.isBlank()) {
            return DEFAULT_ROLE;
        }
        return roleName;
    }

    public String resolvePassword() {
        return password != null ? password : "";
    }
}
