package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.BranchSpecialtyForm;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.BranchSpecialtyAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CU-13 Configuración de Sedes y Especialidades. */
@Controller
@RequestMapping("/admin/sedes-especialidades")
@RequiredArgsConstructor
public class BranchSpecialtyAdminController {

    private final BranchSpecialtyAdminService service;

    @GetMapping
    public String list(@RequestParam(required = false) Integer id, Model model) {
        model.addAttribute("items", service.list(id));
        model.addAttribute("idFilter", id);
        return "admin/sedes-lista";
    }

    @GetMapping("/nuevo")
    public String showForm(Model model) {
        fill(model, new BranchSpecialtyForm());
        return "admin/sedes-form";
    }

    @PostMapping("/nuevo")
    public String assign(@ModelAttribute("form") BranchSpecialtyForm form,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes ra) {
        try {
            service.assign(form, principal.getId());
            ra.addFlashAttribute("flashMessage", "Especialidad asignada a la sede correctamente");
            return "redirect:/admin/sedes-especialidades";
        } catch (BusinessRuleException ex) {
            fill(model, form);
            model.addAttribute("formError", ex.getMessage());
            return "admin/sedes-form";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String remove(@PathVariable Integer id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes ra) {
        try {
            ra.addFlashAttribute("flashMessage", service.remove(id, principal.getId()));
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/sedes-especialidades";
    }

    private void fill(Model model, BranchSpecialtyForm form) {
        model.addAttribute("form", form);
        model.addAttribute("branches", service.activeBranches());
        model.addAttribute("specialties", service.activeSpecialties());
    }
}
