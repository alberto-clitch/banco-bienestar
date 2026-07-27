package com.example.bancobienestar.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EntityAccessorsTest {

    @Test
    void usuarioEntityShouldExposeFieldsThroughAccessors() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre("Ana");
        usuario.setUsername("ana");
        usuario.setPassword("secret");
        usuario.setRol("CLIENTE");

        assertEquals("Ana", usuario.getNombre());
        assertEquals("ana", usuario.getUsername());
        assertEquals("secret", usuario.getPassword());
        assertEquals("CLIENTE", usuario.getRol());
    }

    @Test
    void gastosDtoShouldConstructWithProvidedValues() {
        GastosDTO gasto = new GastosDTO("Comida", 120.5, "#ff0000");

        assertEquals("Comida", gasto.getCategoria());
        assertEquals(120.5, gasto.getMonto());
        assertEquals("#ff0000", gasto.getColorHex());
    }
}
