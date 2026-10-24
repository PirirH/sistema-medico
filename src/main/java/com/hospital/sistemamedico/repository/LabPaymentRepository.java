package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.LabPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabPaymentRepository extends JpaRepository<LabPayment, Integer> {

    List<LabPayment> findByLabOrder_IdOrderByCreatedAtDesc(Integer labOrderId);
}
