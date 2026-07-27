package com.example.bancobienestar.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CuentaController {

    @GetMapping("/registro")
    public String registro() {
       
        return "registro"; 
    }
}
