package com.hospital.sistemamedico.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * RN-GLOBAL-001: el DPI debe ser obligatorio, de exactamente 13 dígitos numéricos.
 */
@Documented
@Constraint(validatedBy = DpiValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Dpi {

    String message() default "El DPI debe contener exactamente 13 dígitos numéricos.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}