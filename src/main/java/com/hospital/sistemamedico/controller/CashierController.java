package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.CashierPaymentForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.Payment;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.model.enums.PaymentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.PaymentRepository;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.CashierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/caja")
@RequiredArgsConstructor
public class CashierController {

    private static final String CARD_REJECTED_MESSAGE =
            "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.";

    private final CashierService cashierService;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/buscar")
    public String showSearchForm() {
        return "admin/caja-buscar";
    }

    @PostMapping("/buscar")
    public String search(@RequestParam(required = false) String numero,
                         @RequestParam(required = false) String dpi,
                         Model model) {
        model.addAttribute("numero", numero);
        model.addAttribute("dpi", dpi);
        try {
            List<Appointment> results = cashierService.searchPendingAppointments(numero, dpi);
            model.addAttribute("results", results);
        } catch (BusinessRuleException ex) {
            model.addAttribute("searchError", ex.getMessage());
        }
        return "admin/caja-buscar";
    }

    @GetMapping("/{id}/cobrar")
    public String showChargeForm(@PathVariable Integer id, Model model) {
        Appointment appointment = loadAppointment(id);

        if (appointment.getStatus() != AppointmentStatus.PENDIENTE_PAGO) {
            throw new BusinessRuleException("Esta cita ya no está pendiente de pago.");
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("cashierForm", new CashierPaymentForm());
        return "admin/caja-cobrar";
    }

    @PostMapping("/{id}/cobrar")
    public String submitCharge(@PathVariable Integer id,
                               @Valid @ModelAttribute("cashierForm") CashierPaymentForm form,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal AppUserPrincipal principal,
                               Model model) {

        Appointment appointment = loadAppointment(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("appointment", appointment);
            return "admin/caja-cobrar";
        }

        try {
            Payment payment = cashierService.registerPayment(id, form, principal.getId());

            if (payment.getStatus() == PaymentStatus.APROBADO) {
                return "redirect:/admin/caja/" + id + "/recibo";
            }

            model.addAttribute("appointment", appointment);
            model.addAttribute("rejectionMessage", CARD_REJECTED_MESSAGE);
            return "admin/caja-cobrar";

        } catch (BusinessRuleException ex) {
            model.addAttribute("appointment", appointment);
            model.addAttribute("chargeError", ex.getMessage());
            return "admin/caja-cobrar";
        }
    }

    @GetMapping("/{id}/recibo")
    public String showReceipt(@PathVariable Integer id, Model model) {
        Appointment appointment = loadAppointment(id);

        Payment payment = paymentRepository.findByAppointmentIdOrderByCreatedAtDesc(id)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.APROBADO)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No se encontró un comprobante de pago para esta cita."));

        model.addAttribute("appointment", appointment);
        model.addAttribute("payment", payment);
        return "admin/caja-recibo";
    }

    private Appointment loadAppointment(Integer id) {
        return appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));
    }
}
