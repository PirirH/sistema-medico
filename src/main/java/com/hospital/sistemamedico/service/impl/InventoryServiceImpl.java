package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.InventoryMovementForm;
import com.hospital.sistemamedico.model.dto.InventoryPanel;
import com.hospital.sistemamedico.model.dto.InventorySearchFilter;
import com.hospital.sistemamedico.model.entity.*;
import com.hospital.sistemamedico.model.enums.MovementType;
import com.hospital.sistemamedico.repository.*;
import com.hospital.sistemamedico.service.InventoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int MAX_ROWS = 300;

    @PersistenceContext
    private EntityManager entityManager;

    private final InventoryMovementRepository movementRepository;
    private final MedicineInventoryRepository inventoryRepository;
    private final MedicineRepository medicineRepository;
    private final BranchRepository branchRepository;
    private final InternalUserRepository internalUserRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovement> search(InventorySearchFilter f) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<InventoryMovement> query = cb.createQuery(InventoryMovement.class);
        Root<InventoryMovement> m = query.from(InventoryMovement.class);

        List<Predicate> predicates = new ArrayList<>();
        if (f.getType() != null) {
            predicates.add(cb.equal(m.get("movementType"), f.getType()));
        }
        if (f.getMedicineId() != null) {
            predicates.add(cb.equal(m.get("medicine").get("id"), f.getMedicineId()));
        }
        if (f.getBranchId() != null) {
            predicates.add(cb.equal(m.get("branch").get("id"), f.getBranchId()));
        }
        if (f.getReference() != null && !f.getReference().isBlank()) {
            predicates.add(cb.like(cb.lower(m.<String>get("referenceNumber")),
                    "%" + f.getReference().trim().toLowerCase() + "%"));
        }
        if (f.getUser() != null && !f.getUser().isBlank()) {
            Join<InventoryMovement, InternalUser> u = m.join("performedBy", JoinType.LEFT);
            String like = "%" + f.getUser().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(u.<String>get("fullName")), like),
                    cb.like(cb.lower(u.<String>get("username")), like)));
        }
        if (f.getDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(m.<LocalDateTime>get("createdAt"), f.getDateFrom().atStartOfDay()));
        }
        if (f.getDateTo() != null) {
            predicates.add(cb.lessThan(m.<LocalDateTime>get("createdAt"), f.getDateTo().plusDays(1).atStartOfDay()));
        }

        query.select(m).where(predicates.toArray(new Predicate[0])).orderBy(cb.desc(m.get("createdAt")));
        return entityManager.createQuery(query).setMaxResults(MAX_ROWS).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryMovement get(Integer id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Movimiento no encontrado."));
    }

    @Override
    @Transactional
    public InventoryMovement register(InventoryMovementForm form, Integer userId) {
        // ---- RN-CU13-01
        if (form.getMedicineId() == null) {
            throw new BusinessRuleException("Debe seleccionar un medicamento.");
        }
        if (form.getBranchId() == null) {
            throw new BusinessRuleException("Debe seleccionar una sucursal.");
        }
        MovementType type = form.getMovementType();
        if (type == null || !type.isManual()) {
            throw new BusinessRuleException("Debe seleccionar el tipo de movimiento.");
        }
        if (form.getQuantity() == null || form.getQuantity() <= 0) {
            throw new BusinessRuleException("La cantidad debe ser un número entero positivo.");
        }

        BigDecimal unitCost = form.getUnitCost();
        if (type == MovementType.COMPRA) {
            if (unitCost == null) {
                throw new BusinessRuleException("El costo unitario es obligatorio para compras");
            }
            if (unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("El costo unitario debe ser mayor a 0");
            }
            if (unitCost.scale() > 2 && unitCost.stripTrailingZeros().scale() > 2) {
                throw new BusinessRuleException("El costo unitario debe tener máximo 2 decimales");
            }
        } else {
            unitCost = null; // el costo solo aplica a compras
        }

        String notes = form.getNotes() == null ? "" : form.getNotes().trim();
        if (type == MovementType.VENTA) {
            notes = ""; // la venta no usa motivo
        } else if (type.isNotesRequired() && (notes.length() < 10 || notes.length() > 500)) {
            throw new BusinessRuleException("El motivo debe contener entre 10 y 500 caracteres.");
        } else if (notes.length() > 500) {
            throw new BusinessRuleException("Las notas no pueden exceder 500 caracteres.");
        }
        String reference = form.getReferenceNumber() == null ? "" : form.getReferenceNumber().trim();
        if (reference.length() > 50) {
            throw new BusinessRuleException("El número de referencia no puede exceder 50 caracteres.");
        }

        Medicine medicine = medicineRepository.findById(form.getMedicineId())
                .orElseThrow(() -> new BusinessRuleException("Debe seleccionar un medicamento."));
        Branch branch = branchRepository.findById(form.getBranchId())
                .orElseThrow(() -> new BusinessRuleException("Debe seleccionar una sucursal."));
        InternalUser user = internalUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Usuario no encontrado."));

        // ---- Inventario (se crea en 0 si un ingreso llega a una sucursal sin registro)
        MedicineInventory inventory = inventoryRepository
                .findByMedicine_IdAndBranch_Id(medicine.getId(), branch.getId())
                .orElse(null);
        int previous = inventory == null ? 0 : inventory.getCurrentStock();

        // ---- RN-CU13-02: salidas no pueden exceder el stock actual
        if (!type.isEntry() && form.getQuantity() > previous) {
            throw new BusinessRuleException(
                    "Stock insuficiente. Stock actual: %d. No se puede registrar una salida de %d unidades."
                            .formatted(previous, form.getQuantity()));
        }

        int updated = type.isEntry() ? previous + form.getQuantity() : previous - form.getQuantity();

        if (inventory == null) {
            inventory = new MedicineInventory();
            inventory.setMedicine(medicine);
            inventory.setBranch(branch);
        }
        inventory.setCurrentStock(updated);
        inventoryRepository.save(inventory); // @Version: dos movimientos simultáneos no se pisan (RNF-025)

        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(type);
        movement.setMedicine(medicine);
        movement.setBranch(branch);
        movement.setQuantity(form.getQuantity());
        movement.setPreviousStock(previous);
        movement.setNewStock(updated);
        movement.setUnitCost(unitCost);
        movement.setReferenceNumber(reference.isEmpty() ? null : reference);
        movement.setNotes(notes.isEmpty() ? null : notes);
        movement.setPerformedBy(user);
        return movementRepository.save(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryPanel panel(Integer medicineId, Integer branchId) {
        Integer minimum = medicineRepository.findById(medicineId).map(Medicine::getMinimumStock).orElse(null);
        return inventoryRepository.findByMedicine_IdAndBranch_Id(medicineId, branchId)
                .map(inv -> new InventoryPanel(true, inv.getCurrentStock(), minimum))
                .orElseGet(() -> new InventoryPanel(false, 0, minimum));
    }

    @Override
    @Transactional
    public InventoryMovement toggleActive(Integer id) {
        InventoryMovement movement = get(id);
        movement.setActive(!movement.isActive());
        return movementRepository.save(movement);
    }
}
