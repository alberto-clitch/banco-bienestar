package com.example.bancobienestar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { 
    http
    .authorizeHttpRequests(authz -> authz
        // RECURSOS PÚBLICOS: Se añade "/login" de forma explícita
        .requestMatchers("/css/**", "/js/**", "/img/**", "/login").permitAll()
        
        // RUTAS AUTENTICADAS: Accesibles para cualquier usuario logueado
        .requestMatchers("/dashboard", "/transferencias",
                        "/procesar-transferencia", "/credito",
                        "/solicitudes-credito",
                        "/procesar-credito", "/api/v1/finanzas/**").authenticated()
        
        // PANEL DE ADMINISTRACIÓN: Filtro por rol del usuario
        .requestMatchers("/admin/**").hasRole("EJECUTIVO")
        
        // CUALQUIER OTRA PETICIÓN: Requiere inicio de sesión
        .anyRequest().authenticated()
    )
    .formLogin(form -> form
        .loginPage("/login")
        .defaultSuccessUrl("/dashboard", true)
        .failureUrl("/login?error=true")
        .permitAll()
    )
    .logout(logout -> logout
       .logoutUrl("/logout")
       .logoutSuccessUrl("/login?logout=true")
       .invalidateHttpSession(true)
       .deleteCookies("JSESSIONID")
       .permitAll()
    );

    return http.build();
}
}