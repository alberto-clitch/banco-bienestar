package com.example.bancobienestar.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bancobienestar.entity.AbonoCreditoEntity;
import com.example.bancobienestar.entity.SolicitudCreditoEntity;

@Repository
public interface AbonoCreditoRepository extends JpaRepository<AbonoCreditoEntity, Long> {

    List<AbonoCreditoEntity> findBySolicitudCreditoOrderByFechaDesc(SolicitudCreditoEntity solicitud);

    List<AbonoCreditoEntity> findAllByOrderByFechaDesc();
}
