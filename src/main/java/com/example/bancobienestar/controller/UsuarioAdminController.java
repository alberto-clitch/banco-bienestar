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
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private final BancaService bancaService;

    public UsuarioAdminController(BancaService bancaService) {
        this.bancaService = bancaService;
    }

    @GetMapping
    public String listarUsuarios(Model modelo) {
        List<Map<String, Object>> usuarios = bancaService.obtenerTodosUsuarios();
        
        long clientesCount = usuarios.stream()
                .filter(u -> "CLIENTE".equals(u.get("rol")))
                .count();
        long ejecutivosCount = usuarios.stream()
                .filter(u -> "EJECUTIVO".equals(u.get("rol")))
                .count();
        
        modelo.addAttribute("usuarios", usuarios);
        modelo.addAttribute("clientesCount", clientesCount);
        modelo.addAttribute("ejecutivosCount", ejecutivosCount);
        return "usuarios";
    }

    @PostMapping("/actualizar")
    public String actualizarUsuario(
            @RequestParam("id") Long id,
            @RequestParam("nombre") String nombre,
            @RequestParam("username") String username,
            @RequestParam(value = "password", required = false) String password,
            RedirectAttributes redirectAttributes) {
        
        try {
            bancaService.actualizarUsuario(id, nombre, username, password);
            redirectAttributes.addFlashAttribute("exito", "Usuario actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/eliminar")
    public String eliminarUsuario(
            @RequestParam("id") Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            bancaService.eliminarUsuario(id);
            redirectAttributes.addFlashAttribute("exito", "Usuario eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        
        return "redirect:/admin/usuarios";
    }
}
