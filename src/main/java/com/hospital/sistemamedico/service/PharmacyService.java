package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.DispenseForm;
import com.hospital.sistemamedico.model.dto.DispensePreview;
import com.hospital.sistemamedico.model.dto.DispensationSummary;
import com.hospital.sistemamedico.model.dto.PrescriptionRow;
import com.hospital.sistemamedico.model.entity.Dispensation;

import java.util.List;

/** CU-11: Despacho de medicamentos (personal de farmacia). */
public interface PharmacyService {

    /** Pasos 2-3: recetas activas por ID de receta (o número RX-...) y/o ID de consulta. */
    List<PrescriptionRow> search(String prescription, String consultation);

    /** Pasos 4-7: valida vigencia (RN-CU10-01) y arma el detalle con la disponibilidad en la sucursal. */
    DispensePreview prepare(Integer prescriptionId, Integer pharmacistUserId);

    /** Pasos 9-12: registra el despacho y descuenta inventario. */
    Dispensation confirm(Integer prescriptionId, Integer pharmacistUserId, DispenseForm form);

    /** FA03: el paciente no adquiere los medicamentos. Devuelve el mensaje de confirmación. */
    String markNotPurchased(Integer prescriptionId, Integer pharmacistUserId);

    /** Resumen del despacho con alertas de stock mínimo (FA04). */
    DispensationSummary getSummary(Integer dispensationId);
}
