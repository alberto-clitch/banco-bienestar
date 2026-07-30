package com.example.bancobienestar.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bancobienestar.service.BancaService;

@Controller
public class CreditoAbonoController {

    private final BancaService bancaService;

    public CreditoAbonoController(BancaService bancaService) {
        this.bancaService = bancaService;
    }

    @GetMapping("/mis-creditos")
    public String mostrarMisCreditos(Model model, Authentication authentication) {
        String username = authentication.getName();
        List<Map<String, Object>> creditos = bancaService.obtenerCreditosAprobados(username);
        model.addAttribute("creditos", creditos);
        return "mis-creditos";
    }

    @PostMapping("/realizar-abono")
    public String realizarAbono(@RequestParam("creditoId") Long creditoId,
                                @RequestParam("monto") Double monto,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Map<String, Object> resultado = bancaService.realizarAbono(creditoId, monto, username);

            boolean pagado = (boolean) resultado.get("creditoPagado");
            if (pagado) {
                redirectAttributes.addFlashAttribute("exito",
                        "Felicidades! Has pagado tu credito completamente. "
                        + "Abonaste $ " + String.format("%,.2f", monto) + ".");
            } else {
                redirectAttributes.addFlashAttribute("exito",
                        "Abono realizado correctamente. Monto: $ "
                        + String.format("%,.2f", monto)
                        + ". Nuevo saldo pendiente: $ "
                        + String.format("%,.2f", (double) resultado.get("nuevoSaldoPendiente")) + ".");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al realizar abono: " + e.getMessage());
        }
        return "redirect:/mis-creditos";
    }
}
