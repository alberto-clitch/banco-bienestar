package com.example.bancobienestar.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bancobienestar.service.BancaService;

@Controller
@RequestMapping("/admin/movimientos")
public class MovimientoAdminController {

    private final BancaService bancaService;

    public MovimientoAdminController(BancaService bancaService) {
        this.bancaService = bancaService;
    }

    @GetMapping
    public String listaMovimientos(Model modelo) {
        List<Map<String, Object>> movimientos = bancaService.obtenerTodosMovimientos();
        modelo.addAttribute("movimientos", movimientos);
        return "movimientos";
    }

    @PostMapping("/autorizar")
    public String autorizarMovimiento(@RequestParam("movimientoId") Long movimientoId,
                                      RedirectAttributes redirectAttributes) {
        try {
            bancaService.autorizarMovimiento(movimientoId);
            redirectAttributes.addFlashAttribute("exito", "Movimiento autorizado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al autorizar: " + e.getMessage());
        }
        return "redirect:/admin/movimientos";
    }

    @PostMapping("/cancelar")
    public String cancelarMovimiento(@RequestParam("movimientoId") Long movimientoId,
                                     RedirectAttributes redirectAttributes) {
        try {
            bancaService.cancelarMovimiento(movimientoId);
            redirectAttributes.addFlashAttribute("exito", "Movimiento cancelado y fondos revertidos correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cancelar: " + e.getMessage());
        }
        return "redirect:/admin/movimientos";
    }

    @PostMapping("/eliminar")
    public String eliminarMovimiento(@RequestParam("movimientoId") Long movimientoId,
                                     RedirectAttributes redirectAttributes) {
        try {
            bancaService.eliminarMovimiento(movimientoId);
            redirectAttributes.addFlashAttribute("exito", "Movimiento eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/admin/movimientos";
    }
}