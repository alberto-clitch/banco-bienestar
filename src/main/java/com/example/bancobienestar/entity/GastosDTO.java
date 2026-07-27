package com.example.bancobienestar.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data 
@AllArgsConstructor
@NoArgsConstructor 
public class GastosDTO {
    private String categoria;
    private Double monto;
    private String colorHex;
}