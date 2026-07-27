package com.example.bancobienestar.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bancobienestar.entity.UsuarioEntity;
import java.util.Optional;


@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity,Long>{
    Optional<UsuarioEntity> findByUsername(String username);
    Optional<UsuarioEntity> findByNombre(String nombre);

}
