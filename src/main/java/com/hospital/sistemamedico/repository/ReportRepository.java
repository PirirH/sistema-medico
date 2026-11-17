package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.model.enums.ConsultationStatus;
import com.hospital.sistemamedico.model.enums.DispensationStatus;
import com.hospital.sistemamedico.model.enums.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de agregación para el módulo de reportes (solo lectura).
 *
 * <p>Se escribe con {@link EntityManager} y JPQL porque cada reporte agrupa y suma, y el filtro
 * de sucursal es opcional: cuando es null el filtro NO se agrega a la consulta (evita el problema
 * de PostgreSQL con parámetros null sin tipo). Todas las fechas son [desde, hasta): {@code hasta}
 * ya viene como el inicio del día siguiente.
 */
@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager em;

    /** Médicos con más consultas finalizadas. Fila: [id, nombre, especialidad, total]. */
    @SuppressWarnings("unchecked")
    public List<Object[]> topDoctorsByConsultations(LocalDateTime from, LocalDateTime to,
                                                    Integer branchId, int limit) {
        String jpql = """
                SELECT d.id, d.fullName, s.name, COUNT(c)
                FROM Consultation c
                JOIN c.appointment a
                JOIN a.doctor d
                JOIN a.branchSpecialty bs
                JOIN bs.specialty s
                WHERE c.status = :status
                  AND c.startedAt >= :from AND c.startedAt < :to
                """
                + (branchId != null ? " AND bs.branch.id = :branchId " : "")
                + """
                GROUP BY d.id, d.fullName, s.name
                ORDER BY COUNT(c) DESC, d.fullName ASC
                """;
        Query q = em.createQuery(jpql)
                .setParameter("status", ConsultationStatus.FINALIZADA)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(limit);
        if (branchId != null) q.setParameter("branchId", branchId);
        return q.getResultList();
    }

    /**
     * Fechas y horas de las citas que representan demanda real (se excluyen las pendientes de pago
     * y las canceladas). La agrupación por día de la semana se hace en Java para no depender de
     * funciones de fecha propias de PostgreSQL.
     */
    @SuppressWarnings("unchecked")
    public List<LocalDateTime> appointmentDates(LocalDateTime from, LocalDateTime to, Integer branchId) {
        String jpql = """
                SELECT a.scheduledAt
                FROM Appointment a
                JOIN a.branchSpecialty bs
                WHERE a.status NOT IN (:excluded)
                  AND a.scheduledAt >= :from AND a.scheduledAt < :to
                """
                + (branchId != null ? " AND bs.branch.id = :branchId " : "");
        Query q = em.createQuery(jpql)
                .setParameter("excluded", List.of(AppointmentStatus.PENDIENTE_PAGO, AppointmentStatus.CANCELADA))
                .setParameter("from", from)
                .setParameter("to", to);
        if (branchId != null) q.setParameter("branchId", branchId);
        return q.getResultList();
    }

    /** Medicamentos más despachados. Fila: [id, nombre, unidades, número de despachos]. */
    @SuppressWarnings("unchecked")
    public List<Object[]> topDispensedMedicines(LocalDateTime from, LocalDateTime to,
                                                Integer branchId, int limit) {
        String jpql = """
                SELECT m.id, m.name, SUM(i.quantity), COUNT(DISTINCT d.id)
                FROM DispensationItem i
                JOIN i.dispensation d
                JOIN i.medicine m
                WHERE d.status = :status
                  AND d.createdAt >= :from AND d.createdAt < :to
                """
                + (branchId != null ? " AND d.branch.id = :branchId " : "")
                + """
                GROUP BY m.id, m.name
                ORDER BY SUM(i.quantity) DESC, m.name ASC
                """;
        Query q = em.createQuery(jpql)
                .setParameter("status", DispensationStatus.DESPACHADO)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(limit);
        if (branchId != null) q.setParameter("branchId", branchId);
        return q.getResultList();
    }

    /** Pagos de citas aprobados por forma de pago y marca. Fila: [método, marca, cantidad, monto]. */
    @SuppressWarnings("unchecked")
    public List<Object[]> appointmentPaymentsByMethod(LocalDateTime from, LocalDateTime to, Integer branchId) {
        String jpql = """
                SELECT p.method, p.cardBrand, COUNT(p), SUM(p.amount)
                FROM Payment p
                WHERE p.status = :status
                  AND p.paidAt >= :from AND p.paidAt < :to
                """
                + (branchId != null ? " AND p.appointment.branchSpecialty.branch.id = :branchId " : "")
                + " GROUP BY p.method, p.cardBrand";
        Query q = em.createQuery(jpql)
                .setParameter("status", PaymentStatus.APROBADO)
                .setParameter("from", from)
                .setParameter("to", to);
        if (branchId != null) q.setParameter("branchId", branchId);
        return q.getResultList();
    }

    /** Cobros de laboratorio aprobados por forma de pago y marca. Misma forma de fila que arriba. */
    @SuppressWarnings("unchecked")
    public List<Object[]> labPaymentsByMethod(LocalDateTime from, LocalDateTime to, Integer branchId) {
        String jpql = """
                SELECT p.method, p.cardBrand, COUNT(p), SUM(p.amount)
                FROM LabPayment p
                WHERE p.status = :status
                  AND p.paidAt >= :from AND p.paidAt < :to
                """
                + (branchId != null ? " AND p.labOrder.branch.id = :branchId " : "")
                + " GROUP BY p.method, p.cardBrand";
        Query q = em.createQuery(jpql)
                .setParameter("status", PaymentStatus.APROBADO)
                .setParameter("from", from)
                .setParameter("to", to);
        if (branchId != null) q.setParameter("branchId", branchId);
        return q.getResultList();
    }
}
