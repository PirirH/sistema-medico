package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.CashierPaymentForm;
import com.hospital.sistemamedico.model.entity.InternalUser;
import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.entity.LabPayment;
import com.hospital.sistemamedico.model.enums.LabOrderStatus;
import com.hospital.sistemamedico.model.enums.PaymentMethod;
import com.hospital.sistemamedico.model.enums.PaymentStatus;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.LabOrderRepository;
import com.hospital.sistemamedico.repository.LabPaymentRepository;
import com.hospital.sistemamedico.service.LabCashierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabCashierServiceImpl implements LabCashierService {

    private final LabOrderRepository labOrderRepository;
    private final LabPaymentRepository labPaymentRepository;
    private final InternalUserRepository internalUserRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LabOrder> searchPending(String dpi, String orderNumber) {
        boolean hasDpi = dpi != null && !dpi.isBlank();
        boolean hasNumber = orderNumber != null && !orderNumber.isBlank();

        if (!hasDpi && !hasNumber) {
            throw new BusinessRuleException("Debe ingresar un DPI o un número de orden para buscar.");
        }

        List<LabOrder> results;
        if (hasNumber) {
            results = labOrderRepository.findPendingByNumber(orderNumber.trim().toUpperCase(), LabOrderStatus.PENDIENTE);
        } else {
            String value = dpi.trim();
            if (!value.matches("\\d{13}")) { // RN-GLOBAL-001
                throw new BusinessRuleException(
                        "El DPI debe contener exactamente 13 dígitos numéricos. Usted ingresó " + value.length() + " caracteres.");
            }
            results = labOrderRepository.findPendingByDpi(value, LabOrderStatus.PENDIENTE);
        }

        if (results.isEmpty()) { // FA01
            throw new BusinessRuleException(
                    "No se encontraron órdenes de laboratorio pendientes de pago. Verifique el DPI o número de orden e intente de nuevo.");
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public LabOrder getPendingOrder(Integer orderId) {
        LabOrder order = load(orderId);
        if (order.getStatus() != LabOrderStatus.PENDIENTE || order.isExternal()) {
            throw new BusinessRuleException("Esta orden ya no está pendiente de pago.");
        }
        return order;
    }

    @Override
    @Transactional
    public LabPayment charge(Integer orderId, CashierPaymentForm form, Integer cashierUserId) {
        LabOrder order = load(orderId);
        if (order.getStatus() != LabOrderStatus.PENDIENTE || order.isExternal()) {
            throw new BusinessRuleException("Esta orden ya no está pendiente de pago.");
        }
        if (form.getMethod() == null) {
            throw new BusinessRuleException("Debe seleccionar una forma de pago.");
        }

        InternalUser cashier = internalUserRepository.findById(cashierUserId)
                .orElseThrow(() -> new BusinessRuleException("Usuario cajero no encontrado."));

        LabPayment payment = new LabPayment();
        payment.setTransactionNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        payment.setLabOrder(order);
        payment.setMethod(form.getMethod());
        payment.setAmount(order.getTotalAmount());
        payment.setProcessedBy(cashier);
        payment.setPaidAt(LocalDateTime.now());

        if (form.getMethod() == PaymentMethod.EFECTIVO) {
            if (form.getAmountReceived() == null) {
                throw new BusinessRuleException("Debe ingresar el monto recibido.");
            }
            if (form.getAmountReceived().compareTo(order.getTotalAmount()) < 0) {
                throw new BusinessRuleException(String.format(Locale.US,
                        "El monto recibido (Q%.2f) es menor al monto a cobrar (Q%.2f)",
                        form.getAmountReceived(), order.getTotalAmount()));
            }
            payment.setAmountReceived(form.getAmountReceived());
            payment.setChangeGiven(form.getAmountReceived().subtract(order.getTotalAmount()));
            payment.setStatus(PaymentStatus.APROBADO);
        } else {
            // FA03
            if (form.getCardLastFour() == null || !form.getCardLastFour().matches("\\d{4}")) {
                throw new BusinessRuleException("Ingrese los últimos 4 dígitos de la tarjeta.");
            }
            payment.setCardBrand(form.getCardBrand());
            payment.setCardLastFour(form.getCardLastFour());
            // Mock reproducible: terminada en 0000 = el banco rechaza (FA04).
            payment.setStatus("0000".equals(form.getCardLastFour()) ? PaymentStatus.RECHAZADO : PaymentStatus.APROBADO);
        }

        payment = labPaymentRepository.save(payment);
        payment.setTransactionNumber("LAB-" + Year.now().getValue() + "-" + String.format("%06d", payment.getId()));
        payment = labPaymentRepository.save(payment);

        if (payment.getStatus() == PaymentStatus.APROBADO) {
            order.setStatus(LabOrderStatus.EN_PROCESO); // la toma de muestras puede proceder (CU-09)
            labOrderRepository.save(order);
        }
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public LabPayment getApprovedPayment(Integer orderId) {
        return labPaymentRepository.findByLabOrder_IdOrderByCreatedAtDesc(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.APROBADO)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No se encontró un comprobante de pago para esta orden."));
    }

    private LabOrder load(Integer orderId) {
        return labOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessRuleException("Orden de laboratorio no encontrada."));
    }
}
