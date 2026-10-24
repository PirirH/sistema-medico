package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.LabResultForm;
import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.enums.LabOrderStatus;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** CU-09: Gestión de laboratorio. */
@Controller
@RequestMapping("/admin/laboratorio")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    @GetMapping("/ordenes")
    public String list(@RequestParam(required = false) LabOrderStatus estado,
                       @RequestParam(required = false) String paciente,
                       @RequestParam(required = false) String medico,
                       Model model) {
        model.addAttribute("orders", labService.search(estado, paciente, medico));
        model.addAttribute("estado", estado);
        model.addAttribute("paciente", paciente);
        model.addAttribute("medico", medico);
        model.addAttribute("statuses", LabOrderStatus.values());
        return "laboratorio/ordenes";
    }

    @GetMapping("/ordenes/{id}")
    public String detail(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            LabOrder order = labService.getOrder(id);
            model.addAttribute("order", order);
            model.addAttribute("today", LocalDate.now());
            return "laboratorio/orden";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/laboratorio/ordenes";
        }
    }

    @PostMapping("/ordenes/{id}/items/{itemId}/resultado")
    public String saveResult(@PathVariable Integer id, @PathVariable Integer itemId,
                             @ModelAttribute LabResultForm form,
                             @AuthenticationPrincipal AppUserPrincipal principal,
                             RedirectAttributes ra) {
        try {
            labService.saveResult(id, itemId, form, principal.getId());
            ra.addFlashAttribute("flashMessage", "Resultado guardado exitosamente.");
            if (form.isOutOfRange()) {
                ra.addFlashAttribute("flashWarning",
                        "Los resultados están fuera del rango de referencia normal. Requiere revisión.");
            }
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/laboratorio/ordenes/" + id;
    }

    @PostMapping("/ordenes/{id}/items/{itemId}/publicar")
    public String publish(@PathVariable Integer id, @PathVariable Integer itemId,
                          @AuthenticationPrincipal AppUserPrincipal principal,
                          RedirectAttributes ra) {
        try {
            LabOrder order = labService.publish(id, itemId, principal.getId());
            ra.addFlashAttribute("flashMessage", "Resultado publicado exitosamente.");
            if (order.getStatus() == LabOrderStatus.COMPLETADA) {
                ra.addFlashAttribute("flashWarning", "Todos los exámenes fueron publicados: la orden está Completada.");
            }
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/laboratorio/ordenes/" + id;
    }
}
