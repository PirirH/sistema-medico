package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.enums.ConsultationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Formulario de consulta médica (CU-08). Las reglas de cierre (RN-CU08-01/02) dependen del
 * estado elegido, por eso se validan en el servicio y aquí solo se limitan longitudes máximas.
 */
@Data
public class ConsultationForm {

    @Size(max = 2000, message = "El motivo de visita no puede exceder 2000 caracteres.")
    private String visitReason;

    @Size(max = 5000, message = "Los hallazgos clínicos no pueden exceder 5000 caracteres.")
    private String clinicalFindings;

    @Size(max = 10, message = "El código CIE-10 no puede exceder 10 caracteres.")
    private String cie10Code;

    @Size(max = 5000, message = "El diagnóstico no puede exceder 5000 caracteres.")
    private String diagnosis;

    @Size(max = 5000, message = "El plan de tratamiento no puede exceder 5000 caracteres.")
    private String treatmentPlan;

    @Size(max = 2000, message = "Las notas adicionales no pueden exceder 2000 caracteres.")
    private String notes;

    @NotNull(message = "Debe seleccionar el estado de la consulta.")
    private ConsultationStatus status = ConsultationStatus.EN_CURSO;
}
