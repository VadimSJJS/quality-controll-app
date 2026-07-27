package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonalRepository extends JpaRepository<Personal, Long> {

    @Query(value = "SELECT * FROM V_PERSONAL_STPC2 WHERE PERSONAL_NO = :personalNo", nativeQuery = true)
    Optional<Personal> findByPersonalNo(@Param("personalNo") Long personalNo);
}