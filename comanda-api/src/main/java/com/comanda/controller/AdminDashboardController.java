package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Bloco 9 (RF-034 a RF-037). ADMIN e GERENTE. Os alertas de estoque (RF-036) vêm de /api/admin/estoque/alertas.
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AdminDashboardController {
    private final DashboardService service;
    public AdminDashboardController(DashboardService service) { this.service = service; }

    // RF-034: faturamento, pedidos, ticket médio e food cost médio do dia (padrão: hoje)
    @GetMapping("/resumo")
    public ResumoResponse resumo(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.resumo(data);
    }

    // RF-035: top 5 pratos do período (padrão: últimos 30 dias)
    @GetMapping("/top-pratos")
    public List<TopPratoResponse> topPratos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.topPratos(dataInicio, dataFim);
    }

    // RF-037: faturamento por dia + totais do período
    @GetMapping("/vendas")
    public VendasResponse vendas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.vendas(dataInicio, dataFim);
    }
}
