package com.example.bancobienestar.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.bancobienestar.Repository.SolicitudCreditoRepository;
import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.SolicitudCreditoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;
import com.example.bancobienestar.service.BancaService;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequestMapping("/admin")
public class AdminController {
    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;
    
    public AdminController(BancaService bancaService,
            UsuarioRepository usuarioRepository, 
            SolicitudCreditoRepository solicitudCreditoRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
    }
     @GetMapping("/creditos-abonos")
     public String mostrarCreditosAbonos(Model modelo) {
         List<Map<String, Object>> creditosConAbonos = bancaService.obtenerCreditosConAbonos();
         modelo.addAttribute("creditos", creditosConAbonos);
         return "admin-creditos-abonos";
     }

     @GetMapping("/dashboard")
     public String mostrarDashboard(Model modelo){
        List<UsuarioEntity> clientes = usuarioRepository.findAll().stream()
            .filter(u -> "CLIENTE".equals(u.getRol()))
            .collect(Collectors.toList());
            List<SolicitudCreditoEntity> solicitudes = 
            solicitudCreditoRepository.findAllByOrderByFechaDesc();

            // Obtener creditos con abonos para el panel
            List<Map<String, Object>> creditosConAbonos = bancaService.obtenerCreditosConAbonos();

            modelo.addAttribute("clientes", clientes);
            modelo.addAttribute("solicitudes", solicitudes);
            modelo.addAttribute("creditosConAbonos", creditosConAbonos);

           
         return "admin";
     }
      @PostMapping("/cambiar-estado-credito")
     public String cambiarEstadoCredito(
             @RequestParam("solicitudId") Long solicitudId,
             @RequestParam("nuevoEstado") String nuevoEstado) {
         try {
             bancaService.cambiarEstadoCredito(solicitudId, nuevoEstado);
             return "redirect:/admin/dashboard?exito=Estado de solicitud actualizado a " + nuevoEstado;
         } catch (Exception e) {
             return "redirect:/admin/dashboard?error=Error al actualizar estado: " + e.getMessage();
         }
     }

     @PostMapping("/crear-cliente")
            public String crearcliente(
                @RequestParam("username") String username,
                @RequestParam("password") String password,
                @RequestParam("saldoInicial") Double saldoInicial,
                @RequestParam("nombre") String nombre)
                {

                    if(username == null || username.trim().isEmpty() 
                     ||password == null || password.trim().isEmpty()
                     ||nombre == null || nombre.trim().isEmpty()) {
                        return "redirect:/admin/dashboard?error=Campos incompletos";
                    }
                    if(saldoInicial == null || saldoInicial < 0) {
                        return "redirect:/admin/dashboard?error=Saldo inicial invalido";
                    }
                    try{
                        bancaService.crearClienteConCuenta(nombre, username, password, saldoInicial);
                        return "redirect:/admin/dashboard?exito=Cliente creado exitosamente";
                    }catch(Exception e){
                        return "redirect:/admin/dashboard?error=Error al crear el cliente: " + e.getMessage();
                    }

            }
     
}
