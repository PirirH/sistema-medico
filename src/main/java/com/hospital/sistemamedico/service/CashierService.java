package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.CashierPaymentForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.Payment;

import java.util.List;

public interface CashierService {

    List<Appointment> searchPendingAppointments(String appointmentNumber, String dpi);

    Payment registerPayment(Integer appointmentId, CashierPaymentForm form, Integer cashierUserId);
}