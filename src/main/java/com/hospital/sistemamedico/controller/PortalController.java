package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.model.dto.DpiCheckForm;
import com.hospital.sistemamedico.service.PortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PortalController {

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