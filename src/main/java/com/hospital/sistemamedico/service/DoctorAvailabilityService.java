package com.hospital.sistemamedico.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DoctorAvailabilityService {

    List<LocalTime> getAvailableSlots(Integer doctorId, LocalDate date);
}