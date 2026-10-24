package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.UserForm;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** CU-01 Mantenimiento de Usuarios. */
@Controller
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "nombre") String campo,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("campo", campo);
        model.addAttribute("q", q);
        model.addAttribute("size", size);
        try {
            model.addAttribute("result", userService.search(campo, q, page, size));
        } catch (BusinessRuleException ex) {
            model.addAttribute("searchError", ex.getMessage());
            model.addAttribute("result", userService.search(campo, null, 0, size));
        }
        return "admin/usuarios-lista";
    }

    @GetMapping("/nuevo")
    public String showCreate(Model model) {
        fill(model, new UserForm(), null, Map.of());
        return "admin/usuarios-form";
    }

    @PostMapping("/nuevo")
    public String create(@ModelAttribute("userForm") UserForm form,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes ra) {
        Map<String, String> errors = userService.validate(form, null);
        if (!errors.isEmpty()) {
            fill(model, form, null, errors);
            return "admin/usuarios-form";
        }
        userService.create(form, principal.getId());
        ra.addFlashAttribute("flashMessage", "Usuario creado correctamente.");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/{id}/editar")
    public String showEdit(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            fill(model, userService.loadForm(id), id, Map.of());
            return "admin/usuarios-form";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/{id}/editar")
    public String update(@PathVariable Integer id,
                         @ModelAttribute("userForm") UserForm form,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes ra) {
        Map<String, String> errors = userService.validate(form, id);
        if (!errors.isEmpty()) {
            fill(model, form, id, errors);
            return "admin/usuarios-form";
        }
        try {
            userService.update(id, form, principal.getId());
            ra.addFlashAttribute("flashMessage", "Usuario actualizado correctamente.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable Integer id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes ra) {
        try {
            String username = userService.delete(id, principal.getId());
            ra.addFlashAttribute("flashMessage", "El usuario " + username + " ha sido eliminado correctamente.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    private void fill(Model model, UserForm form, Integer editingId, Map<String, String> errors) {
        model.addAttribute("userForm", form);
        model.addAttribute("editingId", editingId);
        model.addAttribute("errors", errors);
        model.addAttribute("roles", userService.activeRoles());
        model.addAttribute("branches", userService.activeBranches());
        model.addAttribute("specialties", userService.activeSpecialties());
    }
}
