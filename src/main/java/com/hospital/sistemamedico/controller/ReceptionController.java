package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.ReceptionSearchResult;
import com.hospital.sistemamedico.model.dto.SelectOptionDTO;
import com.hospital.sistemamedico.model.dto.WalkInAppointmentForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.enums.AppointmentPriority;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.BranchRepository;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.SpecialtyRepository;
import com.hospital.sistemamedico.service.AppointmentSchedulingService;
import com.hospital.sistemamedico.service.DoctorAvailabilityService;
import com.hospital.sistemamedico.service.ReceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin/recepcion")
@RequiredArgsConstructor
public class ReceptionController {

    private static final String SEARCH_VIEW = "admin/recepcion-buscar";
    private static final String REASSIGN_VIEW = "admin/recepcion-reasignar";
    private static final String WALK_IN_VIEW = "admin/recepcion-nueva";

    private final ReceptionService receptionService;
    private final AppointmentSchedulingService appointmentSchedulingService;
    private final DoctorAvailabilityService doctorAvailabilityService;
    private final AppointmentRepository appointmentRepository;
    private final InternalUserRepository internalUserRepository;
    private final BranchRepository branchRepository;
    private final SpecialtyRepository specialtyRepository;

    // ------------------------------------------------------------------ Búsqueda (flujo normal)

    @GetMapping("/buscar")
    public String showSearchForm(Model model) {
        model.addAttribute("searchMode", "dpi");
        return SEARCH_VIEW;
    }

    @PostMapping("/buscar")
    public String search(@RequestParam(required = false) String numero,
                         @RequestParam(required = false) String dpi,
                         Model model) {
        boolean byNumber = numero != null && !numero.isBlank();
        model.addAttribute("searchMode", byNumber ? "numero" : "dpi");
        model.addAttribute("numero", numero);
        model.addAttribute("dpi", dpi);

        try {
            ReceptionSearchResult result = receptionService.search(numero, dpi);
            model.addAttribute("outcome", result.getOutcome().name());
            model.addAttribute("appointment", result.getAppointment());
            model.addAttribute("patientName", result.getPatientName());
        } catch (BusinessRuleException ex) {
            model.addAttribute("searchError", ex.getMessage());
        }
        return SEARCH_VIEW;
    }

    // ------------------------------------------------------------------ Registrar llegada (paso 6-7, FA08, FA09)

    @PostMapping("/{id}/llegada")
    public String registerArrival(@PathVariable Integer id,
                                  @RequestParam(required = false) Long version,
                                  Model model) {
        try {
            Appointment appointment = receptionService.registerArrival(id, version);
            String name = appointment.getPatient().getFullName();

            String message = appointment.getPriority() == AppointmentPriority.EMERGENCIA
                    ? "Paciente %s registrado con prioridad de EMERGENCIA. El paciente debe pasar directamente a toma de signos vitales."
                    .formatted(name)
                    : "La llegada del paciente %s ha sido registrada exitosamente. El paciente debe pasar a la sala de espera."
                    .formatted(name);

            model.addAttribute("arrivalSuccess", message);
            showAppointment(model, appointment);

        } catch (BusinessRuleException | ObjectOptimisticLockingFailureException ex) {
            String message = (ex instanceof BusinessRuleException)
                    ? ex.getMessage()
                    : "Operación no permitida: la cita ya fue actualizada por otro usuario. Verifique el estado actual e intente nuevamente.";
            model.addAttribute("arrivalError", message);
            // Mostramos el estado ACTUAL de la cita, no el que tenía la pantalla antes del conflicto.
            appointmentRepository.findByIdWithDetails(id).ifPresent(a -> showAppointment(model, a));
        }
        return SEARCH_VIEW;
    }

    // ------------------------------------------------------------------ Reasignar médico (FA07)

    @GetMapping("/{id}/reasignar")
    public String showReassignForm(@PathVariable Integer id, Model model) {
        populateReassign(model, loadAppointment(id));
        return REASSIGN_VIEW;
    }

    @PostMapping("/{id}/reasignar")
    public String submitReassign(@PathVariable Integer id,
                                 @RequestParam(required = false) Integer newDoctorId,
                                 @RequestParam(required = false) String note,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            if (newDoctorId == null) {
                throw new BusinessRuleException("Debe seleccionar un médico.");
            }
            receptionService.reassignDoctor(id, newDoctorId, note);
            redirectAttributes.addFlashAttribute("flashMessage", "Médico reasignado correctamente");
            return "redirect:/admin/recepcion/buscar";
        } catch (BusinessRuleException ex) {
            populateReassign(model, loadAppointment(id));
            model.addAttribute("reassignError", ex.getMessage());
            return REASSIGN_VIEW;
        }
    }

    // ------------------------------------------------------------------ Nueva cita walk-in / emergencia (FA01, FA04, FA06)

    @GetMapping("/nueva")
    public String showWalkInForm(@RequestParam(required = false) String dpi,
                                 @RequestParam(required = false, defaultValue = "false") boolean emergency,
                                 Model model) {
        WalkInAppointmentForm form = new WalkInAppointmentForm();
        form.setPatientDpi(dpi);
        form.setEmergency(emergency);
        model.addAttribute("walkInForm", form);
        populateWalkInCatalogs(model);
        return WALK_IN_VIEW;
    }

    @PostMapping("/nueva")
    public String submitWalkIn(@Valid @ModelAttribute("walkInForm") WalkInAppointmentForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            populateWalkInCatalogs(model);
            return WALK_IN_VIEW;
        }

        try {
            Appointment appointment = appointmentSchedulingService.scheduleWalkIn(form);
            String name = appointment.getPatient().getFullName();

            String message = form.isEmergency()
                    ? "Paciente %s registrado con prioridad de EMERGENCIA. El paciente debe pasar directamente a toma de signos vitales."
                    .formatted(name)
                    : "Cita %s creada para el paciente %s. Queda pendiente de pago en caja."
                    .formatted(appointment.getAppointmentNumber(), name);

            redirectAttributes.addFlashAttribute("flashMessage", message);
            return "redirect:/admin/recepcion/buscar";

        } catch (BusinessRuleException ex) {
            model.addAttribute("walkInError", ex.getMessage());
            populateWalkInCatalogs(model);
            return WALK_IN_VIEW;
        }
    }

    // ------------------------------------------------------------------ Endpoints JSON para la pantalla walk-in

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

    // ------------------------------------------------------------------ Auxiliares

    private Appointment loadAppointment(Integer id) {
        return appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));
    }

    /** Deja el modelo listo para pintar la tarjeta de una cita en la pantalla de búsqueda. */
    private void showAppointment(Model model, Appointment appointment) {
        model.addAttribute("searchMode", "numero");
        model.addAttribute("numero", appointment.getAppointmentNumber());
        model.addAttribute("dpi", appointment.getPatient().getDpi());
        model.addAttribute("outcome", ReceptionSearchResult.Outcome.CITA_ENCONTRADA.name());
        model.addAttribute("appointment", appointment);
    }

    private void populateReassign(Model model, Appointment appointment) {
        Integer branchId = appointment.getBranchSpecialty().getBranch().getId();
        Integer specialtyId = appointment.getBranchSpecialty().getSpecialty().getId();
        Integer currentDoctorId = appointment.getDoctor().getId();

        List<SelectOptionDTO> doctors = internalUserRepository
                .findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue("Medico", branchId, specialtyId)
                .stream()
                .filter(doctor -> !doctor.getId().equals(currentDoctorId))
                .map(doctor -> new SelectOptionDTO(doctor.getId(), "Dr(a). " + doctor.getFullName()))
                .toList();

        model.addAttribute("appointment", appointment);
        model.addAttribute("doctors", doctors);
    }

    private void populateWalkInCatalogs(Model model) {
        model.addAttribute("branches", branchRepository.findByActiveTrue());
        model.addAttribute("specialties", specialtyRepository.findByActiveTrue());
        model.addAttribute("today", LocalDate.now());
    }
}
