package com.example.bancobienestar.controller;

import com.example.bancobienestar.Repository.SolicitudCreditoRepository;
import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.SolicitudCreditoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;
import com.example.bancobienestar.service.BancaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CreditoController {

    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;

    public CreditoController(BancaService bancaService,
                             UsuarioRepository usuarioRepository,
                             SolicitudCreditoRepository solicitudCreditoRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
    }

    @GetMapping("/credito")
    public String mostrarFormularioCredito(Model model, Authentication authentication) {
        // Obtener el usuario autenticado
        String username = authentication.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener sus solicitudes de crédito (ordenadas por fecha descendente)
        List<SolicitudCreditoEntity> solicitudes = solicitudCreditoRepository
                .findByUsuarioOrderByFechaDesc(usuario);

        // Pasar al modelo
        model.addAttribute("solicitudes", solicitudes);

        return "credito";
    }

    @GetMapping("/solicitudes-credito")
    public String mostrarSolicitudes(Model model, Authentication authentication) {
        String username = authentication.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<SolicitudCreditoEntity> solicitudes;
        if ("EJECUTIVO".equals(usuario.getRol())) {
            // Ejecutivos ven todas las solicitudes
            solicitudes = solicitudCreditoRepository.findAllByOrderByFechaDesc();
        } else {
            // Clientes ven solo sus propias solicitudes
            solicitudes = solicitudCreditoRepository.findByUsuarioOrderByFechaDesc(usuario);
        }

        // Calcular estadísticas para las tarjetas del dashboard
        long pendientes = solicitudes.stream().filter(s -> "PENDIENTE".equals(s.getEstado())).count();
        long aprobadas = solicitudes.stream().filter(s -> "APROBADO".equals(s.getEstado())).count();
        long rechazadas = solicitudes.stream().filter(s -> "RECHAZADO".equals(s.getEstado())).count();

        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("esEjecutivo", "EJECUTIVO".equals(usuario.getRol()));
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("aprobadas", aprobadas);
        model.addAttribute("rechazadas", rechazadas);

        return "solicitudes-credito";
    }

    @PostMapping("/procesar-credito")
    public String procesarCredito(@RequestParam Double monto,
                                  @RequestParam(required = false) String firmaBase64,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();

            // Guardar la solicitud (estado PENDIENTE)
            bancaService.guardarSolicitudCredito(username, monto, firmaBase64);

            //  Mensaje de éxito
            redirectAttributes.addFlashAttribute("exito",
                    " Solicitud de crédito enviada correctamente. Quedará en estado PENDIENTE hasta que un ejecutivo la apruebe.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Error al solicitar crédito: " + e.getMessage());
        }
        return "redirect:/credito";
    }
}