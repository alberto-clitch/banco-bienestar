package com.example.bancobienestar.controller;

import java.security.Principal;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bancobienestar.service.BancaService;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final BancaService bancaService;

    public PerfilController(BancaService bancaService) {
        this.bancaService = bancaService;
    }

    @GetMapping
    public String mostrarPerfil(Model modelo, Principal principal) {
        String username = principal.getName();
        Map<String, Object> perfil = bancaService.obtenerPerfil(username);
        modelo.addAttribute("perfil", perfil);
        return "perfil";
    }

    @PostMapping("/actualizar")
    public String actualizarPerfil(
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "passwordActual", required = false) String passwordActual,
            @RequestParam(value = "passwordNueva", required = false) String passwordNueva,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            String username = principal.getName();
            bancaService.actualizarPerfil(username, nombre, passwordActual, passwordNueva);
            redirectAttributes.addFlashAttribute("exito", "Perfil actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        
        return "redirect:/perfil";
    }

    @PostMapping("/subir-foto")
    public String subirFoto(
            @RequestParam("foto") MultipartFile foto,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (foto.isEmpty()) {
                throw new RuntimeException("No se seleccionó ningún archivo.");
            }
            
            // Validar tipo de archivo
            String contentType = foto.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Solo se permiten archivos de imagen (JPG, PNG, GIF, WEBP).");
            }
            
            // Validar tamaño máximo (5MB)
            if (foto.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("La imagen no puede superar los 5MB.");
            }
            
            // Convertir a Base64 con prefijo data:image/...
            byte[] bytes = foto.getBytes();
            String base64 = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            
            String username = principal.getName();
            bancaService.actualizarFotoPerfil(username, base64);
            redirectAttributes.addFlashAttribute("exito", "Foto de perfil actualizada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al subir foto: " + e.getMessage());
        }
        
        return "redirect:/perfil";
    }
}
