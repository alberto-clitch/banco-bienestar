package com.example.bancobienestar.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "abonos_credito")
public class AbonoCreditoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacion con la solicitud de credito
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_credito_id", nullable = false)
    private SolicitudCreditoEntity solicitudCredito;

    @Column(name = "monto_abonado", nullable = false)
    private Double montoAbonado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 100)
    private String metodoPago = "TRANSFERENCIA";

    @Column(name = "clabe_origen", length = 18)
    private String clabeOrigen;
}
