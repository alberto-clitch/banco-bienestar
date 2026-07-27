package com.example.bancobienestar.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.bancobienestar.Repository.UsuarioRepository;
import com.example.bancobienestar.entity.GastosDTO;
import com.example.bancobienestar.entity.MovimientoEntity;
import com.example.bancobienestar.entity.UsuarioEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // CORREGIDO: Importación correcta
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/finanzas")
public class finanzasRestController { 

    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;

    private static final Map<String, String> COLOR_MAP = new HashMap<>();
    
    static {
        // CORREGIDO: Claves en minúsculas para coincidir con .toLowerCase()
        COLOR_MAP.put("Alimentacion", "#FF6384");
        COLOR_MAP.put("Vivienda", "#650fef");
        COLOR_MAP.put("Transporte", "#50f305");
        COLOR_MAP.put("Otros", "#f46e0e");
        COLOR_MAP.put("Servicios", "rgb(255, 0, 162)");
        COLOR_MAP.put("Ocio", "#00a6ff");
        COLOR_MAP.put("Comida", "#965a67");
        COLOR_MAP.put("Renta", "#5100ff");
        COLOR_MAP.put("Nomina", "#26ff00");
    }

    private static final List<String> PALETA_COLORES = Arrays.asList(
        "#FF6384", "#650fef", "#50f305", "#f46e0e", "rgb(255, 0, 162)",
        "#00a6ff", "#965a67", "#5100ff", "#26ff00"
    );

    public finanzasRestController(UsuarioRepository usuarioRepository,
                                  MovimientoCuentaRepository movimientoCuentaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.movimientoCuentaRepository = movimientoCuentaRepository;
    }
        
    @GetMapping("/gastos-mes")
    public List<GastosDTO> obtenerGastos(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()){
            throw new RuntimeException("usuario no identificado");
        }
        
        String username = auth.getName();
        Optional<UsuarioEntity> usuarioOpt = 
        usuarioRepository.findByUsername(username);
        if(usuarioOpt.isEmpty()){
            throw new RuntimeException("Usuario no encontrado"); 
        }
    
        UsuarioEntity usuario = usuarioOpt.get();
        if(usuario.getCuentas() == null || usuario.getCuentas().isEmpty()){
            throw new RuntimeException("El usuario no tiene cuenta"); 
        }

        String clabe = usuario.getCuentas().get(0).getClabe();
        List<MovimientoEntity> movimientos = movimientoCuentaRepository.findByCuentaOrigen(clabe);

        if (movimientos == null || movimientos.isEmpty()){
             
        }
        
        Map<String, Double> gastosAgrupados = movimientos.stream()
            .collect(Collectors.groupingBy(MovimientoEntity::getDescripcion,
                Collectors.summingDouble(MovimientoEntity::getMonto)
            ));

        List<GastosDTO> resultado = new ArrayList<>();
        int colorIdx = 0; 
        
        for(Map.Entry<String, Double> entry : gastosAgrupados.entrySet()){
            String descripcion = entry.getKey();
            Double monto = entry.getValue();

            String key = descripcion.toLowerCase().trim();
            String color = COLOR_MAP.get(key);
            
            if(color == null){
                color = PALETA_COLORES.get(colorIdx % PALETA_COLORES.size());
                colorIdx++; 
            }
            resultado.add(new GastosDTO(descripcion, monto, color));
        }
        return resultado;
    }
}