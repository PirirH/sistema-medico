package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.service.DoctorAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {

    private static final Set<DayOfWeek> WORKING_DAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private final AppointmentRepository appointmentRepository;

    @Value("${hospital.schedule.start-time}")
    private LocalTime startTime;

    @Value("${hospital.schedule.end-time}")
    private LocalTime endTime;

    @Value("${hospital.schedule.slot-duration-minutes}")
    private int slotDurationMinutes;

    @Override
    public List<LocalTime> getAvailableSlots(Integer doctorId, LocalDate date) {
        if (!WORKING_DAYS.contains(date.getDayOfWeek())) {
            return List.of();
        }

        List<LocalTime> allSlots = generateDaySlots();

        LocalDateTime dayStart = date.atTime(startTime);
        LocalDateTime dayEnd = date.atTime(endTime);

        Set<LocalTime> occupied = appointmentRepository
                .findByDoctorIdAndScheduledAtBetween(doctorId, dayStart, dayEnd)
                .stream()
                .map(appointment -> appointment.getScheduledAt().toLocalTime())
                .collect(Collectors.toSet());

        boolean isToday = date.isEqual(LocalDate.now());
        LocalTime now = LocalTime.now();

        return allSlots.stream()
                .filter(slot -> !occupied.contains(slot))
                .filter(slot -> !isToday || slot.isAfter(now))
                .collect(Collectors.toList());
    }

    private List<LocalTime> generateDaySlots() {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = startTime;
        while (!cursor.plusMinutes(slotDurationMinutes).isAfter(endTime)) {
            slots.add(cursor);
            cursor = cursor.plusMinutes(slotDurationMinutes);
        }
        return slots;
    }
}