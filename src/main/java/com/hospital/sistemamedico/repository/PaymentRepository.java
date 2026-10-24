package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByTransactionNumber(String transactionNumber);

    List<Payment> findByAppointmentIdOrderByCreatedAtDesc(Integer appointmentId);
}