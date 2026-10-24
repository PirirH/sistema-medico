package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.CatalogForm;
import com.hospital.sistemamedico.model.dto.CatalogMeta;
import com.hospital.sistemamedico.model.dto.CatalogRow;
import com.hospital.sistemamedico.model.dto.PageResult;
import com.hospital.sistemamedico.model.entity.*;
import com.hospital.sistemamedico.model.enums.MovementType;
import com.hospital.sistemamedico.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

/** CU-14 Mantenimiento de Catálogos del Sistema (CRUD genérico con eliminación lógica). */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private static final Set<String> BASE_ROLES = Set.of(
            "medico", "enfermero", "recepcionista", "cajero", "laboratorista", "farmaceutico", "administrador");

    private static final Map<String, CatalogMeta> METAS = new LinkedHashMap<>();
    static {
        METAS.put("especialidades", new CatalogMeta("especialidades", "Especialidades", "Descripción", Set.of("name", "description")));
        METAS.put("sucursales", new CatalogMeta("sucursales", "Sucursales", "Dirección / Teléfono",
                Set.of("name", "phone", "address", "openingHours")));
        METAS.put("laboratorios", new CatalogMeta("laboratorios", "Laboratorios", "Descripción", Set.of("name", "description")));
        METAS.put("examenes", new CatalogMeta("examenes", "Exámenes de Laboratorio", "Laboratorio / Precio",
                Set.of("name", "description", "price", "referenceRange", "unit", "laboratoryId")));
        METAS.put("medicamentos", new CatalogMeta("medicamentos", "Medicamentos", "Precio / Unidad",
                Set.of("name", "description", "price", "unit", "controlled", "minimumStock")));
        METAS.put("roles", new CatalogMeta("roles", "Roles", "—", Set.of("name")));
        METAS.put("inventario", new CatalogMeta("inventario", "Inventario de Medicamentos", "Stock actual",
                Set.of("medicineId", "branchId", "currentStock")));
    }

    private final SpecialtyRepository specialtyRepository;
    private final BranchRepository branchRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final LabExamRepository labExamRepository;
    private final MedicineRepository medicineRepository;
    private final RoleRepository roleRepository;
    private final MedicineInventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final InternalUserRepository userRepository;
    private final AuditService auditService;

    public static Collection<CatalogMeta> allMetas() {
        return METAS.values();
    }

    public CatalogMeta meta(String tipo) {
        CatalogMeta m = METAS.get(tipo);
        if (m == null) {
            throw new BusinessRuleException("El catálogo solicitado no existe.");
        }
        return m;
    }

    // ---------------------------------------------------------------- listado

    @Transactional(readOnly = true)
    public PageResult<CatalogRow> list(String tipo, String q, int page, int size) {
        meta(tipo);
        String needle = q == null ? "" : normalize(q.trim());
        List<CatalogRow> rows = rows(tipo).stream()
                .filter(r -> needle.isEmpty() || normalize(r.getName()).contains(needle))
                .toList();
        int safeSize = (size == 10 || size == 25 || size == 50) ? size : 10;
        return PageResult.of(rows, page, safeSize);
    }

    private List<CatalogRow> rows(String tipo) {
        List<CatalogRow> out = new ArrayList<>();
        switch (tipo) {
            case "especialidades" -> specialtyRepository.findAll().forEach(s ->
                    out.add(new CatalogRow(s.getId(), s.getName(), nz(s.getDescription()), s.isActive())));
            case "sucursales" -> branchRepository.findAll().forEach(b ->
                    out.add(new CatalogRow(b.getId(), b.getName(), nz(b.getAddress()) + " " + nz(b.getPhone()), b.isActive())));
            case "laboratorios" -> laboratoryRepository.findAll().forEach(l ->
                    out.add(new CatalogRow(l.getId(), l.getName(), nz(l.getDescription()), l.isActive())));
            case "examenes" -> labExamRepository.findAll().forEach(x ->
                    out.add(new CatalogRow(x.getId(), x.getName(),
                            x.getLaboratory().getName() + " — Q" + x.getDefaultAmount().setScale(2), x.isActive())));
            case "medicamentos" -> medicineRepository.findAll().forEach(m ->
                    out.add(new CatalogRow(m.getId(), m.getName() + (m.isControlled() ? " (CONTROLADO)" : ""),
                            "Q" + m.getDefaultPrice().setScale(2) + " / " + m.getUnit(), m.isActive())));
            case "roles" -> roleRepository.findAll().forEach(r ->
                    out.add(new CatalogRow(r.getId(), r.getName(), "", r.isActive())));
            case "inventario" -> inventoryRepository.findAll().forEach(i ->
                    out.add(new CatalogRow(i.getId(), i.getMedicine().getName() + " @ " + i.getBranch().getName(),
                            String.valueOf(i.getCurrentStock()), i.isActive())));
            default -> throw new BusinessRuleException("El catálogo solicitado no existe.");
        }
        out.sort(Comparator.comparing(CatalogRow::getId));
        return out;
    }

    // ---------------------------------------------------------------- consulta para edición

    @Transactional(readOnly = true)
    public CatalogForm loadForm(String tipo, Integer id) {
        meta(tipo);
        CatalogForm f = new CatalogForm();
        switch (tipo) {
            case "especialidades" -> {
                Specialty s = specialtyRepository.findById(id).orElseThrow(this::notFound);
                f.setName(s.getName()); f.setDescription(s.getDescription()); f.setActive(s.isActive());
            }
            case "sucursales" -> {
                Branch b = branchRepository.findById(id).orElseThrow(this::notFound);
                f.setName(b.getName()); f.setPhone(b.getPhone()); f.setAddress(b.getAddress());
                f.setOpeningHours(b.getOpeningHours()); f.setActive(b.isActive());
            }
            case "laboratorios" -> {
                Laboratory l = laboratoryRepository.findById(id).orElseThrow(this::notFound);
                f.setName(l.getName()); f.setDescription(l.getDescription()); f.setActive(l.isActive());
            }
            case "examenes" -> {
                LabExam x = labExamRepository.findById(id).orElseThrow(this::notFound);
                f.setName(x.getName()); f.setDescription(x.getDescription()); f.setPrice(x.getDefaultAmount());
                f.setReferenceRange(x.getReferenceRange()); f.setUnit(x.getUnit());
                f.setLaboratoryId(x.getLaboratory().getId()); f.setActive(x.isActive());
            }
            case "medicamentos" -> {
                Medicine m = medicineRepository.findById(id).orElseThrow(this::notFound);
                f.setName(m.getName()); f.setDescription(m.getDescription()); f.setPrice(m.getDefaultPrice());
                f.setUnit(m.getUnit()); f.setControlled(m.isControlled()); f.setMinimumStock(m.getMinimumStock());
                f.setActive(m.isActive());
            }
            case "roles" -> {
                Role r = roleRepository.findById(id).orElseThrow(this::notFound);
                f.setName(r.getName()); f.setActive(r.isActive());
            }
            case "inventario" -> {
                MedicineInventory i = inventoryRepository.findById(id).orElseThrow(this::notFound);
                f.setMedicineId(i.getMedicine().getId()); f.setBranchId(i.getBranch().getId());
                f.setCurrentStock(i.getCurrentStock()); f.setActive(i.isActive());
            }
            default -> throw notFound();
        }
        return f;
    }

    public Map<String, Object> lookups(String tipo) {
        Map<String, Object> m = new HashMap<>();
        if ("examenes".equals(tipo)) {
            m.put("laboratories", laboratoryRepository.findByActiveTrueOrderByNameAsc());
        }
        if ("inventario".equals(tipo)) {
            m.put("medicines", medicineRepository.findByActiveTrueOrderByNameAsc());
            m.put("branches", branchRepository.findByActiveTrue());
        }
        return m;
    }

    // ---------------------------------------------------------------- validación (RN-CU15-01..03)

    @Transactional(readOnly = true)
    public Map<String, String> validate(String tipo, CatalogForm f, Integer editingId) {
        CatalogMeta meta = meta(tipo);
        Map<String, String> e = new LinkedHashMap<>();

        if (meta.has("name")) {
            String name = f.getName() == null ? "" : f.getName().trim();
            int max = switch (tipo) {
                case "sucursales" -> 100;
                case "roles" -> 50;
                default -> 200;
            };
            if (name.isEmpty()) {
                e.put("name", "El nombre es obligatorio.");
            } else if (name.length() > max) {
                e.put("name", "El nombre no puede exceder los " + max + " caracteres.");
            } else if (f.isActive() && rows(tipo).stream().anyMatch(r -> r.isActive()
                    && !Objects.equals(r.getId(), editingId)
                    && baseName(r.getName()).equalsIgnoreCase(name))) {
                e.put("name", "Ya existe un registro con el nombre " + name + ".");
            }
            if ("roles".equals(tipo) && editingId != null) {
                roleRepository.findById(editingId).ifPresent(r -> {
                    if (isBaseRole(r.getName()) && (!r.getName().equals(name) || !f.isActive())) {
                        e.put("name", "Los roles base del sistema no pueden modificarse ni desactivarse.");
                    }
                });
            }
        }

        if (meta.has("description")) {
            String d = f.getDescription() == null ? "" : f.getDescription().trim();
            boolean required = "especialidades".equals(tipo) || "medicamentos".equals(tipo);
            if (required && d.isEmpty()) {
                e.put("description", "La descripción es obligatoria.");
            } else if (d.length() > 500) {
                e.put("description", "La descripción no puede exceder los 500 caracteres.");
            }
        }

        if ("sucursales".equals(tipo)) {
            String phone = f.getPhone() == null ? "" : f.getPhone().trim();
            if (!phone.isEmpty() && !phone.matches("\\d{8}")) {
                e.put("phone", "El teléfono debe contener exactamente 8 dígitos.");
            }
        }

        if ("examenes".equals(tipo)) {
            if (f.getPrice() == null || f.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                e.put("price", "El precio base debe ser mayor a 0.");
            }
            if (f.getLaboratoryId() == null) {
                e.put("laboratoryId", "Debe seleccionar un laboratorio.");
            }
        }

        if ("medicamentos".equals(tipo)) {
            if (f.getPrice() == null || f.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                e.put("price", "El precio debe ser mayor a 0.");
            }
            String unit = f.getUnit() == null ? "" : f.getUnit().trim();
            if (unit.isEmpty()) {
                e.put("unit", "La unidad es obligatoria.");
            } else if (unit.length() > 50) {
                e.put("unit", "La unidad no puede exceder los 50 caracteres.");
            }
            if (f.getMinimumStock() != null && f.getMinimumStock() < 0) {
                e.put("minimumStock", "El stock mínimo debe ser mayor o igual a 0.");
            }
        }

        if ("inventario".equals(tipo)) {
            if (f.getMedicineId() == null) e.put("medicineId", "Debe seleccionar un medicamento.");
            if (f.getBranchId() == null) e.put("branchId", "Debe seleccionar una sucursal.");
            if (f.getCurrentStock() == null || f.getCurrentStock() < 0) {
                e.put("currentStock", "El stock actual debe ser un número mayor o igual a 0.");
            }
            if (f.getMedicineId() != null && f.getBranchId() != null && editingId == null) {
                inventoryRepository.findByMedicine_IdAndBranch_Id(f.getMedicineId(), f.getBranchId()).ifPresent(i -> {
                    if (i.isActive()) {
                        e.put("medicineId", "Ya existe un registro de inventario para ese medicamento en esa sucursal.");
                    }
                });
            }
        }
        return e;
    }

    // ---------------------------------------------------------------- escritura

    @Transactional
    public String save(String tipo, Integer editingId, CatalogForm f, Integer actorId) {
        meta(tipo);
        String name;
        Integer id;
        switch (tipo) {
            case "especialidades" -> {
                Specialty s = editingId == null ? new Specialty() : specialtyRepository.findById(editingId).orElseThrow(this::notFound);
                s.setName(f.getName().trim()); s.setDescription(blankToNull(f.getDescription())); s.setActive(f.isActive());
                s = specialtyRepository.save(s); name = s.getName(); id = s.getId();
            }
            case "sucursales" -> {
                Branch b = editingId == null ? new Branch() : branchRepository.findById(editingId).orElseThrow(this::notFound);
                b.setName(f.getName().trim()); b.setPhone(blankToNull(f.getPhone())); b.setAddress(blankToNull(f.getAddress()));
                b.setOpeningHours(blankToNull(f.getOpeningHours())); b.setActive(f.isActive());
                b = branchRepository.save(b); name = b.getName(); id = b.getId();
            }
            case "laboratorios" -> {
                Laboratory l = editingId == null ? new Laboratory() : laboratoryRepository.findById(editingId).orElseThrow(this::notFound);
                l.setName(f.getName().trim()); l.setDescription(blankToNull(f.getDescription())); l.setActive(f.isActive());
                l = laboratoryRepository.save(l); name = l.getName(); id = l.getId();
            }
            case "examenes" -> {
                LabExam x = editingId == null ? new LabExam() : labExamRepository.findById(editingId).orElseThrow(this::notFound);
                x.setName(f.getName().trim()); x.setDescription(blankToNull(f.getDescription()));
                x.setDefaultAmount(f.getPrice()); x.setReferenceRange(blankToNull(f.getReferenceRange()));
                x.setUnit(blankToNull(f.getUnit()));
                x.setLaboratory(laboratoryRepository.findById(f.getLaboratoryId())
                        .orElseThrow(() -> new BusinessRuleException("Debe seleccionar un laboratorio.")));
                x.setActive(f.isActive());
                x = labExamRepository.save(x); name = x.getName(); id = x.getId();
            }
            case "medicamentos" -> {
                Medicine m = editingId == null ? new Medicine() : medicineRepository.findById(editingId).orElseThrow(this::notFound);
                m.setName(f.getName().trim()); m.setDescription(f.getDescription().trim());
                m.setDefaultPrice(f.getPrice()); m.setUnit(f.getUnit().trim()); m.setControlled(f.isControlled());
                m.setMinimumStock(f.getMinimumStock()); m.setActive(f.isActive());
                m = medicineRepository.save(m); name = m.getName(); id = m.getId();
            }
            case "roles" -> {
                Role r = editingId == null ? new Role() : roleRepository.findById(editingId).orElseThrow(this::notFound);
                r.setName(f.getName().trim()); r.setActive(f.isActive());
                r = roleRepository.save(r); name = r.getName(); id = r.getId();
            }
            case "inventario" -> {
                MedicineInventory i = editingId != null ? inventoryRepository.findById(editingId).orElseThrow(this::notFound)
                        : inventoryRepository.findByMedicine_IdAndBranch_Id(f.getMedicineId(), f.getBranchId()).orElse(new MedicineInventory());
                int previous = i.getId() == null ? 0 : i.getCurrentStock();
                Medicine med = medicineRepository.findById(f.getMedicineId())
                        .orElseThrow(() -> new BusinessRuleException("Debe seleccionar un medicamento."));
                Branch br = branchRepository.findById(f.getBranchId())
                        .orElseThrow(() -> new BusinessRuleException("Debe seleccionar una sucursal."));
                i.setMedicine(med); i.setBranch(br); i.setCurrentStock(f.getCurrentStock()); i.setActive(f.isActive());
                i = inventoryRepository.save(i);
                int diff = f.getCurrentStock() - previous;
                if (diff != 0) { // el ajuste desde el catálogo también deja rastro en la bitácora (CU-15)
                    InventoryMovement mv = new InventoryMovement();
                    mv.setMovementType(diff > 0 ? MovementType.AJUSTE_POSITIVO : MovementType.AJUSTE_NEGATIVO);
                    mv.setMedicine(med); mv.setBranch(br); mv.setQuantity(Math.abs(diff));
                    mv.setPreviousStock(previous); mv.setNewStock(f.getCurrentStock());
                    mv.setReferenceNumber("CATALOGO");
                    mv.setNotes("Ajuste realizado desde el mantenimiento de catálogos.");
                    mv.setPerformedBy(userRepository.getReferenceById(actorId));
                    movementRepository.save(mv);
                }
                name = med.getName() + " @ " + br.getName(); id = i.getId();
            }
            default -> throw notFound();
        }
        auditService.log(editingId == null ? "CREAR" : "ACTUALIZAR", "CATALOGO_" + tipo.toUpperCase(Locale.ROOT), id, name, actorId);
        return name;
    }

    /** Eliminación lógica (State = 0). */
    @Transactional
    public String delete(String tipo, Integer id, Integer actorId) {
        meta(tipo);
        String name;
        switch (tipo) {
            case "especialidades" -> { Specialty s = specialtyRepository.findById(id).orElseThrow(this::notFound);
                s.setActive(false); specialtyRepository.save(s); name = s.getName(); }
            case "sucursales" -> { Branch b = branchRepository.findById(id).orElseThrow(this::notFound);
                b.setActive(false); branchRepository.save(b); name = b.getName(); }
            case "laboratorios" -> { Laboratory l = laboratoryRepository.findById(id).orElseThrow(this::notFound);
                l.setActive(false); laboratoryRepository.save(l); name = l.getName(); }
            case "examenes" -> { LabExam x = labExamRepository.findById(id).orElseThrow(this::notFound);
                x.setActive(false); labExamRepository.save(x); name = x.getName(); }
            case "medicamentos" -> { Medicine m = medicineRepository.findById(id).orElseThrow(this::notFound);
                m.setActive(false); medicineRepository.save(m); name = m.getName(); }
            case "roles" -> { Role r = roleRepository.findById(id).orElseThrow(this::notFound);
                if (isBaseRole(r.getName())) {
                    throw new BusinessRuleException("Los roles base del sistema no pueden modificarse ni desactivarse.");
                }
                r.setActive(false); roleRepository.save(r); name = r.getName(); }
            case "inventario" -> { MedicineInventory i = inventoryRepository.findById(id).orElseThrow(this::notFound);
                i.setActive(false); inventoryRepository.save(i); name = i.getMedicine().getName() + " @ " + i.getBranch().getName(); }
            default -> throw notFound();
        }
        auditService.log("ELIMINAR", "CATALOGO_" + tipo.toUpperCase(Locale.ROOT), id, name, actorId);
        return name;
    }

    // ---------------------------------------------------------------- auxiliares

    private BusinessRuleException notFound() {
        return new BusinessRuleException("El registro solicitado no existe.");
    }

    private static boolean isBaseRole(String name) {
        return BASE_ROLES.contains(normalize(name));
    }

    /** Quita el sufijo " (CONTROLADO)" que se agrega solo para mostrar. */
    private static String baseName(String displayName) {
        return displayName.replace(" (CONTROLADO)", "");
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
