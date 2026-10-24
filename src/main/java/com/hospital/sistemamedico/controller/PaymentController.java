package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.PaymentForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.Payment;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.model.enums.PaymentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.PaymentService;
import com.hospital.sistemamedico.repository.PaymentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
@RequestMapping("/paciente/citas")
@RequiredArgsConstructor
public class PaymentController {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @GetMapping("/{id}/pagar")
    public String showPaymentPage(@PathVariable Integer id,
                                  @AuthenticationPrincipal AppUserPrincipal principal,
                                  Model model) {

        Appointment appointment = loadOwnedAppointment(id, principal);

        if (appointment.getStatus() != AppointmentStatus.PENDIENTE_PAGO) {
            return "redirect:/paciente/citas/" + id + "/confirmacion";
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("onlinePaymentDeadlineMillis", toEpochMillis(appointment.getCreatedAt().plusMinutes(5)));
        model.addAttribute("paymentForm", new PaymentForm());
        return "paciente/citas-pagar";
    }

    @PostMapping("/{id}/pagar")
    public String submitPayment(@PathVariable Integer id,
                                @Valid @ModelAttribute("paymentForm") PaymentForm form,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal AppUserPrincipal principal,
                                Model model) {

        Appointment appointment = loadOwnedAppointment(id, principal);

        if (bindingResult.hasErrors()) {
            model.addAttribute("appointment", appointment);
            model.addAttribute("onlinePaymentDeadlineMillis", toEpochMillis(appointment.getCreatedAt().plusMinutes(5)));
            return "paciente/citas-pagar";
        }

        try {
            Payment payment = paymentService.processCardPayment(id, form);

            if (payment.getStatus() == PaymentStatus.APROBADO) {
                return "redirect:/paciente/citas/" + id + "/confirmacion";
            }

            model.addAttribute("appointment", appointment);
            model.addAttribute("onlinePaymentDeadlineMillis", toEpochMillis(appointment.getCreatedAt().plusMinutes(5)));
            model.addAttribute("rejectionMessage", payment.getRejectionReason().getMessage());
            return "paciente/citas-pagar";

        } catch (BusinessRuleException ex) {
            model.addAttribute("appointment", appointment);
            model.addAttribute("onlinePaymentDeadlineMillis", toEpochMillis(appointment.getCreatedAt().plusMinutes(5)));
            model.addAttribute("paymentError", ex.getMessage());
            return "paciente/citas-pagar";
        }
    }

    @GetMapping("/{id}/confirmacion")
    public String showConfirmation(@PathVariable Integer id,
                                   @AuthenticationPrincipal AppUserPrincipal principal,
                                   Model model) {
        Appointment appointment = loadOwnedAppointment(id, principal);

        Payment payment = paymentRepository.findByAppointmentIdOrderByCreatedAtDesc(id)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.APROBADO)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No se encontró un comprobante de pago para esta cita."));

        model.addAttribute("appointment", appointment);
        model.addAttribute("payment", payment);
        return "paciente/citas-confirmacion";
    }

    private Appointment loadOwnedAppointment(Integer id, AppUserPrincipal principal) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));

        if (!appointment.getPatient().getId().equals(principal.getId())) {
            throw new BusinessRuleException("No tiene permiso para acceder a esta cita.");
        }

        return appointment;
    }

    private long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}