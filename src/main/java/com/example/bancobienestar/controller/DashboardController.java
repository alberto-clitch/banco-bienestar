package com.example.bancobienestar.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.CuentaEntity;
import com.example.bancobienestar.entity.MovimientoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {
    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;

    public DashboardController(UsuarioRepository usuario, MovimientoCuentaRepository movi) {
        this.usuarioRepository = usuario;
        this.movimientoCuentaRepository = movi;
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
}
