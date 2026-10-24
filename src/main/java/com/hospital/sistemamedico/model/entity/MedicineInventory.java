package com.hospital.sistemamedico.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Existencias de un medicamento en una sucursal. @Version = control de concurrencia optimista (RNF-025). */
@Entity
@Table(name = "medicine_inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"medicine_id", "branch_id"})
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class MedicineInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;
}
