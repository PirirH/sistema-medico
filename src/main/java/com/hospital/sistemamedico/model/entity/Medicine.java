package com.hospital.sistemamedico.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "medicine")
@Data
@NoArgsConstructor
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "default_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultPrice;

    @Column(nullable = false, length = 50)
    private String unit;

    /** RNF-017: los medicamentos controlados requieren autorización adicional al despachar. */
    @Column(nullable = false)
    private boolean controlled = false;

    /** RN-CU10-03: nivel mínimo de stock para la alerta de reorden (opcional). */
    @Column(name = "minimum_stock")
    private Integer minimumStock;

    @Column(nullable = false)
    private boolean active = true;
}
