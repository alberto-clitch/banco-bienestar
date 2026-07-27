package com.example.bancobienestar.controller;

import java.lang.annotation.Target;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.CuentaEntity;
import com.example.bancobienestar.entity.UsuarioEntity;
import com.example.bancobienestar.service.BancaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@Controller

public class TransferenciaController {
    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;
    public TransferenciaController(BancaService bancaService, UsuarioRepository usuarioRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
    }
    @GetMapping("/transferencias")
    public String mostrarFormTransferencias(Model modelo, Authentication auth) {
        String username = auth.getName();
        UsuarioEntity usuario  = usuarioRepository.findByUsername(username)
        .orElseThrow(()-> new RuntimeException("Usuario no encontrado"));

        String clabe ="No asignada";
        Double saldo = 0.0;
        
        if(usuario.getCuentas()!=null && !usuario.getCuentas().isEmpty()) {
            CuentaEntity cuentaPrincipal = usuario.getCuentas().get(0);
            clabe = cuentaPrincipal.getClabe();
            saldo = cuentaPrincipal.getSaldo();
        }
        modelo.addAttribute("cuentaClabe",clabe);
        modelo.addAttribute("saldo", saldo);
        return "transferencia";
    }

    @PostMapping("/procesar-transferencia")
    public String procesar(
        @RequestParam String cuentaDestino,
        @RequestParam Double monto,
        @RequestParam String descripcion,
        Authentication auth){
            String usernameAutorizado = auth.getName();
            try{
                bancaService.transferirDesdeUsuario(usernameAutorizado,
                    cuentaDestino, monto, descripcion);
                    return"redirect:/dashboard?exito=transferencia Exitosa";
            }catch(Exception e){
                
                return "redirect:/transferencias?error="+e.getMessage();
            }  
       
    }
    

}
