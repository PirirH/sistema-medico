package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.PaymentForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.Payment;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.model.enums.PaymentMethod;
import com.hospital.sistemamedico.model.enums.PaymentStatus;
import com.hospital.sistemamedico.model.enums.RejectionReason;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.PaymentRepository;
import com.hospital.sistemamedico.model.dto.ReceiptEmailData;
import com.hospital.sistemamedico.service.EmailService;
import com.hospital.sistemamedico.service.PaymentService;
import com.hospital.sistemamedico.service.ReservationHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationHoldService reservationHoldService;
    private final EmailService emailService;

    @Override
    @Transactional
    public Payment processCardPayment(Integer appointmentId, PaymentForm form) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));

        if (appointment.getStatus() != AppointmentStatus.PENDIENTE_PAGO) {
            throw new BusinessRuleException("Esta cita ya no está pendiente de pago.");
        }

        if (appointment.getReservationExpiresAt() == null
                || appointment.getReservationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException(RejectionReason.SESION_EXPIRADA.getMessage());
        }

        LocalDateTime onlinePaymentDeadline = appointment.getCreatedAt().plusMinutes(5);
        if (LocalDateTime.now().isAfter(onlinePaymentDeadline)){
            throw new BusinessRuleException(
                    "El tiempo para pagar en línea ha finalizado. Puede acercarse a caja para completar el pago antes de que su reserva expire.");
        }

        String cleanCardNumber = form.getCardNumber().replaceAll("[\\s-]", "");
        Optional<RejectionReason> rejection = simulateGateway(cleanCardNumber);

        Payment payment = new Payment();
        payment.setTransactionNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        payment.setAppointment(appointment);
        payment.setMethod(PaymentMethod.TARJETA_CREDITO);
        payment.setAmount(appointment.getTotalAmount());
        payment.setCardHolderName(form.getCardHolderName());
        payment.setCardLastFour(cleanCardNumber.substring(cleanCardNumber.length() - 4));
        payment.setCardBrand(detectCardBrand(cleanCardNumber));
        payment.setPaidAt(LocalDateTime.now());

        if (rejection.isPresent()) {
            payment.setStatus(PaymentStatus.RECHAZADO);
            payment.setRejectionReason(rejection.get());
        } else {
            payment.setStatus(PaymentStatus.APROBADO);
        }

        payment = paymentRepository.save(payment);
        payment.setTransactionNumber(generateTransactionNumber(payment.getId()));
        payment = paymentRepository.save(payment);

        if (payment.getStatus() == PaymentStatus.APROBADO) {
            appointment.setStatus(AppointmentStatus.CONFIRMADA);
            appointment.setReservationExpiresAt(null);
            appointmentRepository.save(appointment);
            reservationHoldService.confirmHold(appointmentId);
           // rechazo de email no existente
            emailService.sendPaymentReceipt(ReceiptEmailData.builder()
                    .to(appointment.getPatient().getEmail())
                    .patientName(appointment.getPatient().getFullName())
                    .appointmentNumber(appointment.getAppointmentNumber())
                    .transactionNumber(payment.getTransactionNumber())
                    .doctorName(appointment.getDoctor().getFullName())
                    .specialty(appointment.getBranchSpecialty().getSpecialty().getName())
                    .branch(appointment.getBranchSpecialty().getBranch().getName())
                    .scheduledAt(appointment.getScheduledAt())
                    .amount(payment.getAmount())
                    .build());
        }
        // Si fue rechazado, la cita se queda en PENDIENTE_PAGO: el paciente puede
        // reintentar con otra tarjeta mientras siga dentro de la ventana de pago en
        // línea (5 min) y antes de que la reserva completa expire (10 min).

        return payment;
    }

    private Optional<RejectionReason> simulateGateway(String cardNumber) {
        if (cardNumber.endsWith("0001")) return Optional.of(RejectionReason.FONDOS_INSUFICIENTES);
        if (cardNumber.endsWith("0002")) return Optional.of(RejectionReason.TARJETA_INVALIDA);
        if (cardNumber.endsWith("0003")) return Optional.of(RejectionReason.TARJETA_VENCIDA);
        if (cardNumber.endsWith("0004")) return Optional.of(RejectionReason.ERROR_COMUNICACION);
        return Optional.empty();
    }

    private String detectCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) return "Visa";
        if (cardNumber.startsWith("5")) return "Mastercard";
        return "Otra";
    }

    private String generateTransactionNumber(Integer id) {
        return "TXN-" + Year.now().getValue() + "-" + String.format("%06d", id);
    }
}