package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.CashierPaymentForm;
import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.entity.LabPayment;
import com.hospital.sistemamedico.model.enums.PaymentStatus;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.LabCashierService;
import com.hospital.sistemamedico.service.LabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** CU-10: Cobro de laboratorio en caja ("Cobro Lab"). */
@Controller
@RequestMapping("/admin/caja/laboratorio")
@RequiredArgsConstructor
public class LabCashierController {

    private static final String CARD_REJECTED_MESSAGE =
            "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.";

    private final LabCashierService labCashierService;
    private final LabService labService;

    @GetMapping("/buscar")
    public String showSearch() {
        return "admin/caja-lab-buscar";
    }

    @PostMapping("/buscar")
    public String search(@RequestParam(required = false) String dpi,
                         @RequestParam(required = false) String orden,
                         Model model) {
        model.addAttribute("dpi", dpi);
        model.addAttribute("orden", orden);
        try {
            List<LabOrder> results = labCashierService.searchPending(dpi, orden);
            model.addAttribute("results", results);
        } catch (BusinessRuleException ex) {
            model.addAttribute("searchError", ex.getMessage());
        }
        return "admin/caja-lab-buscar";
    }

    @GetMapping("/{id}/cobrar")
    public String showChargeForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("order", labCashierService.getPendingOrder(id));
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/caja/laboratorio/buscar";
        }
        model.addAttribute("cashierForm", new CashierPaymentForm());
        return "admin/caja-lab-cobrar";
    }

    @PostMapping("/{id}/cobrar")
    public String charge(@PathVariable Integer id,
                         @Valid @ModelAttribute("cashierForm") CashierPaymentForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes ra) {
        LabOrder order;
        try {
            order = labCashierService.getPendingOrder(id);
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/caja/laboratorio/buscar";
        }
        model.addAttribute("order", order);

        if (bindingResult.hasErrors()) {
            return "admin/caja-lab-cobrar";
        }
        try {
            LabPayment payment = labCashierService.charge(id, form, principal.getId());
            if (payment.getStatus() == PaymentStatus.APROBADO) {
                ra.addFlashAttribute("flashMessage",
                        "¡Pago de laboratorio registrado exitosamente! Paciente: %s. La orden ha sido actualizada a estado 'En proceso'."
                                .formatted(order.getPatient().getFullName()));
                return "redirect:/admin/caja/laboratorio/" + id + "/recibo";
            }
            model.addAttribute("rejectionMessage", CARD_REJECTED_MESSAGE); // FA04
            return "admin/caja-lab-cobrar";
        } catch (BusinessRuleException ex) {
            model.addAttribute("chargeError", ex.getMessage());
            return "admin/caja-lab-cobrar";
        }
    }

    @GetMapping("/{id}/recibo")
    public String receipt(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("order", labService.getOrder(id));
            model.addAttribute("payment", labCashierService.getApprovedPayment(id));
            return "admin/caja-lab-recibo";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/caja/laboratorio/buscar";
        }
    }
}
