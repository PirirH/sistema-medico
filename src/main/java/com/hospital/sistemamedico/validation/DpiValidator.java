package com.hospital.sistemamedico.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DpiValidator implements ConstraintValidator<Dpi, String> {

    private static final String DPI_PATTERN = "\\d{13}";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false; // el DPI es obligatorio
        }
        return value.matches(DPI_PATTERN);
    }
}