package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.AgendaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendaEventRepository extends JpaRepository<AgendaEvent, Integer> {

    /** Eventos del médico que se cruzan con el rango [from, to). */
    @Query("""
            SELECT e FROM AgendaEvent e
            WHERE e.doctorId = :doctorId AND e.startAt < :to AND e.endAt > :from
            ORDER BY e.startAt ASC
            """)
    List<AgendaEvent> findOverlapping(@Param("doctorId") Integer doctorId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);
}
