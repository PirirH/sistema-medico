package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.LabResultForm;
import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.enums.LabOrderStatus;

import java.util.List;

/** CU-09: Gestión de laboratorio (personal de laboratorio). */
public interface LabService {

    /** Tabla de órdenes con filtros opcionales por estado, paciente (nombre o DPI) y médico. */
    List<LabOrder> search(LabOrderStatus status, String patient, String doctor);

    LabOrder getOrder(Integer orderId);

    /** Pasos 9-10. Devuelve la orden para que el controlador pueda avisar si quedó fuera de rango. */
    LabOrder saveResult(Integer orderId, Integer itemId, LabResultForm form, Integer userId);

    /** Pasos 11-12 (publicación individual). Si todos los exámenes quedan publicados, la orden pasa a Completada. */
    LabOrder publish(Integer orderId, Integer itemId, Integer userId);
}
