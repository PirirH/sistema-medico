package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.MedicineInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MedicineInventoryRepository extends JpaRepository<MedicineInventory, Integer> {

    Optional<MedicineInventory> findByMedicine_IdAndBranch_Id(Integer medicineId, Integer branchId);

    List<MedicineInventory> findByBranch_IdAndMedicine_IdIn(Integer branchId, Collection<Integer> medicineIds);
}
