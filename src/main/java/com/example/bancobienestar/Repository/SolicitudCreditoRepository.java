package com.example.bancobienestar.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bancobienestar.entity.SolicitudCreditoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;

import java.util.List;

@Repository  // ✅ Opcional pero recomendado para claridad (Spring lo detecta automáticamente)
public interface SolicitudCreditoRepository extends JpaRepository<SolicitudCreditoEntity, Long> {

    // Busca solicitudes por usuario específico, ordenadas por fecha descendente (más reciente primero)
    List<SolicitudCreditoEntity> findByUsuarioOrderByFechaDesc(UsuarioEntity usuario);

    // Busca todas las solicitudes ordenadas por fecha descendente (más reciente primero)
    List<SolicitudCreditoEntity> findAllByOrderByFechaDesc();

    // Busca solicitudes por estado (ej. "PENDIENTE", "APROBADO", "RECHAZADO"), ordenadas por fecha descendente
    List<SolicitudCreditoEntity> findByEstadoOrderByFechaDesc(String estado);

    // Busca solicitudes por usuario y estado, ordenadas por fecha descendente
    List<SolicitudCreditoEntity> findByUsuarioAndEstadoOrderByFechaDesc(UsuarioEntity usuario, String estado);
}