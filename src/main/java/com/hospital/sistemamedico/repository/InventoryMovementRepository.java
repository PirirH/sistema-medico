package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

/** La búsqueda con filtros opcionales vive en InventoryServiceImpl (Criteria API). */
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Integer> {
}
