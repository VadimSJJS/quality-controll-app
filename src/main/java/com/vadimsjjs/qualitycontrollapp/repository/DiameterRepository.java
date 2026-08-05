package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.Diameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiameterRepository extends JpaRepository<Diameter, Long> {

    List<Diameter> findAllByOrderByDiameter();
}
