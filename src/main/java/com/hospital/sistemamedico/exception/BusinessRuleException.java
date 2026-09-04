package com.hospital.sistemamedico.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio (RN-*) que no
 * puede validarse solo con Bean Validation (ej. unicidad de username/DPI/email).
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}