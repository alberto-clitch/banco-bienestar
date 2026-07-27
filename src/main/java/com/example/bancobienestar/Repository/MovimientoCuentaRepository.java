package com.example.bancobienestar.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bancobienestar.entity.MovimientoEntity;


@Repository
public interface MovimientoCuentaRepository extends JpaRepository<MovimientoEntity,Long>{
    List<MovimientoEntity> 
    findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc
    (String cuentaOrigen, String cuentaDestino);
    List<MovimientoEntity> findByCuentaOrigen(String cuentaOrigen);
    List<MovimientoEntity> findAllByOrderByFechaDesc();

}

