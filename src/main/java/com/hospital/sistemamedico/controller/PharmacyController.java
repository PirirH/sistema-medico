package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.DispenseForm;
import com.hospital.sistemamedico.model.dto.DispensePreview;
import com.hospital.sistemamedico.model.dto.DispensationSummary;
import com.hospital.sistemamedico.model.entity.Dispensation;
import com.hospital.sistemamedico.model.entity.DispensationItem;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** CU-11: Despacho de medicamentos. */
@Controller
@RequestMapping("/admin/farmacia/despachos")
@RequiredArgsConstructor
public class PharmacyController {

    private static final String SEARCH_VIEW = "farmacia/despacho-buscar";
    private static final String DETAIL_VIEW = "farmacia/despacho-detalle";

    private final PharmacyService pharmacyService;

    @GetMapping("/nuevo")
    public String showSearch() {
        return SEARCH_VIEW;
    }

    @PostMapping("/buscar")
    public String search(@RequestParam(required = false) String receta,
                         @RequestParam(required = false) String consulta,
                         Model model) {
        model.addAttribute("receta", receta);
        model.addAttribute("consulta", consulta);
        try {
            model.addAttribute("results", pharmacyService.search(receta, consulta));
        } catch (BusinessRuleException ex) {
            model.addAttribute("searchError", ex.getMessage());
        }
        return SEARCH_VIEW;
    }

    @GetMapping("/receta/{id}")
    public String detail(@PathVariable Integer id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes ra) {
        try {
            DispensePreview preview = pharmacyService.prepare(id, principal.getId());
            fillDetail(model, preview);
            return DETAIL_VIEW;
        } catch (BusinessRuleException ex) {
            // Receta vencida / inactiva / sin sucursal: se vuelve a la búsqueda con el mensaje.
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/farmacia/despachos/nuevo";
        }
    }

    @PostMapping("/receta/{id}/confirmar")
    public String confirm(@PathVariable Integer id,
                          @ModelAttribute("dispenseForm") DispenseForm form,
                          @AuthenticationPrincipal AppUserPrincipal principal,
                          Model model, RedirectAttributes ra) {
        try {
            Dispensation dispensation = pharmacyService.confirm(id, principal.getId(), form);
            return "redirect:/admin/farmacia/despachos/" + dispensation.getId() + "/resumen";
        } catch (BusinessRuleException ex) {
            try {
                fillDetail(model, pharmacyService.prepare(id, principal.getId()));
            } catch (BusinessRuleException inner) {
                ra.addFlashAttribute("flashError", inner.getMessage());
                return "redirect:/admin/farmacia/despachos/nuevo";
            }
            model.addAttribute("formError", ex.getMessage());
            return DETAIL_VIEW;
        }
    }

    /** FA03: el paciente no desea adquirir los medicamentos. */
    @PostMapping("/receta/{id}/cancelar")
    public String cancel(@PathVariable Integer id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes ra) {
        try {
            ra.addFlashAttribute("flashMessage", pharmacyService.markNotPurchased(id, principal.getId()));
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/farmacia/despachos/nuevo";
    }

    @GetMapping("/{id}/resumen")
    public String summary(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            DispensationSummary summary = pharmacyService.getSummary(id);
            Dispensation d = summary.getDispensation();
            int count = d.getItems().size();
            BigDecimal total = d.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
            model.addAttribute("dispensation", d);
            model.addAttribute("alerts", summary.getAlerts());
            model.addAttribute("successMessage",
                    "Despacho registrado exitosamente. %d medicamento(s) despachado(s). Total: Q%s."
                            .formatted(count, total.toPlainString()));
            long substitutions = d.getItems().stream().filter(DispensationItem::isSubstituted).count();
            model.addAttribute("substitutions", substitutions);
            return "farmacia/despacho-resumen";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/farmacia/despachos/nuevo";
        }
    }

    private void fillDetail(Model model, DispensePreview preview) {
        model.addAttribute("preview", preview);
        if (!model.containsAttribute("dispenseForm")) {
            model.addAttribute("dispenseForm", new DispenseForm());
        }
    }
}
