package com.example.bancobienestar.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "movimientos")
public class MovimientoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_origen", nullable = false, length = 18)
    private String cuentaOrigen;

    @Column(name = "cuenta_destino", nullable = false, length = 18)
    private String cuentaDestino;

    @Column(nullable = false)
    private Double monto;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // MAPEA LA COLUMNA 'estado' (La que dice COMPLETADO en tu captura)
    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    // MAPEA LA COLUMNA 'estado_movimiento' DE FORMA EXPLÍCITA (La que dice EXITOSO)
    @Column(name = "estado_movimiento", nullable = false, length = 255)
    private String estadoMovimiento = "PENDIENTE";
}