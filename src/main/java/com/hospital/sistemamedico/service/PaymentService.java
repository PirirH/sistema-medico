package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.PaymentForm;
import com.hospital.sistemamedico.model.entity.Payment;

public interface PaymentService {

    Payment processCardPayment(Integer appointmentId, PaymentForm form);
}