package com.hospital.sistemamedico.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Recursos que no existen (por ejemplo /favicon.ico): respuesta 404 silenciosa, sin ensuciar el log. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRule(BusinessRuleException ex, Model model) {
        log.warn("BusinessRuleException no capturada localmente: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /** Dos usuarios modificaron el mismo registro a la vez (@Version): gana el primero que guardó. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(ObjectOptimisticLockingFailureException ex, Model model) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        model.addAttribute("errorMessage",
                "Operación no permitida: el registro fue actualizado por otro usuario mientras lo editaba. "
                        + "Verifique el estado actual e intente nuevamente.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception ex, Model model) {
        log.error("Error inesperado no controlado", ex);
        model.addAttribute("errorMessage", "Ocurrió un error inesperado. Por favor, intente nuevamente.");
        return "error";
    }
}
