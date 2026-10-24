package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.ReceptionSearchResult;
import com.hospital.sistemamedico.model.dto.SelectOptionDTO;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.service.ReceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/recepcion")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService receptionService;
    private final AppointmentRepository appointmentRepository;
    private final InternalUserRepository internalUserRepository;

    @GetMapping("/buscar")
    public String showSearchForm() {
        return "admin/recepcion-buscar";
    }

    @PostMapping("/buscar")
    public String search(@RequestParam(required = false) String numero,
                         @RequestParam(required = false) String dpi,
                         Model model) {
        try {
            ReceptionSearchResult result = receptionService.search(numero, dpi);
            model.addAttribute("outcome", result.getOutcome());
            model.addAttribute("appointment", result.getAppointment());
        } catch (BusinessRuleException ex) {
            model.addAttribute("searchError", ex.getMessage());
        }
        return "admin/recepcion-buscar";
    }

    @PostMapping("/{id}/llegada")
    public String registerArrival(@PathVariable Integer id,
                                  @RequestParam Long version,
                                  Model model) {
        try {
            Appointment appointment = receptionService.registerArrival(id, version);
            model.addAttribute("outcome", ReceptionSearchResult.Outcome.CITA_ENCONTRADA);
            model.addAttribute("appointment", appointment);
            model.addAttribute("arrivalSuccess", true);
        } catch (BusinessRuleException ex) {
            model.addAttribute("arrivalError", ex.getMessage());
            // Recargamos el estado ACTUAL de la cita (no el que tenía la pantalla antes
            // del conflicto) para que el recepcionista vea la realidad y decida qué hacer.
            appointmentRepository.findById(id).ifPresent(a -> {
                model.addAttribute("outcome", ReceptionSearchResult.Outcome.CITA_ENCONTRADA);
                model.addAttribute("appointment", a);
            });
        }
        return "admin/recepcion-buscar";
    }

    @GetMapping("/{id}/reasignar")
    public String showReassignForm(@PathVariable Integer id, Model model) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));

        model.addAttribute("appointment", appointment);
        model.addAttribute("doctors", availableDoctorsFor(appointment));
        return "admin/recepcion-reasignar";
    }

    @PostMapping("/{id}/reasignar")
    public String submitReassign(@PathVariable Integer id,
                                 @RequestParam Integer newDoctorId,
                                 Model model) {
        try {
            receptionService.reassignDoctor(id, newDoctorId);
            return "redirect:/admin/recepcion/buscar";
        } catch (BusinessRuleException ex) {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));
            model.addAttribute("appointment", appointment);
            model.addAttribute("doctors", availableDoctorsFor(appointment));
            model.addAttribute("reassignError", ex.getMessage());
            return "admin/recepcion-reasignar";
        }
    }

    private List<SelectOptionDTO> availableDoctorsFor(Appointment appointment) {
        Integer branchId = appointment.getBranchSpecialty().getBranch().getId();
        Integer specialtyId = appointment.getBranchSpecialty().getSpecialty().getId();
        Integer currentDoctorId = appointment.getDoctor().getId();

        return internalUserRepository
                .findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue("Medico", branchId, specialtyId)
                .stream()
                .filter(doctor -> !doctor.getId().equals(currentDoctorId))
                .map(doctor -> new SelectOptionDTO(doctor.getId(), "Dr(a). " + doctor.getFullName()))
                .toList();
    }
}