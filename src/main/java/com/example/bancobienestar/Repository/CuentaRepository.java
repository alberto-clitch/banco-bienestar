package com.example.bancobienestar.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.bancobienestar.entity.CuentaEntity;
import com.example.bancobienestar.entity.UsuarioEntity;

import java.util.List;
import java.util.Optional;




@Repository
public interface CuentaRepository extends JpaRepository<CuentaEntity,Long>{
    Optional<CuentaEntity> findByClabe(String clabe); 
    List<CuentaEntity> findByUsuario(UsuarioEntity usuario);
}
