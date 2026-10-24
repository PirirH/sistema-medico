package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.LabResultForm;
import com.hospital.sistemamedico.model.entity.InternalUser;
import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.entity.LabOrderItem;
import com.hospital.sistemamedico.model.enums.LabOrderStatus;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.LabOrderRepository;
import com.hospital.sistemamedico.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

    private final LabOrderRepository labOrderRepository;
    private final InternalUserRepository internalUserRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LabOrder> search(LabOrderStatus status, String patient, String doctor) {
        String patientLike = like(patient);
        String doctorLike = like(doctor);
        return status == null
                ? labOrderRepository.search(patientLike, doctorLike)
                : labOrderRepository.searchByStatus(status, patientLike, doctorLike);
    }

    @Override
    @Transactional(readOnly = true)
    public LabOrder getOrder(Integer orderId) {
        return labOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessRuleException("Orden de laboratorio no encontrada."));
    }

    @Override
    @Transactional
    public LabOrder saveResult(Integer orderId, Integer itemId, LabResultForm form, Integer userId) {
        LabOrder order = getOrder(orderId);
        LabOrderItem item = findItem(order, itemId);
        ensureProcessable(order);

        // RNF-024: un resultado publicado es inmutable.
        if (item.isPublished()) {
            throw new BusinessRuleException("El resultado ya fue publicado y no puede modificarse.");
        }

        String value = form.getResultValue() == null ? "" : form.getResultValue().trim();
        if (value.isEmpty() || value.length() > 200) {
            throw new BusinessRuleException("El valor del resultado es obligatorio (máximo 200 caracteres).");
        }
        String unit = form.getResultUnit() == null ? "" : form.getResultUnit().trim();
        if (unit.length() > 50) {
            throw new BusinessRuleException("La unidad no puede exceder 50 caracteres.");
        }
        String notes = form.getResultNotes() == null ? "" : form.getResultNotes().trim();
        if (notes.length() > 500) {
            throw new BusinessRuleException("Las notas del resultado no pueden exceder 500 caracteres.");
        }
        LocalDate date = form.getResultDate() == null ? LocalDate.now() : form.getResultDate();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha del resultado no puede ser futura.");
        }

        item.setResultValue(value);
        item.setResultUnit(unit.isEmpty() ? null : unit);
        item.setResultDate(date);
        item.setOutOfRange(form.isOutOfRange());
        item.setResultNotes(notes.isEmpty() ? null : notes);
        item.setResultedBy(user(userId));

        return labOrderRepository.save(order);
    }

    @Override
    @Transactional
    public LabOrder publish(Integer orderId, Integer itemId, Integer userId) {
        LabOrder order = getOrder(orderId);
        LabOrderItem item = findItem(order, itemId);
        ensureProcessable(order);

        if (item.isPublished()) {
            throw new BusinessRuleException("El resultado ya fue publicado.");
        }
        if (!item.hasResult()) {
            throw new BusinessRuleException("Debe guardar el resultado del examen antes de publicarlo.");
        }

        item.setPublished(true);
        item.setPublishedAt(LocalDateTime.now());
        item.setPublishedBy(user(userId));

        // Paso 14: con todos los exámenes publicados, la orden se considera completada.
        boolean allPublished = order.getItems().stream().allMatch(LabOrderItem::isPublished);
        if (allPublished) {
            order.setStatus(LabOrderStatus.COMPLETADA);
        }
        return labOrderRepository.save(order);
    }

    // ------------------------------------------------------------------ auxiliares

    private void ensureProcessable(LabOrder order) {
        if (order.isExternal()) {
            throw new BusinessRuleException("Las órdenes externas no se procesan en este laboratorio.");
        }
        if (order.getStatus() == LabOrderStatus.PENDIENTE) {
            // RN-CU09-01
            throw new BusinessRuleException("El pago debe estar completado antes de proceder con la toma de muestras.");
        }
        if (order.getStatus() == LabOrderStatus.COMPLETADA) {
            throw new BusinessRuleException("La orden ya fue completada; sus resultados no pueden modificarse.");
        }
    }

    private LabOrderItem findItem(LabOrder order, Integer itemId) {
        return order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("El examen no pertenece a esta orden."));
    }

    private InternalUser user(Integer userId) {
        return internalUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Usuario no encontrado."));
    }

    /** "" -> "%" (todo); "ana" -> "%ana%". Se compara en minúsculas. */
    private String like(String text) {
        return "%" + (text == null ? "" : text.trim().toLowerCase()) + "%";
    }
}
