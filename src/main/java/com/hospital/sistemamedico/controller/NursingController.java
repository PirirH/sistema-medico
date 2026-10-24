package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.VitalSignsForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.VitalSigns;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.NursingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CU-07: Toma de signos vitales (personal de enfermería). */
@Controller
@RequestMapping("/admin/enfermeria")
@RequiredArgsConstructor
public class NursingController {

    private static final String PANEL_VIEW = "enfermeria/panel";
    private static final String FORM_VIEW = "enfermeria/signos";
    private static final String PANEL_REDIRECT = "redirect:/admin/enfermeria/panel";

    private final NursingService nursingService;

    @GetMapping("/panel")
    public String panel(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("panel", nursingService.getPanel(principal.getId()));
        return PANEL_VIEW;
    }

    /** Paso 1: "Llamar y Tomar Signos" + anuncio por voz (TTS en el navegador). */
    @PostMapping("/citas/{id}/llamar")
    public String call(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = nursingService.callPatient(id);
            String name = appointment.getPatient().getFullName();
            redirectAttributes.addFlashAttribute("announcement",
                    "Turno número %d. Paciente %s, favor pasar a toma de signos vitales."
                            .formatted(appointment.getId(), name));
            redirectAttributes.addFlashAttribute("flashMessage",
                    "Paciente %s llamado a toma de signos vitales.".formatted(name));
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return PANEL_REDIRECT;
    }

    /** Paso 2: abre el formulario con el contexto de la cita precargado. */
    @GetMapping("/citas/{id}/signos")
    public String showForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("appointment", nursingService.loadForVitalSigns(id));
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return PANEL_REDIRECT;
        }
        model.addAttribute("vitalSignsForm", new VitalSignsForm());
        return FORM_VIEW;
    }

    @PostMapping("/citas/{id}/signos")
    public String submit(@PathVariable Integer id,
                         @Valid @ModelAttribute("vitalSignsForm") VitalSignsForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        Appointment appointment;
        try {
            appointment = nursingService.loadForVitalSigns(id);
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return PANEL_REDIRECT;
        }
        model.addAttribute("appointment", appointment);

        if (bindingResult.hasErrors()) {
            return FORM_VIEW; // FA02: mensaje específico de cada campo
        }

        try {
            VitalSigns saved = nursingService.register(id, principal.getId(), form);
            String name = appointment.getPatient().getFullName();

            String message = form.isEmergency()
                    ? "Signos vitales de emergencia registrados para paciente %s. El paciente debe pasar directamente a consulta médica."
                    .formatted(name)
                    : "Signos vitales del paciente %s registrados correctamente. El paciente puede regresar a la sala de espera."
                    .formatted(name);
            redirectAttributes.addFlashAttribute("flashMessage", message);
            if (saved.getAlerts() != null) {
                redirectAttributes.addFlashAttribute("flashWarning",
                        "Alertas clínicas registradas para %s: %s".formatted(name, saved.getAlerts()));
            }
            return PANEL_REDIRECT;

        } catch (BusinessRuleException ex) {
            model.addAttribute("formError", ex.getMessage());
            return FORM_VIEW;
        }
    }
}
