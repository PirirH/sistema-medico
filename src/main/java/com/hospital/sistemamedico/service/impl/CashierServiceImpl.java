package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.CashierPaymentForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.InternalUser;
import com.hospital.sistemamedico.model.entity.Payment;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.model.enums.PaymentMethod;
import com.hospital.sistemamedico.model.enums.PaymentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.PaymentRepository;
import com.hospital.sistemamedico.service.CashierService;
import com.hospital.sistemamedico.service.ReservationHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashierServiceImpl implements CashierService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final InternalUserRepository internalUserRepository;
    private final ReservationHoldService reservationHoldService;

    @Override
    public List<Appointment> searchPendingAppointments(String appointmentNumber, String dpi) {
        boolean hasNumber = appointmentNumber != null && !appointmentNumber.isBlank();
        boolean hasDpi = dpi != null && !dpi.isBlank();

        if (!hasNumber && !hasDpi) {
            throw new BusinessRuleException("Debe ingresar un número de cita o DPI para buscar.");
        }

        List<Appointment> results = hasNumber
                ? appointmentRepository
                .findByAppointmentNumberAndStatus(appointmentNumber.trim(), AppointmentStatus.PENDIENTE_PAGO)
                .map(List::of)
                .orElseGet(List::of)
                : appointmentRepository
                .findByPatient_DpiAndStatus(dpi.trim(), AppointmentStatus.PENDIENTE_PAGO);

        if (results.isEmpty()) {
            throw new BusinessRuleException("No se encontraron citas pendientes de pago para el criterio ingresado.");
        }

        return results;
    }

    @Override
    @Transactional
    public Payment registerPayment(Integer appointmentId, CashierPaymentForm form, Integer cashierUserId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));

        if (appointment.getStatus() != AppointmentStatus.PENDIENTE_PAGO) {
            throw new BusinessRuleException("Esta cita ya no está pendiente de pago.");
        }

        InternalUser cashier = internalUserRepository.findById(cashierUserId)
                .orElseThrow(() -> new BusinessRuleException("Usuario cajero no encontrado."));

        Payment payment = new Payment();
        payment.setTransactionNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        payment.setAppointment(appointment);
        payment.setMethod(form.getMethod());
        payment.setAmount(appointment.getTotalAmount());
        payment.setProcessedBy(cashier);
        payment.setPaidAt(LocalDateTime.now());

        if (form.getMethod() == PaymentMethod.EFECTIVO) {
            applyCashPayment(payment, appointment, form);
        } else {
            applyCardPayment(payment, form);
        }

        payment = paymentRepository.save(payment);
        payment.setTransactionNumber(generateTransactionNumber(payment.getId()));
        payment = paymentRepository.save(payment);

        if (payment.getStatus() == PaymentStatus.APROBADO) {
            appointment.setStatus(AppointmentStatus.CONFIRMADA);
            appointment.setReservationExpiresAt(null);
            appointmentRepository.save(appointment);
            reservationHoldService.confirmHold(appointmentId);
        }
        // Si fue rechazado (solo posible con tarjeta), la cita se queda en
        // PENDIENTE_PAGO y el cajero puede reintentar con otro método (FA04).

        return payment;
    }

    private void applyCashPayment(Payment payment, Appointment appointment, CashierPaymentForm form) {
        if (form.getAmountReceived() == null) {
            throw new BusinessRuleException("Debe ingresar el monto recibido.");
        }
        if (form.getAmountReceived().compareTo(appointment.getTotalAmount()) < 0) {
            throw new BusinessRuleException(String.format(
                    "El monto recibido (Q%.2f) es menor al monto a cobrar (Q%.2f)",
                    form.getAmountReceived(), appointment.getTotalAmount()));
        }
        payment.setAmountReceived(form.getAmountReceived());
        payment.setChangeGiven(form.getAmountReceived().subtract(appointment.getTotalAmount()));
        payment.setStatus(PaymentStatus.APROBADO);
    }

    private void applyCardPayment(Payment payment, CashierPaymentForm form) {
        if (form.getCardBrand() == null || form.getCardBrand().isBlank()
                || form.getCardLastFour() == null || !form.getCardLastFour().matches("\\d{4}")) {
            throw new BusinessRuleException("Debe indicar la marca de la tarjeta y sus últimos 4 dígitos.");
        }

        payment.setCardBrand(form.getCardBrand());
        payment.setCardLastFour(form.getCardLastFour());

        // Mock reproducible para pruebas: la referencia "0000" simula un rechazo del banco (FA04).
        boolean declined = form.getCardLastFour().equals("0000");
        payment.setStatus(declined ? PaymentStatus.RECHAZADO : PaymentStatus.APROBADO);
    }

    private String generateTransactionNumber(Integer id) {
        return "TXN-" + Year.now().getValue() + "-" + String.format("%06d", id);
    }
}