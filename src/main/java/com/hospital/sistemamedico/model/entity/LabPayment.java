package com.hospital.sistemamedico.model.entity;

import com.hospital.sistemamedico.model.enums.PaymentMethod;
import com.hospital.sistemamedico.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cobro de una orden de laboratorio en caja (CU-10). Bitácora: los rechazos también se guardan. */
@Entity
@Table(name = "lab_payment")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"labOrder", "processedBy"})
public class LabPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "transaction_number", nullable = false, unique = true, length = 30)
    private String transactionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "amount_received", precision = 10, scale = 2)
    private BigDecimal amountReceived;

    @Column(name = "change_given", precision = 10, scale = 2)
    private BigDecimal changeGiven;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private InternalUser processedBy;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
