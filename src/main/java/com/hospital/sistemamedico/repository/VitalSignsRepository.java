package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.VitalSigns;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VitalSignsRepository extends JpaRepository<VitalSigns, Integer> {

    Optional<VitalSigns> findByAppointment_Id(Integer appointmentId);
}
