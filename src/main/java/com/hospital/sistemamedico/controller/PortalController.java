package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.model.dto.DpiCheckForm;
import com.hospital.sistemamedico.service.PortalService;
import jakarta.validation.Valid;  // validación automatica del DTO anotacones como @NotBlank, Pattern etc.
import lombok.RequiredArgsConstructor; // lombok genera en impo de compilación constructores con todos los campos final
import org.springframework.http.ResponseEntity; // respuestas http
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;  // para guardar que campos fallaron y con que mensaje
import org.springframework.web.bind.annotation.*; // para anotaciones web

import java.util.HashMap; // para armar respuestas JSON
import java.util.Map; // para armar respuestas JSON

@Controller// bean para peticiones web, en otras palabras es una clase de spring para peticiones web
@RequiredArgsConstructor // genera este constructor
public class PortalController { // constructor Portal Controller

    private final PortalService portalService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("branches", portalService.getActiveBranches());
        model.addAttribute("specialties", portalService.getActiveSpecialties());
        model.addAttribute("dpiCheckForm", new DpiCheckForm());
        return "index";
    }

    @PostMapping("/verificar-dpi")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarDpi(@Valid @RequestBody DpiCheckForm form,
                                                            BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        if (bindingResult.hasErrors()) {
            response.put("status", "INVALID_FORMAT");
            response.put("message", bindingResult.getFieldError("dpi").getDefaultMessage());
            return ResponseEntity.badRequest().body(response);
        }

        var result = portalService.checkDpi(form.getDpi());

        switch (result) {
            case REGISTERED_PATIENT -> {
                response.put("status", "REGISTERED");
                response.put("message", "Bienvenido(a). Será redirigido a la pantalla de inicio de sesión.");
                response.put("redirectUrl", "/login");
            }
            case NOT_REGISTERED -> {
                response.put("status", "NOT_REGISTERED");
                response.put("message", "No se encontró un registro asociado a este DPI. Será redirigido al formulario de registro.");
                response.put("redirectUrl", "/registro?dpi=" + form.getDpi());
            }
            case BELONGS_TO_INTERNAL_USER -> {
                response.put("status", "INTERNAL_USER");
                response.put("message", "Este DPI pertenece a un usuario del sistema interno. Por favor, contacte a recepción.");
            }
        }

        return ResponseEntity.ok(response);
    }
}