package com.example.bancobienestar.security;


import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.UsuarioEntity;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuario){
        this.usuarioRepository = usuario;

    }

    @Override
    public UserDetails loadUserByUsername(String username){
        UsuarioEntity usuariologin = usuarioRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("No se encontro cliente"));
        
        // aqui se usa el builder de spring security
        // se agrega automaticamente el rol de cliente
        return User.builder()
        .username(usuariologin.getUsername())
        .password(usuariologin.getPassword())
        .roles(usuariologin.getRol())
        .build();
        
    }
}
