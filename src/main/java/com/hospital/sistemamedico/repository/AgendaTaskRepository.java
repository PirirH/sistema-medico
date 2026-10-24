package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.AgendaTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgendaTaskRepository extends JpaRepository<AgendaTask, Integer> {
    List<AgendaTask> findByDoctorIdOrderByDueDateAscIdAsc(Integer doctorId);
}
