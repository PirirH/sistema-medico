package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.InventoryMovementForm;
import com.hospital.sistemamedico.model.dto.InventoryPanel;
import com.hospital.sistemamedico.model.dto.InventorySearchFilter;
import com.hospital.sistemamedico.model.entity.InventoryMovement;
import com.hospital.sistemamedico.model.entity.Medicine;
import com.hospital.sistemamedico.model.enums.MovementType;
import com.hospital.sistemamedico.repository.BranchRepository;
import com.hospital.sistemamedico.repository.MedicineRepository;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/** CU-15: Bitácora de movimientos de inventario. */
@Controller
@RequestMapping("/admin/farmacia/inventario")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final MedicineRepository medicineRepository;
    private final BranchRepository branchRepository;

    @GetMapping
    public String list(@ModelAttribute("filter") InventorySearchFilter filter, Model model) {
        model.addAttribute("movements", inventoryService.search(filter));
        fillCatalogs(model, true);
        return "farmacia/inventario-lista";
    }

    @GetMapping("/nuevo")
    public String showForm(Model model) {
        model.addAttribute("movementForm", new InventoryMovementForm());
        fillCatalogs(model, false);
        return "farmacia/inventario-form";
    }

    @PostMapping("/nuevo")
    public String register(@ModelAttribute("movementForm") InventoryMovementForm form,
                           @AuthenticationPrincipal AppUserPrincipal principal,
                           Model model, RedirectAttributes ra) {
        try {
            InventoryMovement m = inventoryService.register(form, principal.getId());
            ra.addFlashAttribute("flashMessage",
                    "Movimiento registrado exitosamente. Medicamento: %s. Tipo: %s. Cantidad: %d. Stock actualizado: %d."
                            .formatted(m.getMedicine().getName(), m.getMovementType().getLabel(),
                                    m.getQuantity(), m.getNewStock()));
            Integer min = m.getMedicine().getMinimumStock();
            if (min != null && m.getNewStock() <= min) { // RN-CU10-03 / FA05
                ra.addFlashAttribute("flashWarning",
                        "%s: Stock bajo — disponible: %d (mínimo: %d)."
                                .formatted(m.getMedicine().getName(), m.getNewStock(), min));
            }
            return "redirect:/admin/farmacia/inventario";
        } catch (BusinessRuleException ex) {
            model.addAttribute("formError", ex.getMessage());
            fillCatalogs(model, false);
            return "farmacia/inventario-form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("movement", inventoryService.get(id));
            return "farmacia/inventario-detalle";
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/farmacia/inventario";
        }
    }

    @PostMapping("/{id}/estado")
    public String toggle(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            InventoryMovement m = inventoryService.toggleActive(id);
            ra.addFlashAttribute("flashMessage",
                    m.isActive() ? "Movimiento activado." : "Movimiento desactivado.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/farmacia/inventario";
    }

    /** Panel informativo (stock actual / mínimo) para el formulario. */
    @GetMapping("/api/panel")
    @ResponseBody
    public InventoryPanel panel(@RequestParam Integer medicineId, @RequestParam Integer branchId) {
        return inventoryService.panel(medicineId, branchId);
    }

    private void fillCatalogs(Model model, boolean includeAutomatic) {
        List<MovementType> types = Arrays.stream(MovementType.values())
                .filter(t -> includeAutomatic || t.isManual())
                .toList();
        List<Medicine> medicines = medicineRepository.findByActiveTrueOrderByNameAsc();
        model.addAttribute("types", types);
        model.addAttribute("medicines", medicines);
        model.addAttribute("branches", branchRepository.findByActiveTrue());
    }
}
