package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.Cie10Option;
import com.hospital.sistemamedico.model.dto.ConsultationDetail;
import com.hospital.sistemamedico.model.dto.ConsultationForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.Consultation;
import com.hospital.sistemamedico.model.enums.ConsultationStatus;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** CU-08: Consulta médica (panel del médico). */
@Controller
@RequestMapping("/admin/medico")
@RequiredArgsConstructor
public class ConsultationController {

    private static final String PANEL_VIEW = "medico/panel";
    private static final String FORM_VIEW = "medico/consulta";
    private static final String PANEL_REDIRECT = "redirect:/admin/medico/panel";

    private final ConsultationService consultationService;

    @GetMapping("/panel")
    public String panel(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("panel", consultationService.getPanel(principal.getId()));
        return PANEL_VIEW;
    }

    /** Paso 2: "Iniciar Consulta" + anuncio por voz. */
    @PostMapping("/citas/{id}/iniciar")
    public String start(@PathVariable Integer id,
                        @AuthenticationPrincipal AppUserPrincipal principal,
                        RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = consultationService.startConsultation(id, principal.getId());
            String name = appointment.getPatient().getFullName();
            redirectAttributes.addFlashAttribute("announcement",
                    "Turno número %d. Paciente %s, favor pasar a consulta médica."
                            .formatted(appointment.getId(), name));
            redirectAttributes.addFlashAttribute("flashMessage",
                    "Consulta iniciada para el paciente %s.".formatted(name));
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return PANEL_REDIRECT;
    }

    /** FA06: paciente no asistió. */
    @PostMapping("/citas/{id}/no-asistio")
    public String noShow(@PathVariable Integer id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = consultationService.markNoShow(id, principal.getId());
            redirectAttributes.addFlashAttribute("flashMessage",
                    "Cita #%s marcada como No Asistió.".formatted(appointment.getAppointmentNumber()));
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return PANEL_REDIRECT;
    }

    /** Paso 3: "Ver / Completar Consulta". */
    @GetMapping("/citas/{id}/consulta")
    public String showConsultation(@PathVariable Integer id,
                                   @AuthenticationPrincipal AppUserPrincipal principal,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            ConsultationDetail detail = consultationService.load(id, principal.getId());
            fillModel(model, detail, formFrom(detail.getConsultation()));
            return FORM_VIEW;
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return PANEL_REDIRECT;
        }
    }

    /** Pasos 4-10: "Guardar Consulta". */
    @PostMapping("/citas/{id}/consulta")
    public String saveConsultation(@PathVariable Integer id,
                                   @Valid @ModelAttribute("consultationForm") ConsultationForm form,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal AppUserPrincipal principal,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        ConsultationDetail detail;
        try {
            detail = consultationService.load(id, principal.getId());
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return PANEL_REDIRECT;
        }

        if (bindingResult.hasErrors()) {
            fillModel(model, detail, null);
            return FORM_VIEW;
        }

        try {
            consultationService.save(id, principal.getId(), form);
        } catch (BusinessRuleException ex) {
            model.addAttribute("formError", ex.getMessage());
            fillModel(model, detail, null);
            return FORM_VIEW;
        }

        if (form.getStatus() == ConsultationStatus.FINALIZADA) {
            redirectAttributes.addFlashAttribute("flashMessage",
                    "La consulta ha sido finalizada exitosamente. El paciente puede proceder a las siguientes indicaciones médicas.");
            return PANEL_REDIRECT;
        }
        redirectAttributes.addFlashAttribute("flashMessage", "Consulta guardada. Continúa en curso.");
        return "redirect:/admin/medico/citas/" + id + "/consulta";
    }

    /** Paso 11: "Finalizar Atención". */
    @PostMapping("/citas/{id}/finalizar")
    public String finalizeAttention(@PathVariable Integer id,
                                    @AuthenticationPrincipal AppUserPrincipal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = consultationService.finalizeAttention(id, principal.getId());
            redirectAttributes.addFlashAttribute("flashMessage",
                    "Atención finalizada para cita #%s.".formatted(appointment.getAppointmentNumber()));
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return PANEL_REDIRECT;
    }

    /** Autocompletado CIE-10 (RNF-004). */
    @GetMapping("/api/cie10")
    @ResponseBody
    public List<Cie10Option> cie10(@RequestParam("q") String query) {
        return consultationService.searchCie10(query);
    }

    // ------------------------------------------------------------------ auxiliares

    private void fillModel(Model model, ConsultationDetail detail, ConsultationForm formIfAbsent) {
        model.addAttribute("appointment", detail.getAppointment());
        model.addAttribute("consultation", detail.getConsultation());
        model.addAttribute("vitalSigns", detail.getVitalSigns());
        model.addAttribute("readOnly", detail.getConsultation().getStatus() == ConsultationStatus.FINALIZADA);
        if (formIfAbsent != null) {
            model.addAttribute("consultationForm", formIfAbsent);
        }
    }

    private ConsultationForm formFrom(Consultation c) {
        ConsultationForm form = new ConsultationForm();
        form.setVisitReason(c.getVisitReason());
        form.setClinicalFindings(c.getClinicalFindings());
        form.setCie10Code(c.getCie10Code());
        form.setDiagnosis(c.getDiagnosis());
        form.setTreatmentPlan(c.getTreatmentPlan());
        form.setNotes(c.getNotes());
        form.setStatus(c.getStatus());
        return form;
    }
}
