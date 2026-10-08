package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.model.CanalPedido;
import com.comanda.model.StatusPedido;
import com.comanda.service.PedidoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Painel da cozinha/gerência. O controller só recebe, chama o Service e devolve (RNF04).
@RestController
@RequestMapping("/api/admin/pedidos")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','COZINHEIRO')")
public class AdminPedidoController {
    private final PedidoService service;
    public AdminPedidoController(PedidoService service) { this.service = service; }

    // Ex.: GET /api/admin/pedidos?status=RECEBIDO&dataInicio=2026-10-01&dataFim=2026-10-08&page=0&size=10
    @GetMapping
    public PaginaResponse<PedidoAdminResponse> listar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) CanalPedido canal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return service.listarAdmin(status, canal, dataInicio, dataFim, pageable);
    }

    @GetMapping("/{id}")
    public PedidoAdminResponse detalhe(@PathVariable Long id) { return service.buscarAdmin(id); }

    @PatchMapping("/{id}/status")
    public PedidoAdminResponse mudarStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r, Authentication auth) {
        return service.mudarStatus(id, r.status(), auth.getName());
    }

    // Só ADMIN e GERENTE cancelam (tabela de endpoints do SRS). O Service confere de novo (RN04).
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public PedidoAdminResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarRequest r, Authentication auth) {
        return service.cancelar(id, r.motivo(), auth.getName());
    }
}
