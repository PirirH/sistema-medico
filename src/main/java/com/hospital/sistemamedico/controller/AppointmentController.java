package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.AppointmentRequestForm;
import com.hospital.sistemamedico.model.dto.SelectOptionDTO;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.repository.BranchRepository;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.SpecialtyRepository;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.AppointmentSchedulingService;
import com.hospital.sistemamedico.service.DoctorAvailabilityService;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/paciente/citas")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentSchedulingService appointmentSchedulingService;
    private final DoctorAvailabilityService doctorAvailabilityService;
    private final BranchRepository branchRepository;
    private final SpecialtyRepository specialtyRepository;
    private final InternalUserRepository internalUserRepository;
    private final AppointmentRepository appointmentRepository;

    @GetMapping("/nueva")
    public String showWizard(Model model) {
        populateCatalogs(model);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("appointmentForm", new AppointmentRequestForm());
        return "paciente/citas-nueva";
    }

    @PostMapping("/nueva")
    public String submitWizard(@Valid @ModelAttribute("appointmentForm") AppointmentRequestForm form,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal AppUserPrincipal principal,
                               Model model) {

        if (bindingResult.hasErrors()) {
            populateCatalogs(model);
            return "paciente/citas-nueva";
        }

        try {
            Appointment appointment = appointmentSchedulingService.scheduleFromPortal(principal.getId(), form);
            return "redirect:/paciente/citas/" + appointment.getId() + "/pagar";
        } catch (BusinessRuleException ex) {
            model.addAttribute("wizardError", ex.getMessage());
            populateCatalogs(model);
            return "paciente/citas-nueva";
        }
    }

    @GetMapping("/api/medicos")
    @ResponseBody
    public List<SelectOptionDTO> getMedicos(@RequestParam Integer branchId, @RequestParam Integer specialtyId) {
        return internalUserRepository
                .findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue("Medico", branchId, specialtyId)
                .stream()
                .map(doctor -> new SelectOptionDTO(doctor.getId(), "Dr(a). " + doctor.getFullName()))
                .toList();
    }

    @GetMapping("/api/horarios")
    @ResponseBody
    public List<LocalTime> getHorarios(@RequestParam Integer doctorId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return doctorAvailabilityService.getAvailableSlots(doctorId, fecha);
    }

    private void populateCatalogs(Model model) {
        model.addAttribute("branches", branchRepository.findByActiveTrue());
        model.addAttribute("specialties", specialtyRepository.findByActiveTrue());
    }

    @GetMapping
    public String listMyAppointments(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("appointments",
                appointmentRepository.findByPatientIdOrderByScheduledAtDesc(principal.getId()));
        return "paciente/citas-listado";
    }
}