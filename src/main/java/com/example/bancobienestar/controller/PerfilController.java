package com.example.bancobienestar.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

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
                throw new RuntimeException("No se selecciono ningun archivo.");
            }
            
            // Validar tipo de archivo
            String contentType = foto.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Solo se permiten archivos de imagen.");
            }
            
            // Validar tamano maximo (5MB)
            if (foto.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("La imagen no puede superar los 5MB.");
            }
            
            // Leer los bytes originales
            byte[] bytesOriginales = foto.getBytes();
            
            // Intentar leer la imagen con ImageIO (soporta PNG, JPG, GIF, BMP, WBMP)
            BufferedImage imagenLeida = ImageIO.read(new ByteArrayInputStream(bytesOriginales));
            if (imagenLeida == null) {
                throw new RuntimeException(
                        "Formato de imagen no soportado. "
                        + "Usa PNG, JPG o GIF. Si tu foto es AVIF o HEIC, "
                        + "conviertela a PNG antes de subirla.");
            }
            
            // Convertir la imagen a PNG
            ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();
            ImageIO.write(imagenLeida, "png", pngBaos);
            byte[] bytesPng = pngBaos.toByteArray();
            
            // Codificar como Base64 con prefijo data:image/png
            String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytesPng);
            
            String username = principal.getName();
            bancaService.actualizarFotoPerfil(username, base64);
            
            String tamanoOriginal = String.format("%.1f", bytesOriginales.length / 1024.0);
            String tamanoPng = String.format("%.1f", bytesPng.length / 1024.0);
            redirectAttributes.addFlashAttribute("exito",
                    "Foto de perfil actualizada y convertida a PNG correctamente "
                    + "(" + tamanoOriginal + "KB -> " + tamanoPng + "KB).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al subir foto: " + e.getMessage());
        }
        
        return "redirect:/perfil";
    }
}
