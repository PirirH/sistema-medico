package com.hospital.sistemamedico.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * RN-CU04-01: el número de tarjeta debe tener entre 13 y 19 dígitos
 * y ser válido según el algoritmo de Luhn.
 */
@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CardNumber {

    String message() default "El número de tarjeta debe contener entre 13 y 19 dígitos y ser válido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}