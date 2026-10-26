package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.model.StatusCompra;
import com.comanda.service.CompraService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// RF-024 e RF-025: pedidos de compra. ADMIN e GERENTE.
@RestController
@RequestMapping("/api/admin/compras")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AdminCompraController {
    private final CompraService service;
    public AdminCompraController(CompraService service) { this.service = service; }

    @GetMapping
    public PaginaResponse<CompraResponse> listar(@RequestParam(required = false) StatusCompra status,
                                                 @RequestParam(required = false) Long fornecedorId,
                                                 @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(status, fornecedorId, pageable);
    }

    @GetMapping("/{id}")
    public CompraResponse buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CompraResponse criar(@Valid @RequestBody CompraRequest r, Authentication auth) { return service.criar(r, auth.getName()); }

    @PutMapping("/{id}")
    public CompraResponse atualizar(@PathVariable Long id, @Valid @RequestBody CompraRequest r) { return service.atualizar(id, r); }

    @PatchMapping("/{id}/enviar")
    public CompraResponse enviar(@PathVariable Long id) { return service.enviar(id); }

    // POST /api/admin/compras/{id}/receber (tabela de endpoints do SRS): entrada no estoque + custo novo (RN05)
    @PostMapping("/{id}/receber")
    public RecebimentoResponse receber(@PathVariable Long id, @Valid @RequestBody(required = false) ReceberRequest r, Authentication auth) {
        return service.receber(id, r, auth.getName());
    }

    @PatchMapping("/{id}/cancelar")
    public CompraResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarRequest r) { return service.cancelar(id, r.motivo()); }
}
