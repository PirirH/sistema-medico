package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.LabOrderForm;
import com.hospital.sistemamedico.model.dto.PrescriptionForm;
import com.hospital.sistemamedico.model.dto.PrescriptionItemForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.entity.LabOrderItem;
import com.hospital.sistemamedico.model.entity.Prescription;
import com.hospital.sistemamedico.model.entity.PrescriptionItem;
import com.hospital.sistemamedico.repository.LabExamRepository;
import com.hospital.sistemamedico.repository.MedicineRepository;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.ClinicalOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

/** CU-08 FA01 (orden de laboratorio) y FA04 (receta médica) + consulta de resultados. */
@Controller
@RequestMapping("/admin/medico/citas/{id}")
@RequiredArgsConstructor
public class ClinicalOrderController {

    private final ClinicalOrderService clinicalOrderService;
    private final LabExamRepository labExamRepository;
    private final MedicineRepository medicineRepository;

    // ------------------------------------------------------------------ Orden de laboratorio

    @GetMapping("/orden-lab")
    public String showLabOrderForm(@PathVariable Integer id,
                                   @AuthenticationPrincipal AppUserPrincipal principal,
                                   Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("appointment", clinicalOrderService.loadForOrders(id, principal.getId()));
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/medico/panel";
        }
        model.addAttribute("exams", labExamRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("labOrderForm", new LabOrderForm());
        return "medico/orden-lab";
    }

    @PostMapping("/orden-lab")
    public String createLabOrder(@PathVariable Integer id,
                                 @ModelAttribute("labOrderForm") LabOrderForm form,
                                 @AuthenticationPrincipal AppUserPrincipal principal,
                                 Model model, RedirectAttributes ra) {
        try {
            LabOrder order = clinicalOrderService.createLabOrder(id, principal.getId(), form);
            String exams = order.getItems().stream()
                    .map(LabOrderItem::getExam).map(e -> e.getName()).collect(Collectors.joining(", "));
            String message = "Orden de laboratorio generada exitosamente. Número de orden: %s. Exámenes: %s. "
                    .formatted(order.getOrderNumber(), exams)
                    + (order.isExternal()
                    ? "Es una orden externa: el paciente realizará los exámenes en otro laboratorio."
                    : "El paciente debe dirigirse al área de laboratorio.");
            ra.addFlashAttribute("flashMessage", message);
            return "redirect:/admin/medico/citas/" + id + "/consulta";
        } catch (BusinessRuleException ex) {
            try {
                model.addAttribute("appointment", clinicalOrderService.loadAppointment(id, principal.getId()));
            } catch (BusinessRuleException inner) {
                ra.addFlashAttribute("flashError", inner.getMessage());
                return "redirect:/admin/medico/panel";
            }
            model.addAttribute("exams", labExamRepository.findByActiveTrueOrderByNameAsc());
            model.addAttribute("formError", ex.getMessage());
            return "medico/orden-lab";
        }
    }

    // ------------------------------------------------------------------ Receta médica

    @GetMapping("/receta")
    public String showPrescriptionForm(@PathVariable Integer id,
                                       @AuthenticationPrincipal AppUserPrincipal principal,
                                       Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("appointment", clinicalOrderService.loadForOrders(id, principal.getId()));
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/medico/panel";
        }
        PrescriptionForm form = new PrescriptionForm();
        form.getItems().add(new PrescriptionItemForm());
        model.addAttribute("prescriptionForm", form);
        model.addAttribute("medicines", medicineRepository.findByActiveTrueOrderByNameAsc());
        return "medico/receta";
    }

    @PostMapping("/receta")
    public String createPrescription(@PathVariable Integer id,
                                     @ModelAttribute("prescriptionForm") PrescriptionForm form,
                                     @AuthenticationPrincipal AppUserPrincipal principal,
                                     Model model, RedirectAttributes ra) {
        try {
            Prescription prescription = clinicalOrderService.createPrescription(id, principal.getId(), form);
            String medicines = prescription.getItems().stream()
                    .map(PrescriptionItem::getMedicine).map(m -> m.getName()).collect(Collectors.joining(", "));
            ra.addFlashAttribute("flashMessage",
                    "Receta médica generada exitosamente. Número de receta: %s. Medicamentos: %s. El paciente puede adquirirlos en la farmacia de la clínica."
                            .formatted(prescription.getPrescriptionNumber(), medicines));
            return "redirect:/admin/medico/citas/" + id + "/consulta";
        } catch (BusinessRuleException ex) {
            try {
                model.addAttribute("appointment", clinicalOrderService.loadAppointment(id, principal.getId()));
            } catch (BusinessRuleException inner) {
                ra.addFlashAttribute("flashError", inner.getMessage());
                return "redirect:/admin/medico/panel";
            }
            if (form.getItems().isEmpty()) {
                form.getItems().add(new PrescriptionItemForm());
            }
            model.addAttribute("medicines", medicineRepository.findByActiveTrueOrderByNameAsc());
            model.addAttribute("formError", ex.getMessage());
            return "medico/receta";
        }
    }

    // ------------------------------------------------------------------ Resultados de laboratorio

    @GetMapping("/ordenes")
    public String labResults(@PathVariable Integer id,
                             @AuthenticationPrincipal AppUserPrincipal principal,
                             Model model, RedirectAttributes ra) {
        try {
            Appointment appointment = clinicalOrderService.loadAppointment(id, principal.getId());
            model.addAttribute("appointment", appointment);
            model.addAttribute("orders", clinicalOrderService.labOrdersOfPatient(id, principal.getId()));
            return "medico/resultados";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/medico/panel";
        }
    }
}
