package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/paciente/dashboard")
    public String patientDashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("fullName", principal.getFullName());
        return "paciente/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("fullName", principal.getFullName());
        return "admin/dashboard";
    }
}