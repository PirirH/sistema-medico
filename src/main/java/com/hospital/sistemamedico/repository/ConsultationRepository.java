package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Integer> {

    Optional<Consultation> findByAppointment_Id(Integer appointmentId);

    List<Consultation> findByAppointment_IdIn(Collection<Integer> appointmentIds);
}
