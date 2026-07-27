package com.example.bancobienestar.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bancobienestar.Repository.CuentaRepository;
import com.example.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.bancobienestar.Repository.SolicitudCreditoRepository;
import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.CuentaEntity;
import com.example.bancobienestar.entity.MovimientoEntity;
import com.example.bancobienestar.entity.SolicitudCreditoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;
import com.example.bancobienestar.service.FondosinsuficientesException;


@Service
public class BancaService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final MovimientoCuentaRepository movimientoRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;
    private final PasswordEncoder passwordEncoder;

    public BancaService(UsuarioRepository usuarioRepository,
                        CuentaRepository cuentaRepository,
                        MovimientoCuentaRepository movimientoRepository,
                        SolicitudCreditoRepository solicitudCreditoRepository,
                        @Lazy PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================================
    // 1. TRANSFERENCIA ENTRE CLABES (ACID)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void transferirMonto(String clabeOrigen, String clabeDestino, Double monto, String descripcion) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        if (clabeOrigen.equals(clabeDestino)) {
            throw new IllegalArgumentException("La cuenta de destino no puede ser la misma que la de origen.");
        }

        CuentaEntity origen = cuentaRepository.findByClabe(clabeOrigen)
                .orElseThrow(() -> new RuntimeException("La cuenta de origen no existe."));

        CuentaEntity destino = cuentaRepository.findByClabe(clabeDestino)
                .orElseThrow(() -> new RuntimeException("La cuenta de destino no existe."));

        if (origen.getSaldo() < monto) {
            throw new FondosinsuficientesException("No cuentas con saldo suficiente para esta operación."); // ✅ Nombre corregido
        }
        //cargo a la cuenta destino
        origen.setSaldo(origen.getSaldo() - monto);
        cuentaRepository.save(origen);

        //abono a la cuenta destino
        destino.setSaldo(destino.getSaldo() + monto);
        cuentaRepository.save(destino);

        // Registro del movimiento
        MovimientoEntity movimiento = new MovimientoEntity();
        movimiento.setCuentaOrigen(clabeOrigen);
        movimiento.setCuentaDestino(clabeDestino);
        movimiento.setMonto(monto);
        movimiento.setDescripcion(descripcion);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipo("TRANSFERENCIA");
        movimiento.setEstado("COMPLETADO");
        movimiento.setEstadoMovimiento("autorizado");
        movimientoRepository.save(movimiento);
    }

    // ============================================================
    // 2. TRANSFERENCIA DESDE USUARIO AUTENTICADO
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void transferirDesdeUsuario(String username, String clabeDestino, Double monto, String descripcion) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (usuario.getCuentas() == null || usuario.getCuentas().isEmpty()) {
            throw new RuntimeException("El usuario no tiene una cuenta bancaria asignada.");
        }

        String clabeOrigen = usuario.getCuentas().get(0).getClabe();
        transferirMonto(clabeOrigen, clabeDestino, monto, descripcion);
    }

    // ============================================================
    // 3. CREAR CLIENTE CON CUENTA (CLABE ÚNICA)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public UsuarioEntity crearClienteConCuenta(String nombre, String username, String password, Double saldoInicial) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("El nombre de usuario ya está registrado.");
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol("CLIENTE");
        usuario.setNombre(nombre);
        UsuarioEntity usuarioGuardado = usuarioRepository.save(usuario);

        String clabe = generarClabeUnica();

        CuentaEntity cuentas = new CuentaEntity();
        cuentas.setClabe(clabe);
        cuentas.setSaldo(saldoInicial);
        cuentas.setUsuario(usuarioGuardado);
        cuentaRepository.save(cuentas);

        List<CuentaEntity> list = new ArrayList<>();
        list.add(cuentas);
        usuarioGuardado.setCuentas(list);

        return usuarioGuardado;
    }

    // ============================================================
    // 4. SOLICITUD DE CRÉDITO (APROBACIÓN CON CAMBIO DE ESTADO)
    // ============================================================
    
    // Guardar solicitud inicial (estado PENDIENTE)
    @Transactional(rollbackFor = Exception.class)
    public SolicitudCreditoEntity guardarSolicitudCredito(String username, Double monto, String firmaBase64) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        SolicitudCreditoEntity solicitud = new SolicitudCreditoEntity();
        solicitud.setUsuario(usuario);
        solicitud.setMontoSolicitado(monto);
        solicitud.setFirmaBase64(firmaBase64);
        solicitud.setEstado("PENDIENTE");
        solicitud.setFecha(LocalDateTime.now());

        return solicitudCreditoRepository.save(solicitud);
    }

    // Cambiar estado de una solicitud (aprobado/rechazado)
    @Transactional(rollbackFor = Exception.class)
    public SolicitudCreditoEntity cambiarEstadoCredito(Long solicitudId, String nuevoEstado) {
        SolicitudCreditoEntity solicitud = solicitudCreditoRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        // Si se aprueba, abonar a la cuenta
        if ("APROBADO".equals(nuevoEstado) && !"APROBADO".equals(solicitud.getEstado())) {
            UsuarioEntity usuario = solicitud.getUsuario();
            if (usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
                CuentaEntity cuenta = usuario.getCuentas().get(0);
                cuenta.setSaldo(cuenta.getSaldo() + solicitud.getMontoSolicitado());
                cuentaRepository.save(cuenta);

                // Registrar movimiento
                MovimientoEntity movimiento = new MovimientoEntity();
                movimiento.setCuentaOrigen("CRÉDITO-BANCO");
                movimiento.setCuentaDestino(cuenta.getClabe());
                movimiento.setMonto(solicitud.getMontoSolicitado());
                movimiento.setDescripcion("Abono de Crédito Aprobado");
                movimiento.setFecha(LocalDateTime.now());
                movimiento.setTipo("CREDITO");
                movimiento.setEstado("COMPLETADO");
                movimiento.setEstadoMovimiento("autorizado");
                movimientoRepository.save(movimiento);
            }
        }

        solicitud.setEstado(nuevoEstado);
        return solicitudCreditoRepository.save(solicitud);
    }

    // Obtener solicitudes pendientes para el administrador
    public List<SolicitudCreditoEntity> obtenerSolicitudesPendientes() {
        return solicitudCreditoRepository.findByEstadoOrderByFechaDesc("PENDIENTE");
    }

    // ============================================================
    // 5. OBTENER TODOS LOS MOVIMIENTOS CON NOMBRE DE CLIENTE
    // ============================================================
    
    /**
     * Obtiene todos los movimientos ordenados por fecha descendente,
     * incluyendo el nombre del cliente asociado (origen o destino).
     */
    public List<Map<String, Object>> obtenerTodosMovimientos() {
        List<MovimientoEntity> movimientos = movimientoRepository.findAllByOrderByFechaDesc();
        List<Map<String, Object>> resultado = new ArrayList<>();
        
        for (MovimientoEntity mov : movimientos) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", mov.getId());
            item.put("fecha", mov.getFecha());
            item.put("cuentaOrigen", mov.getCuentaOrigen());
            item.put("cuentaDestino", mov.getCuentaDestino());
            item.put("monto", mov.getMonto());
            item.put("descripcion", mov.getDescripcion());
            item.put("tipo", mov.getTipo());
            item.put("estado", mov.getEstado());
            item.put("estadoMovimiento", mov.getEstadoMovimiento());
            
            // Buscar nombre del cliente asociado a la cuenta de origen
            String nombreCliente = "Desconocido";
            try {
                if (mov.getCuentaOrigen() != null && !mov.getCuentaOrigen().isEmpty()
                        && !mov.getCuentaOrigen().equals("CRÉDITO-BANCO")) {
                    Optional<CuentaEntity> cuentaOpt = cuentaRepository.findByClabe(mov.getCuentaOrigen());
                    if (cuentaOpt.isPresent()) {
                        nombreCliente = cuentaOpt.get().getUsuario().getNombre();
                    }
                } else if (mov.getCuentaDestino() != null && !mov.getCuentaDestino().isEmpty()) {
                    Optional<CuentaEntity> cuentaOpt = cuentaRepository.findByClabe(mov.getCuentaDestino());
                    if (cuentaOpt.isPresent()) {
                        nombreCliente = cuentaOpt.get().getUsuario().getNombre();
                    }
                }
            } catch (Exception e) {
                nombreCliente = "N/A";
            }
            item.put("nombreCliente", nombreCliente);
            resultado.add(item);
        }
        
        return resultado;
    }

    // ============================================================
    // 6. AUTORIZAR MOVIMIENTO
    // ============================================================
    
    @Transactional(rollbackFor = Exception.class)
    public void autorizarMovimiento(Long movimientoId) {
        MovimientoEntity movimiento = movimientoRepository.findById(movimientoId)
                .orElseThrow(() -> new RuntimeException("Movimiento no encontrado con ID: " + movimientoId));
        
        if ("autorizado".equalsIgnoreCase(movimiento.getEstadoMovimiento())) {
            throw new RuntimeException("El movimiento ya está autorizado.");
        }
        
        movimiento.setEstadoMovimiento("autorizado");
        movimientoRepository.save(movimiento);
    }

    // ============================================================
    // 7. CANCELAR MOVIMIENTO CON REVERSIÓN DE FONDOS
    // ============================================================
    
    @Transactional(rollbackFor = Exception.class)
    public void cancelarMovimiento(Long movimientoId) {
        MovimientoEntity movimiento = movimientoRepository.findById(movimientoId)
                .orElseThrow(() -> new RuntimeException("Movimiento no encontrado con ID: " + movimientoId));
        
        if ("cancelado".equalsIgnoreCase(movimiento.getEstadoMovimiento())) {
            throw new RuntimeException("El movimiento ya está cancelado.");
        }
        
        String tipo = movimiento.getTipo();
        String cuentaOrigen = movimiento.getCuentaOrigen();
        String cuentaDestino = movimiento.getCuentaDestino();
        Double monto = movimiento.getMonto();
        
        if ("TRANSFERENCIA".equals(tipo)) {
            // Reversión: tomar dinero de la cuenta destino y regresarlo a la cuenta origen
            CuentaEntity destino = cuentaRepository.findByClabe(cuentaDestino)
                    .orElseThrow(() -> new RuntimeException("La cuenta destino ya no existe."));
            CuentaEntity origen = cuentaRepository.findByClabe(cuentaOrigen)
                    .orElseThrow(() -> new RuntimeException("La cuenta origen ya no existe."));
            
            if (destino.getSaldo() < monto) {
                throw new FondosinsuficientesException(
                    "La cuenta destino no tiene fondos suficientes para realizar la cancelación (saldo: $" 
                    + String.format("%.2f", destino.getSaldo()) + ").");
            }
            
            // Revertir: quitar del destino, regresar al origen
            destino.setSaldo(destino.getSaldo() - monto);
            origen.setSaldo(origen.getSaldo() + monto);
            cuentaRepository.save(destino);
            cuentaRepository.save(origen);
            
            // Registrar movimiento de reversión
            MovimientoEntity reverso = new MovimientoEntity();
            reverso.setCuentaOrigen(cuentaDestino);
            reverso.setCuentaDestino(cuentaOrigen);
            reverso.setMonto(monto);
            reverso.setDescripcion("REVERSIÓN: " + movimiento.getDescripcion());
            reverso.setFecha(LocalDateTime.now());
            reverso.setTipo("REVERSIÓN");
            reverso.setEstado("COMPLETADO");
            reverso.setEstadoMovimiento("autorizado");
            movimientoRepository.save(reverso);
            
        } else if ("CREDITO".equals(tipo)) {
            // Reversión de crédito: quitar el monto de la cuenta que lo recibió
            CuentaEntity cuentaDest = cuentaRepository.findByClabe(cuentaDestino)
                    .orElseThrow(() -> new RuntimeException("La cuenta destino del crédito ya no existe."));
            
            if (cuentaDest.getSaldo() < monto) {
                throw new FondosinsuficientesException(
                    "La cuenta del cliente no tiene fondos suficientes para cancelar el crédito (saldo: $" 
                    + String.format("%.2f", cuentaDest.getSaldo()) + ").");
            }
            
            cuentaDest.setSaldo(cuentaDest.getSaldo() - monto);
            cuentaRepository.save(cuentaDest);
            
            // Registrar movimiento de reversión
            MovimientoEntity reverso = new MovimientoEntity();
            reverso.setCuentaOrigen(cuentaDestino);
            reverso.setCuentaDestino("CRÉDITO-BANCO");
            reverso.setMonto(monto);
            reverso.setDescripcion("CANCELACIÓN DE CRÉDITO: " + movimiento.getDescripcion());
            reverso.setFecha(LocalDateTime.now());
            reverso.setTipo("REVERSIÓN");
            reverso.setEstado("COMPLETADO");
            reverso.setEstadoMovimiento("autorizado");
            movimientoRepository.save(reverso);
        }
        
        // Marcar el movimiento original como cancelado
        movimiento.setEstadoMovimiento("cancelado");
        movimientoRepository.save(movimiento);
    }

    // ============================================================
    // 8. ELIMINAR MOVIMIENTO (CON REVERSIÓN SI ESTABA AUTORIZADO)
    // ============================================================
    
    @Transactional(rollbackFor = Exception.class)
    public void eliminarMovimiento(Long movimientoId) {
        MovimientoEntity movimiento = movimientoRepository.findById(movimientoId)
                .orElseThrow(() -> new RuntimeException("Movimiento no encontrado con ID: " + movimientoId));
        
        String estadoMov = movimiento.getEstadoMovimiento();
        String tipo = movimiento.getTipo();
        String cuentaOrigen = movimiento.getCuentaOrigen();
        String cuentaDestino = movimiento.getCuentaDestino();
        Double monto = movimiento.getMonto();
        
        // Si estaba autorizado y era transferencia/credito, revertir fondos primero
        if ("autorizado".equalsIgnoreCase(estadoMov)) {
            if ("TRANSFERENCIA".equals(tipo)) {
                CuentaEntity destino = cuentaRepository.findByClabe(cuentaDestino)
                        .orElseThrow(() -> new RuntimeException("La cuenta destino ya no existe."));
                CuentaEntity origen = cuentaRepository.findByClabe(cuentaOrigen)
                        .orElseThrow(() -> new RuntimeException("La cuenta origen ya no existe."));
                
                if (destino.getSaldo() < monto) {
                    throw new FondosinsuficientesException(
                        "La cuenta destino no tiene fondos suficientes para revertir (saldo: $" 
                        + String.format("%.2f", destino.getSaldo()) + ").");
                }
                
                destino.setSaldo(destino.getSaldo() - monto);
                origen.setSaldo(origen.getSaldo() + monto);
                cuentaRepository.save(destino);
                cuentaRepository.save(origen);
                
            } else if ("CREDITO".equals(tipo)) {
                CuentaEntity cuentaDest = cuentaRepository.findByClabe(cuentaDestino)
                        .orElseThrow(() -> new RuntimeException("La cuenta destino del crédito ya no existe."));
                
                if (cuentaDest.getSaldo() < monto) {
                    throw new FondosinsuficientesException(
                        "La cuenta del cliente no tiene fondos suficientes (saldo: $" 
                        + String.format("%.2f", cuentaDest.getSaldo()) + ").");
                }
                
                cuentaDest.setSaldo(cuentaDest.getSaldo() - monto);
                cuentaRepository.save(cuentaDest);
            }
        }
        
        // Eliminar el movimiento de la base de datos
        movimientoRepository.delete(movimiento);
    }

    // ============================================================
    // 9. OBTENER PERFIL DE USUARIO
    // ============================================================
    
    public Map<String, Object> obtenerPerfil(String username) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("id", usuario.getId());
        perfil.put("nombre", usuario.getNombre());
        perfil.put("username", usuario.getUsername());
        perfil.put("rol", usuario.getRol());
        perfil.put("fotoPerfil", usuario.getFotoPerfil());
        
        // Obtener información de la cuenta asociada
        if (usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
            CuentaEntity cuenta = usuario.getCuentas().get(0);
            perfil.put("clabe", cuenta.getClabe());
            perfil.put("saldo", cuenta.getSaldo());
        } else {
            perfil.put("clabe", "Sin cuenta");
            perfil.put("saldo", 0.0);
        }
        
        return perfil;
    }

    // ============================================================
    // 10. ACTUALIZAR PERFIL DE USUARIO
    // ============================================================
    
    @Transactional(rollbackFor = Exception.class)
    public void actualizarPerfil(String username, String nombreNuevo, String passwordActual, String passwordNueva) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        
        // Actualizar nombre si se proporcionó
        if (nombreNuevo != null && !nombreNuevo.isBlank()) {
            usuario.setNombre(nombreNuevo);
        }
        
        // Actualizar contraseña si se proporcionaron ambos campos
        if (passwordActual != null && !passwordActual.isBlank() 
            && passwordNueva != null && !passwordNueva.isBlank()) {
            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                throw new RuntimeException("La contraseña actual no es correcta.");
            }
            usuario.setPassword(passwordEncoder.encode(passwordNueva));
        }
        
        usuarioRepository.save(usuario);
    }

    // ============================================================
    // 11. ACTUALIZAR FOTO DE PERFIL
    // ============================================================
    
    @Transactional(rollbackFor = Exception.class)
    public void actualizarFotoPerfil(String username, String fotoBase64) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        
        usuario.setFotoPerfil(fotoBase64);
        usuarioRepository.save(usuario);
    }

    // ============================================================
    // 12. OBTENER TODOS LOS USUARIOS CON SUS CUENTAS
    // ============================================================
    
    public List<Map<String, Object>> obtenerTodosUsuarios() {
        List<UsuarioEntity> usuarios = usuarioRepository.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();
        
        for (UsuarioEntity usuario : usuarios) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", usuario.getId());
            item.put("nombre", usuario.getNombre());
            item.put("username", usuario.getUsername());
            item.put("rol", usuario.getRol());
            
            // Obtener cuentas del usuario
            List<Map<String, Object>> cuentasInfo = new ArrayList<>();
            if (usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
                for (CuentaEntity cuenta : usuario.getCuentas()) {
                    Map<String, Object> cuentaMap = new HashMap<>();
                    cuentaMap.put("id", cuenta.getId());
                    cuentaMap.put("clabe", cuenta.getClabe());
                    cuentaMap.put("saldo", cuenta.getSaldo());
                    cuentaMap.put("estado", cuenta.getEstado());
                    cuentasInfo.add(cuentaMap);
                }
            }
            item.put("cuentas", cuentasInfo);
            item.put("totalCuentas", cuentasInfo.size());
            
            // Primer CLABE y saldo para mostrar rápido
            if (!cuentasInfo.isEmpty()) {
                item.put("clabePrincipal", cuentasInfo.get(0).get("clabe"));
                item.put("saldoPrincipal", cuentasInfo.get(0).get("saldo"));
            } else {
                item.put("clabePrincipal", "Sin cuenta");
                item.put("saldoPrincipal", 0.0);
            }
            
            resultado.add(item);
        }
        
        return resultado;
    }

    // ============================================================
    // 12. ACTUALIZAR USUARIO (desde administración)
    // ============================================================
    
    @Transactional(rollbackFor = Exception.class)
    public void actualizarUsuario(Long id, String nombre, String username, String password) {
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        
        if (nombre != null && !nombre.isBlank()) {
            usuario.setNombre(nombre);
        }
        
        if (username != null && !username.isBlank()) {
            // Verificar que el nuevo username no esté en uso por otro usuario
            Optional<UsuarioEntity> existente = usuarioRepository.findByUsername(username);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new RuntimeException("El nombre de usuario '" + username + "' ya está registrado.");
            }
            usuario.setUsername(username);
        }
        
        if (password != null && !password.isBlank()) {
            usuario.setPassword(passwordEncoder.encode(password));
        }
        
        usuarioRepository.save(usuario);
    }

    // ============================================================
    // 13. ELIMINAR USUARIO Y SUS CUENTAS
    // ============================================================
    
    @Transactional(rollbackFor = Exception.class)
    public void eliminarUsuario(Long id) {
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        
        // Eliminar las cuentas asociadas
        if (usuario.getCuentas() != null) {
            cuentaRepository.deleteAll(usuario.getCuentas());
        }
        
        usuarioRepository.delete(usuario);
    }

    // ============================================================
    // 14. GENERAR CLABE ÚNICA (18 dígitos)
    // ============================================================
    private String generarClabeUnica() {
        Random random = new Random();
        String clabe;
        do {
            StringBuilder sb = new StringBuilder("012");
            for (int i = 0; i < 15; i++) {
                sb.append(random.nextInt(10));
            }
            clabe = sb.toString();
        } while (cuentaRepository.findByClabe(clabe).isPresent());
        return clabe;
    }
}