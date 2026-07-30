package com.example.bancobienestar.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.CuentaEntity;
import com.example.bancobienestar.entity.MovimientoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;
import com.example.bancobienestar.service.BancaService;
import com.example.bancobienestar.service.PdfGeneratorService;
import com.lowagie.text.DocumentException;

import java.io.IOException;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;
    private final PdfGeneratorService pdfGeneratorService;

    public UsuarioAdminController(BancaService bancaService,
                                  UsuarioRepository usuarioRepository,
                                  MovimientoCuentaRepository movimientoCuentaRepository,
                                  PdfGeneratorService pdfGeneratorService) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
        this.movimientoCuentaRepository = movimientoCuentaRepository;
        this.pdfGeneratorService = pdfGeneratorService;
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

    // ============================================================
    // DESCARGA DE PDF - ESTADO DE CUENTA DE UN USUARIO (para admin)
    // ============================================================

    /**
     * Endpoint para que el administrador descargue el estado de cuenta
     * de cualquier cliente en formato PDF.
     */
    @GetMapping("/{userId}/descargar-estado-cuenta")
    public ResponseEntity<byte[]> descargarEstadoCuentaUsuario(@PathVariable Long userId) {
        UsuarioEntity usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        String clave = "No asignada";
        Double saldo = 0.0;
        List<MovimientoEntity> movimientos = new ArrayList<>();

        if (usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
            CuentaEntity cuentaPrincipal = usuario.getCuentas().get(0);
            clave = cuentaPrincipal.getClabe();
            saldo = cuentaPrincipal.getSaldo();

            movimientos = movimientoCuentaRepository
                    .findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(clave, clave);
        }

        try {
            byte[] pdfBytes = pdfGeneratorService.generarPdfEstadoCuenta(usuario, clave, saldo, movimientos);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "estado-cuenta-" + usuario.getUsername().replaceAll("\\s+", "-") + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error al generar el PDF de estado de cuenta: " + e.getMessage());
        }
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
