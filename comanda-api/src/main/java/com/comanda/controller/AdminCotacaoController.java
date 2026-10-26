package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.service.CotacaoService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// RF-023 (GET /api/admin/cotacao/{ingredienteId}, como na tabela de endpoints do SRS) e RF-026 (histórico de preços)
@RestController
@RequestMapping("/api/admin/cotacao")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AdminCotacaoController {
    private final CotacaoService service;
    public AdminCotacaoController(CotacaoService service) { this.service = service; }

    @GetMapping("/{ingredienteId}")
    public CotacaoResponse cotacao(@PathVariable Long ingredienteId) { return service.cotacao(ingredienteId); }

    @GetMapping("/{ingredienteId}/historico")
    public List<PrecoHistoricoResponse> historico(@PathVariable Long ingredienteId) { return service.historico(ingredienteId); }
}
