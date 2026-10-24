package com.hospital.sistemamedico.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * RN-CU04-03: la fecha de vencimiento debe venir en formato MM/AA
 * y no debe estar vencida respecto a la fecha actual.
 */
@Documented
@Constraint(validatedBy = CardExpiryValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CardExpiry {

    String message() default "La fecha de vencimiento debe estar en formato MM/AA y la tarjeta no debe estar vencida.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}