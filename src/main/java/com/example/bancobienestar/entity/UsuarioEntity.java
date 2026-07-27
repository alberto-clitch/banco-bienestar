package com.example.bancobienestar.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "usuarios")
public class UsuarioEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 


    @Column(nullable = false)
    private String nombre;
    
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String rol;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String fotoPerfil;

    // relacion con muchas cuentas 
   @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY) //  CORRECTO
private List<CuentaEntity> cuentas;
    
}
