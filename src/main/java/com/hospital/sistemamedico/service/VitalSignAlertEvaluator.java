package com.hospital.sistemamedico.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** RN-CU07-06: rangos clínicos normales para generar alertas (informativas, no bloquean el registro). */
public final class VitalSignAlertEvaluator {

    private VitalSignAlertEvaluator() {
    }

    public static List<String> evaluate(int systolic, int diastolic, BigDecimal temperature, int heartRate) {
        List<String> alerts = new ArrayList<>();

        if (systolic < 90 || systolic > 140 || diastolic < 60 || diastolic > 90) {
            alerts.add("Presión arterial fuera de rango normal.");
        }
        if (temperature.compareTo(new BigDecimal("36.0")) < 0 || temperature.compareTo(new BigDecimal("37.5")) > 0) {
            alerts.add("Temperatura fuera de rango normal.");
        }
        if (heartRate < 60 || heartRate > 100) {
            alerts.add("Frecuencia cardíaca fuera de rango normal.");
        }
        return alerts;
    }
}
