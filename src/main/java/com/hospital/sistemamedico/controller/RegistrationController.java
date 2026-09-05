package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.PatientRegistrationForm;
import com.hospital.sistemamedico.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final PatientService patientService;

    @GetMapping("/registro")
    public String showForm(@RequestParam(value = "dpi", required = false) String dpi, Model model) {
        PatientRegistrationForm form = new PatientRegistrationForm();
        if (dpi != null) {
            form.setDpi(dpi);
        }
        model.addAttribute("registrationForm", form);
        return "register";
    }

    @PostMapping("/registro")
    public String submit(@Valid @ModelAttribute("registrationForm") PatientRegistrationForm form,
                         BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            patientService.register(form);
        } catch (BusinessRuleException ex) {
            model.addAttribute("registrationError", ex.getMessage());
            return "register";
        }

        model.addAttribute("registrationSuccess", true);
        return "login";
    }
}