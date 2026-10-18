package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.model.StatusRegistro;
import com.comanda.model.TipoMovimentacao;
import com.comanda.service.EstoqueService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Bloco 8 (RF-028 a RF-033). ADMIN e GERENTE, como na tabela de endpoints do SRS.
@RestController
@RequestMapping("/api/admin/estoque")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AdminEstoqueController {
    private final EstoqueService service;
    public AdminEstoqueController(EstoqueService service) { this.service = service; }

    // RF-031: saldo atual de cada ingrediente
    @GetMapping("/saldo")
    public PaginaResponse<EstoqueSaldoResponse> saldo(@RequestParam(required = false) StatusRegistro status,
                                                      @RequestParam(required = false) String busca,
                                                      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.listarSaldo(status, busca, pageable);
    }

    // RF-032: ingredientes ativos abaixo do estoque mínimo
    @GetMapping("/alertas")
    public PaginaResponse<EstoqueSaldoResponse> alertas(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.alertas(pageable);
    }

    // RF-033: histórico de entradas e saídas
    @GetMapping("/movimentacoes")
    public PaginaResponse<MovimentacaoResponse> movimentacoes(
            @RequestParam(required = false) Long ingredienteId,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.listarMovimentacoes(ingredienteId, tipo, dataInicio, dataFim, pageable);
    }

    // RF-028 (entrada de ajuste) e RF-030 (saída manual com motivo obrigatório)
    @PostMapping("/movimentacao") @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse registrar(@Valid @RequestBody MovimentacaoRequest r, Authentication auth) {
        return service.registrarManual(r, auth.getName());
    }
}
