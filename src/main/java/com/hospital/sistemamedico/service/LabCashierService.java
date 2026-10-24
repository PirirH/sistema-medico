package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.CashierPaymentForm;
import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.entity.LabPayment;

import java.util.List;

/** CU-10: Cobro de laboratorio en caja. */
public interface LabCashierService {

    /** Pasos 2-3 y FA01: órdenes pendientes de pago por DPI o por número de orden. */
    List<LabOrder> searchPending(String dpi, String orderNumber);

    /** Orden lista para cobrar (debe estar pendiente y no ser externa). */
    LabOrder getPendingOrder(Integer orderId);

    /** Pasos 6-9, FA03 y FA04. Si es rechazado devuelve el pago con estado RECHAZADO. */
    LabPayment charge(Integer orderId, CashierPaymentForm form, Integer cashierUserId);

    /** Pago aprobado de la orden (para el comprobante; se puede reimprimir sin límite, RNF-033). */
    LabPayment getApprovedPayment(Integer orderId);
}
