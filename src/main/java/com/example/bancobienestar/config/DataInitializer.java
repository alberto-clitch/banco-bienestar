package com.example.bancobienestar.config;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
// la unica funcion es crear un usuario ejecutivo y un cliente 
import org.springframework.stereotype.Component;

import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.UsuarioEntity;

@Component

public class DataInitializer implements CommandLineRunner{
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuario,PasswordEncoder pass ){
        this.usuarioRepository= usuario;
        this.passwordEncoder = pass;
    }
    @Override
    public void run(String... args) throws Exception{
        if (usuarioRepository.count() == 0 ) {
            System.out.println("Agregando datos de prueba...");
            // 1.- creando un ejecutivo 
            UsuarioEntity ejecutivo = new UsuarioEntity();
            ejecutivo.setUsername("ACHM");
            ejecutivo.setNombre("Jose Alberto Chavez");
            ejecutivo.setPassword(passwordEncoder.encode("Mmg2017032"));
            ejecutivo.setRol("EJECUTIVO");
            usuarioRepository.save(ejecutivo);
            System.out.println("Datos ejecutivo: ACHM - Mmg2017032");

            //2.- creamo un cliente 
            UsuarioEntity cliente = new UsuarioEntity();
            cliente.setUsername("alberto");
            cliente.setNombre("Beto Chavez");
            cliente.setPassword(passwordEncoder.encode("Mmg2017032"));
            cliente.setRol("CLIENTE");
            usuarioRepository.save(cliente);
            System.out.println("Datos cliente: alberto - Mmg2017032");
            
        }
    }


}
