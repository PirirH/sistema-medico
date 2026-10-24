package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.enums.LabOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LabOrderRepository extends JpaRepository<LabOrder, Integer> {

    @Query("""
            SELECT DISTINCT o FROM LabOrder o
            JOIN FETCH o.patient
            JOIN FETCH o.doctor
            JOIN FETCH o.branch
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.exam
            WHERE o.id = :id
            """)
    Optional<LabOrder> findByIdWithDetails(@Param("id") Integer id);

    /** Tabla de órdenes del laboratorio (CU-09 paso 1). Los filtros llegan ya en minúsculas con %...%. */
    @Query("""
            SELECT o FROM LabOrder o
            JOIN FETCH o.patient p
            JOIN FETCH o.doctor d
            JOIN FETCH o.branch
            WHERE (LOWER(p.fullName) LIKE :patient OR p.dpi LIKE :patient)
              AND LOWER(d.fullName) LIKE :doctor
            ORDER BY o.createdAt DESC
            """)
    List<LabOrder> search(@Param("patient") String patient, @Param("doctor") String doctor);

    @Query("""
            SELECT o FROM LabOrder o
            JOIN FETCH o.patient p
            JOIN FETCH o.doctor d
            JOIN FETCH o.branch
            WHERE o.status = :status
              AND (LOWER(p.fullName) LIKE :patient OR p.dpi LIKE :patient)
              AND LOWER(d.fullName) LIKE :doctor
            ORDER BY o.createdAt DESC
            """)
    List<LabOrder> searchByStatus(@Param("status") LabOrderStatus status,
                                  @Param("patient") String patient, @Param("doctor") String doctor);

    /** Órdenes pendientes de pago por DPI (CU-10). Excluye las externas. */
    @Query("""
            SELECT DISTINCT o FROM LabOrder o
            JOIN FETCH o.patient p
            JOIN FETCH o.doctor
            JOIN FETCH o.branch
            LEFT JOIN FETCH o.items
            WHERE p.dpi = :dpi AND o.status = :status AND o.external = false
            ORDER BY o.createdAt ASC
            """)
    List<LabOrder> findPendingByDpi(@Param("dpi") String dpi, @Param("status") LabOrderStatus status);

    @Query("""
            SELECT DISTINCT o FROM LabOrder o
            JOIN FETCH o.patient
            JOIN FETCH o.doctor
            JOIN FETCH o.branch
            LEFT JOIN FETCH o.items
            WHERE o.orderNumber = :number AND o.status = :status AND o.external = false
            """)
    List<LabOrder> findPendingByNumber(@Param("number") String number, @Param("status") LabOrderStatus status);

    /** Todas las órdenes de un paciente con sus exámenes y resultados (vista del médico). */
    @Query("""
            SELECT DISTINCT o FROM LabOrder o
            JOIN FETCH o.doctor
            JOIN FETCH o.branch
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.exam
            WHERE o.patient.id = :patientId
            ORDER BY o.createdAt DESC
            """)
    List<LabOrder> findByPatientWithItems(@Param("patientId") Integer patientId);
}
