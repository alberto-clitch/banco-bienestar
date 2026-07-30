package com.example.bancobienestar.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.CuentaEntity;
import com.example.bancobienestar.entity.MovimientoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;
import com.example.bancobienestar.service.PdfGeneratorService;
import com.lowagie.text.DocumentException;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class DashboardController {
    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;
    private final PdfGeneratorService pdfGeneratorService;

    public DashboardController(UsuarioRepository usuario, MovimientoCuentaRepository movi,
                               PdfGeneratorService pdfGeneratorService) {
        this.usuarioRepository = usuario;
        this.movimientoCuentaRepository = movi;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }


    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String mostrarDashboard(Model modelo, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        String username = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                        .orElseThrow(()-> new RuntimeException("usuario no existe"));

        if("EJECUTIVO".equalsIgnoreCase(usuario.getRol())){
            return"redirect:/admin/dashboard";
        }
        
        //cargar datos del cliente 
        String clave = "No asignada";
        Double saldo = 0.0;
        List<MovimientoEntity> ultimosMovi = new ArrayList<>();

        if(usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()){
            CuentaEntity cuentaPrincipal = usuario.getCuentas().get(0);
            clave = cuentaPrincipal.getClabe();
            saldo = cuentaPrincipal.getSaldo();

            //cargamos ultimos movimientos 
            ultimosMovi = 
            movimientoCuentaRepository.findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(clave, clave);
        }


        // inyectar los datos al modelo thymeleaf
        modelo.addAttribute("nombreCliente", usuario.getNombre());
        modelo.addAttribute("saldoTotal", saldo);
        modelo.addAttribute("cuentaClabe", clave);
        modelo.addAttribute("movimientos", ultimosMovi);

        return "dashboard";
    }

    // ============================================================
    // DESCARGA DE PDF - ESTADO DE CUENTA
    // ============================================================

    /**
     * Endpoint para descargar un PDF con el estado de cuenta del cliente,
     * incluyendo informacion basica y el historial de movimientos.
     */
    @GetMapping("/dashboard/descargar-movimientos-pdf")
    public ResponseEntity<byte[]> descargarMovimientosPdf(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }

        String username = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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
}
