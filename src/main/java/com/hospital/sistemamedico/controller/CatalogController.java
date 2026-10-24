package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.CatalogForm;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** CU-14 Mantenimiento de Catálogos del Sistema. */
@Controller
@RequestMapping("/admin/catalogos")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public String menu(Model model) {
        model.addAttribute("catalogs", CatalogService.allMetas());
        return "admin/catalogos-menu";
    }

    @GetMapping("/{tipo}")
    public String list(@PathVariable String tipo,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("meta", catalogService.meta(tipo));
            model.addAttribute("result", catalogService.list(tipo, q, page, size));
            model.addAttribute("q", q);
            model.addAttribute("size", size);
            return "admin/catalogos-lista";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/catalogos";
        }
    }

    @GetMapping("/{tipo}/nuevo")
    public String showCreate(@PathVariable String tipo, Model model, RedirectAttributes ra) {
        try {
            fill(model, tipo, new CatalogForm(), null, Map.of());
            return "admin/catalogos-form";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/catalogos";
        }
    }

    @PostMapping("/{tipo}/nuevo")
    public String create(@PathVariable String tipo,
                         @ModelAttribute("form") CatalogForm form,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes ra) {
        return save(tipo, null, form, principal, model, ra, "creado exitosamente.");
    }

    @GetMapping("/{tipo}/{id}/editar")
    public String showEdit(@PathVariable String tipo, @PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            fill(model, tipo, catalogService.loadForm(tipo, id), id, Map.of());
            return "admin/catalogos-form";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/catalogos";
        }
    }

    @PostMapping("/{tipo}/{id}/editar")
    public String update(@PathVariable String tipo, @PathVariable Integer id,
                         @ModelAttribute("form") CatalogForm form,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes ra) {
        return save(tipo, id, form, principal, model, ra, "actualizado correctamente.");
    }

    @PostMapping("/{tipo}/{id}/eliminar")
    public String delete(@PathVariable String tipo, @PathVariable Integer id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes ra) {
        try {
            String name = catalogService.delete(tipo, id, principal.getId());
            ra.addFlashAttribute("flashMessage", "El registro " + name + " ha sido eliminado correctamente.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/catalogos/" + tipo;
    }

    private String save(String tipo, Integer id, CatalogForm form, AppUserPrincipal principal,
                        Model model, RedirectAttributes ra, String verb) {
        try {
            Map<String, String> errors = catalogService.validate(tipo, form, id);
            if (!errors.isEmpty()) {
                fill(model, tipo, form, id, errors);
                return "admin/catalogos-form";
            }
            String name = catalogService.save(tipo, id, form, principal.getId());
            ra.addFlashAttribute("flashMessage", "El registro " + name + " ha sido " + verb);
            return "redirect:/admin/catalogos/" + tipo;
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/catalogos";
        }
    }

    private void fill(Model model, String tipo, CatalogForm form, Integer editingId, Map<String, String> errors) {
        model.addAttribute("meta", catalogService.meta(tipo));
        model.addAttribute("form", form);
        model.addAttribute("editingId", editingId);
        model.addAttribute("errors", errors);
        model.addAllAttributes(catalogService.lookups(tipo));
    }
}
