package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.BranchSpecialtyForm;
import com.hospital.sistemamedico.model.entity.Branch;
import com.hospital.sistemamedico.model.entity.BranchSpecialty;
import com.hospital.sistemamedico.model.entity.Specialty;
import com.hospital.sistemamedico.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** CU-13 Configuración de Sedes y Especialidades. */
@Service
@RequiredArgsConstructor
public class BranchSpecialtyAdminService {

    private final BranchSpecialtyRepository repository;
    private final BranchRepository branchRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BranchSpecialty> list(Integer idFilter) {
        List<BranchSpecialty> all = repository.findAllWithDetails();
        if (idFilter == null) {
            return all;
        }
        return all.stream().filter(bs -> bs.getId().equals(idFilter)).toList();
    }

    public List<Branch> activeBranches() {
        return branchRepository.findByActiveTrue();
    }

    public List<Specialty> activeSpecialties() {
        return specialtyRepository.findByActiveTrue();
    }

    @Transactional
    public void assign(BranchSpecialtyForm f, Integer actorId) {
        if (f.getBranchId() == null) {
            throw new BusinessRuleException("Debe seleccionar una sede.");
        }
        if (f.getSpecialtyId() == null) {
            throw new BusinessRuleException("Debe seleccionar una especialidad.");
        }
        BigDecimal price = f.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El precio de la consulta debe ser mayor a 0.");
        }
        Branch branch = branchRepository.findById(f.getBranchId())
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessRuleException("Debe seleccionar una sede."));
        Specialty specialty = specialtyRepository.findById(f.getSpecialtyId())
                .filter(Specialty::isActive)
                .orElseThrow(() -> new BusinessRuleException("Debe seleccionar una especialidad."));

        BranchSpecialty existing = repository.findByBranch_IdAndSpecialty_Id(branch.getId(), specialty.getId()).orElse(null);
        if (existing != null && existing.isActive()) {
            throw new BusinessRuleException("Esta combinación de sede y especialidad ya existe en el sistema.");
        }
        BranchSpecialty bs = existing != null ? existing : new BranchSpecialty();
        bs.setBranch(branch);
        bs.setSpecialty(specialty);
        bs.setPrice(price);
        bs.setActive(true);
        bs = repository.save(bs);
        auditService.log("ASIGNAR", "SEDE_ESPECIALIDAD", bs.getId(),
                branch.getName() + " / " + specialty.getName(), actorId);
    }

    /** Si la asignación ya tiene citas se desactiva (se conserva el historial); si no, se elimina. */
    @Transactional
    public String remove(Integer id, Integer actorId) {
        BranchSpecialty bs = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("La asignación solicitada no existe."));
        String label = bs.getBranch().getName() + " / " + bs.getSpecialty().getName();
        String message;
        if (appointmentRepository.existsByBranchSpecialty_Id(id)) {
            bs.setActive(false);
            repository.save(bs);
            message = "La asignación " + label + " tiene citas registradas, por lo que fue desactivada.";
        } else {
            repository.delete(bs);
            message = "Asignación eliminada correctamente.";
        }
        auditService.log("ELIMINAR", "SEDE_ESPECIALIDAD", id, label, actorId);
        return message;
    }
}
