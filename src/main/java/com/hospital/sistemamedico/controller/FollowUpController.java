package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.FollowUpForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.enums.AppointmentPriority;
import com.hospital.sistemamedico.model.enums.FollowUpType;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.DoctorAvailabilityService;
import com.hospital.sistemamedico.service.FollowUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** CU-12: Cita de seguimiento, iniciada por el médico desde la consulta. */
@Controller
@RequestMapping("/admin/medico")
@RequiredArgsConstructor
public class FollowUpController {

    private static final String FORM_VIEW = "medico/seguimiento";

    private final FollowUpService followUpService;
    private final DoctorAvailabilityService doctorAvailabilityService;

    @GetMapping("/citas/{id}/seguimiento")
    public String showForm(@PathVariable Integer id,
                           @AuthenticationPrincipal AppUserPrincipal principal,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            populate(model, followUpService.loadParent(id, principal.getId()));
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/medico/panel";
        }
        model.addAttribute("followUpForm", new FollowUpForm());
        return FORM_VIEW;
    }

    @PostMapping("/citas/{id}/seguimiento")
    public String submit(@PathVariable Integer id,
                         @Valid @ModelAttribute("followUpForm") FollowUpForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Appointment parent;
        try {
            parent = followUpService.loadParent(id, principal.getId());
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/medico/panel";
        }

        if (bindingResult.hasErrors()) {
            populate(model, parent);
            return FORM_VIEW;
        }

        try {
            Appointment followUp = followUpService.schedule(id, principal.getId(), form);
            redirectAttributes.addFlashAttribute("flashMessage",
                    "Cita de seguimiento agendada exitosamente. Tipo: %s. Paciente: %s. Fecha: %s %s."
                            .formatted(form.getFollowUpType().getLabel(),
                                    parent.getPatient().getFullName(),
                                    form.getAppointmentDate(), form.getAppointmentTime()));
            return "redirect:/admin/medico/panel";
        } catch (BusinessRuleException ex) {
            // FA01: se vuelve al paso de selección de horario con el mensaje en rojo.
            populate(model, parent);
            model.addAttribute("formError", ex.getMessage());
            return FORM_VIEW;
        }
    }

    /** Horarios libres del propio médico (el seguimiento siempre es con el mismo médico). */
    @GetMapping("/api/horarios")
    @ResponseBody
    public List<LocalTime> slots(@AuthenticationPrincipal AppUserPrincipal principal,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return doctorAvailabilityService.getAvailableSlots(principal.getId(), fecha);
    }

    private void populate(Model model, Appointment parent) {
        model.addAttribute("parent", parent);
        model.addAttribute("followUpTypes", FollowUpType.values());
        model.addAttribute("priorities", AppointmentPriority.values());
        model.addAttribute("today", LocalDate.now().plusDays(1));
    }
}
