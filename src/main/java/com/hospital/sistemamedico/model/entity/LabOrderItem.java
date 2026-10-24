package com.hospital.sistemamedico.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un examen dentro de una orden, con su resultado (CU-09). */
@Entity
@Table(name = "lab_order_item")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"labOrder", "exam", "resultedBy", "publishedBy"})
public class LabOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private LabExam exam;

    /** Precio del examen "congelado" al crear la orden. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "result_value", length = 200)
    private String resultValue;

    @Column(name = "result_unit", length = 50)
    private String resultUnit;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Column(name = "out_of_range", nullable = false)
    private boolean outOfRange = false;

    @Column(name = "result_notes", length = 500)
    private String resultNotes;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resulted_by")
    private InternalUser resultedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private InternalUser publishedBy;

    public boolean hasResult() {
        return resultValue != null && !resultValue.isBlank();
    }
}
