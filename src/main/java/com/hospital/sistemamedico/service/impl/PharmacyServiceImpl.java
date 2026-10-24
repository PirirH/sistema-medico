package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.*;
import com.hospital.sistemamedico.model.entity.*;
import com.hospital.sistemamedico.model.enums.DispensationStatus;
import com.hospital.sistemamedico.model.enums.MovementType;
import com.hospital.sistemamedico.model.enums.PrescriptionStatus;
import com.hospital.sistemamedico.repository.*;
import com.hospital.sistemamedico.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyServiceImpl implements PharmacyService {

    /** RN-CU10-01: la receta no debe exceder 7 días de antigüedad. */
    private static final int PRESCRIPTION_VALID_DAYS = 7;

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineInventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final DispensationRepository dispensationRepository;
    private final InternalUserRepository internalUserRepository;

    // ------------------------------------------------------------------ búsqueda

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionRow> search(String prescription, String consultation) {
        String number = prescription == null ? "" : prescription.trim().toUpperCase();
        String consultationText = consultation == null ? "" : consultation.trim();
        if (number.isEmpty() && consultationText.isEmpty()) {
            throw new BusinessRuleException("Debe ingresar un ID de receta o un ID de consulta para buscar.");
        }

        Integer id = -1;
        if (number.matches("\\d{1,9}")) {
            id = Integer.valueOf(number);
        }
        Integer consultationId = -1;
        if (!consultationText.isEmpty()) {
            if (!consultationText.matches("\\d{1,9}")) {
                throw new BusinessRuleException("El ID de consulta debe ser numérico.");
            }
            consultationId = Integer.valueOf(consultationText);
        }

        List<Prescription> found = prescriptionRepository.search(PrescriptionStatus.ACTIVA, number, id, consultationId);
        if (found.isEmpty()) {
            throw new BusinessRuleException("No se encontraron recetas activas para los datos ingresados.");
        }
        return found.stream().map(p -> {
            long days = daysSince(p);
            return new PrescriptionRow(p, days, days > PRESCRIPTION_VALID_DAYS);
        }).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ detalle

    @Override
    @Transactional(readOnly = true)
    public DispensePreview prepare(Integer prescriptionId, Integer pharmacistUserId) {
        Prescription prescription = loadActiveValid(prescriptionId);
        Branch branch = branchOf(pharmacistUserId);

        Map<Integer, MedicineInventory> inventoryByMedicine = inventoryOf(prescription, branch.getId());

        List<DispensePreviewLine> lines = new ArrayList<>();
        for (PrescriptionItem item : prescription.getItems()) {
            Medicine m = item.getMedicine();
            MedicineInventory inv = inventoryByMedicine.get(m.getId());
            Integer stock = inv == null ? null : inv.getCurrentStock();
            String message = null;
            if (inv == null) {
                message = "Sin inventario registrado"; // FA01
            } else if (m.getMinimumStock() != null && stock <= m.getMinimumStock()) {
                message = "%s: Stock bajo — disponible: %d (mínimo: %d).".formatted(m.getName(), stock, m.getMinimumStock());
            }
            lines.add(new DispensePreviewLine(item.getId(), m.getName(), m.getUnit(), m.isControlled(),
                    item.getDose(), item.getFrequency(), item.getDuration(), item.getInstructions(),
                    item.getQuantity(), m.getDefaultPrice(), stock, m.getMinimumStock(), message));
        }
        return new DispensePreview(prescription, lines, daysSince(prescription), branch.getName());
    }

    // ------------------------------------------------------------------ confirmar

    @Override
    @Transactional
    public Dispensation confirm(Integer prescriptionId, Integer pharmacistUserId, DispenseForm form) {
        Prescription prescription = loadActiveValid(prescriptionId);
        InternalUser pharmacist = pharmacist(pharmacistUserId);
        Branch branch = pharmacist.getBranch();
        if (branch == null) {
            throw noBranch();
        }

        Map<Integer, DispenseLineForm> linesByItem = new HashMap<>();
        if (form.getLines() != null) {
            for (DispenseLineForm line : form.getLines()) {
                if (line != null && line.getPrescriptionItemId() != null) {
                    linesByItem.put(line.getPrescriptionItemId(), line);
                }
            }
        }
        Map<Integer, MedicineInventory> inventoryByMedicine = inventoryOf(prescription, branch.getId());

        Dispensation dispensation = new Dispensation();
        dispensation.setDispensationNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        dispensation.setPrescription(prescription);
        dispensation.setBranch(branch);
        dispensation.setPharmacist(pharmacist);
        dispensation.setStatus(DispensationStatus.DESPACHADO);

        BigDecimal total = BigDecimal.ZERO;
        List<Object[]> stockChanges = new ArrayList<>(); // [MedicineInventory, cantidad, stock previo]

        for (PrescriptionItem item : prescription.getItems()) {
            DispenseLineForm line = linesByItem.get(item.getId());
            int qty = (line == null || line.getQuantity() == null) ? 0 : line.getQuantity();
            if (qty < 0) {
                throw new BusinessRuleException("La cantidad a despachar no puede ser negativa.");
            }
            if (qty == 0) {
                continue; // no se despacha este medicamento
            }
            Medicine medicine = item.getMedicine();
            if (qty > item.getQuantity()) {
                throw new BusinessRuleException("La cantidad a despachar de %s no puede exceder la cantidad recetada (%d)."
                        .formatted(medicine.getName(), item.getQuantity()));
            }
            MedicineInventory inv = inventoryByMedicine.get(medicine.getId());
            int stock = inv == null ? 0 : inv.getCurrentStock();
            if (inv == null || qty > stock) {
                throw new BusinessRuleException("Stock insuficiente de %s. Disponible: %d unidades."
                        .formatted(medicine.getName(), stock));
            }
            // FA02: sustitución
            if (line.isSubstituted()) {
                String reason = line.getSubstitutionReason() == null ? "" : line.getSubstitutionReason().trim();
                String alternative = line.getSubstituteName() == null ? "" : line.getSubstituteName().trim();
                if (reason.isEmpty() || alternative.isEmpty() || reason.length() > 300 || alternative.length() > 200) {
                    throw new BusinessRuleException(
                            "Para sustituir %s debe indicar la razón de sustitución y el medicamento alternativo."
                                    .formatted(medicine.getName()));
                }
            }
            // RNF-017: medicamentos controlados, autorización adicional con auditoría
            if (medicine.isControlled()) {
                if (!line.isControlledAuthorized()) {
                    throw new BusinessRuleException(
                            "El medicamento controlado %s requiere la autorización adicional del farmacéutico."
                                    .formatted(medicine.getName()));
                }
                log.warn("AUDITORIA DESPACHO CONTROLADO: farmacéutico={} receta={} medicamento={} cantidad={}",
                        pharmacist.getUsername(), prescription.getPrescriptionNumber(), medicine.getName(), qty);
            }

            DispensationItem di = new DispensationItem();
            di.setDispensation(dispensation);
            di.setMedicine(medicine);
            di.setQuantity(qty);
            di.setUnitPrice(medicine.getDefaultPrice());
            di.setSubstituted(line.isSubstituted());
            if (line.isSubstituted()) {
                di.setSubstitutionReason(line.getSubstitutionReason().trim());
                di.setSubstituteName(line.getSubstituteName().trim());
            }
            dispensation.getItems().add(di);
            total = total.add(di.getSubtotal());
            stockChanges.add(new Object[]{inv, qty, stock});
        }

        if (dispensation.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "Debe despachar al menos un medicamento. Si el paciente no desea adquirirlos, use 'Cancelar despacho'.");
        }
        dispensation.setTotalAmount(total);

        Dispensation saved = dispensationRepository.save(dispensation);
        saved.setDispensationNumber("DSP-" + Year.now().getValue() + "-" + String.format("%06d", saved.getId()));
        saved = dispensationRepository.save(saved);

        // Descuento de inventario + bitácora (tipo 6 = Despacho, automático). @Version evita carreras (RNF-025).
        for (Object[] change : stockChanges) {
            MedicineInventory inv = (MedicineInventory) change[0];
            int qty = (Integer) change[1];
            int previous = (Integer) change[2];

            inv.setCurrentStock(previous - qty);
            inventoryRepository.save(inv);

            InventoryMovement movement = new InventoryMovement();
            movement.setMovementType(MovementType.DESPACHO);
            movement.setMedicine(inv.getMedicine());
            movement.setBranch(branch);
            movement.setQuantity(qty);
            movement.setPreviousStock(previous);
            movement.setNewStock(previous - qty);
            movement.setReferenceNumber(saved.getDispensationNumber());
            movement.setNotes("Despacho de receta " + prescription.getPrescriptionNumber());
            movement.setPerformedBy(pharmacist);
            movementRepository.save(movement);
        }

        prescription.setStatus(PrescriptionStatus.DESPACHADA);
        prescriptionRepository.save(prescription);
        return saved;
    }

    // ------------------------------------------------------------------ FA03

    @Override
    @Transactional
    public String markNotPurchased(Integer prescriptionId, Integer pharmacistUserId) {
        Prescription prescription = loadActiveValid(prescriptionId);
        InternalUser pharmacist = pharmacist(pharmacistUserId);
        if (pharmacist.getBranch() == null) {
            throw noBranch();
        }

        Dispensation dispensation = new Dispensation();
        dispensation.setDispensationNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        dispensation.setPrescription(prescription);
        dispensation.setBranch(pharmacist.getBranch());
        dispensation.setPharmacist(pharmacist);
        dispensation.setStatus(DispensationStatus.NO_ADQUIRIDO);
        dispensation.setTotalAmount(BigDecimal.ZERO);
        Dispensation saved = dispensationRepository.save(dispensation);
        saved.setDispensationNumber("DSP-" + Year.now().getValue() + "-" + String.format("%06d", saved.getId()));
        dispensationRepository.save(saved);

        prescription.setStatus(PrescriptionStatus.NO_ADQUIRIDA);
        prescriptionRepository.save(prescription);

        String message = "Se ha registrado que el paciente %s no adquirió los medicamentos recetados en farmacia interna. Receta: %s."
                .formatted(prescription.getPatient().getFullName(), prescription.getPrescriptionNumber());
        log.info(message);
        return message;
    }

    // ------------------------------------------------------------------ resumen

    @Override
    @Transactional(readOnly = true)
    public DispensationSummary getSummary(Integer dispensationId) {
        Dispensation dispensation = dispensationRepository.findByIdWithDetails(dispensationId)
                .orElseThrow(() -> new BusinessRuleException("Despacho no encontrado."));

        List<String> alerts = new ArrayList<>();
        Set<Integer> medicineIds = dispensation.getItems().stream()
                .map(i -> i.getMedicine().getId()).collect(Collectors.toSet());
        if (!medicineIds.isEmpty()) {
            for (MedicineInventory inv : inventoryRepository.findByBranch_IdAndMedicine_IdIn(
                    dispensation.getBranch().getId(), medicineIds)) {
                Integer min = inv.getMedicine().getMinimumStock();
                if (min != null && inv.getCurrentStock() <= min) {
                    alerts.add("ALERTA: El medicamento %s ha alcanzado el nivel de stock mínimo (%d unidades restantes). Se recomienda generar orden de reabastecimiento."
                            .formatted(inv.getMedicine().getName(), inv.getCurrentStock()));
                }
            }
        }
        return new DispensationSummary(dispensation, alerts);
    }

    // ------------------------------------------------------------------ auxiliares

    private Prescription loadActiveValid(Integer prescriptionId) {
        Prescription prescription = prescriptionRepository.findByIdWithDetails(prescriptionId)
                .orElseThrow(() -> new BusinessRuleException("Receta no encontrada."));

        if (prescription.getStatus() != PrescriptionStatus.ACTIVA) {
            throw new BusinessRuleException("La receta %s ya no está activa.".formatted(prescription.getPrescriptionNumber()));
        }
        long days = daysSince(prescription);
        if (days > PRESCRIPTION_VALID_DAYS) {
            throw new BusinessRuleException(
                    "Receta Vencida. La receta #%s fue emitida hace %d días y ya no es válida para despacho."
                            .formatted(prescription.getPrescriptionNumber(), days));
        }
        return prescription;
    }

    private long daysSince(Prescription prescription) {
        return ChronoUnit.DAYS.between(prescription.getCreatedAt().toLocalDate(), LocalDate.now());
    }

    private Map<Integer, MedicineInventory> inventoryOf(Prescription prescription, Integer branchId) {
        Set<Integer> medicineIds = prescription.getItems().stream()
                .map(i -> i.getMedicine().getId()).collect(Collectors.toSet());
        Map<Integer, MedicineInventory> map = new HashMap<>();
        if (!medicineIds.isEmpty()) {
            for (MedicineInventory inv : inventoryRepository.findByBranch_IdAndMedicine_IdIn(branchId, medicineIds)) {
                map.put(inv.getMedicine().getId(), inv);
            }
        }
        return map;
    }

    private InternalUser pharmacist(Integer userId) {
        return internalUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Usuario de farmacia no encontrado."));
    }

    private Branch branchOf(Integer userId) {
        Branch branch = pharmacist(userId).getBranch();
        if (branch == null) {
            throw noBranch();
        }
        return branch;
    }

    private BusinessRuleException noBranch() {
        return new BusinessRuleException(
                "Su usuario no tiene una sucursal asignada, necesaria para consultar el inventario. Contacte al administrador.");
    }
}
