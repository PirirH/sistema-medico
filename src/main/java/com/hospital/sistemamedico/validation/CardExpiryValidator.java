package com.hospital.sistemamedico.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.YearMonth;

public class CardExpiryValidator implements ConstraintValidator<CardExpiry, String> {

    private static final String EXPIRY_PATTERN = "(0[1-9]|1[0-2])/\\d{2}";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || !value.matches(EXPIRY_PATTERN)) {
            return false;
        }

        int month = Integer.parseInt(value.substring(0, 2));
        int year = 2000 + Integer.parseInt(value.substring(3, 5));

        YearMonth expiry = YearMonth.of(year, month);
        return !expiry.isBefore(YearMonth.now());
    }
}