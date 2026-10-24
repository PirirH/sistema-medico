package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.InventoryMovementForm;
import com.hospital.sistemamedico.model.dto.InventoryPanel;
import com.hospital.sistemamedico.model.dto.InventorySearchFilter;
import com.hospital.sistemamedico.model.entity.InventoryMovement;

import java.util.List;

/** CU-15: Bitácora de movimientos de inventario. */
public interface InventoryService {

    List<InventoryMovement> search(InventorySearchFilter filter);

    InventoryMovement get(Integer id);

    /** Pasos 6-8: valida (RN-CU13-01/02), registra el movimiento y actualiza el stock. */
    InventoryMovement register(InventoryMovementForm form, Integer userId);

    /** Panel informativo del formulario (RN-CU13-03). */
    InventoryPanel panel(Integer medicineId, Integer branchId);

    /** Acción "Desactivar/Activar" de la tabla. */
    InventoryMovement toggleActive(Integer id);
}
