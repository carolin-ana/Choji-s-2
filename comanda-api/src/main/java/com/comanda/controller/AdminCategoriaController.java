package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.model.StatusRegistro;
import com.comanda.service.CategoriaService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// RF-009 (bloco 3). ADMIN e GERENTE, como na tabela de endpoints do SRS.
@RestController
@RequestMapping("/api/admin/categorias")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AdminCategoriaController {
    private final CategoriaService service;
    public AdminCategoriaController(CategoriaService service) { this.service = service; }

    @GetMapping
    public PaginaResponse<CategoriaResponse> listar(@RequestParam(required = false) StatusRegistro status,
                                                    @RequestParam(required = false) String busca,
                                                    @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(status, busca, pageable);
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criar(@Valid @RequestBody CategoriaRequest r) { return service.criar(r); }

    @PutMapping("/{id}")
    public CategoriaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest r) { return service.atualizar(id, r); }

    @DeleteMapping("/{id}") // RN06: desativa
    public CategoriaResponse desativar(@PathVariable Long id) { return service.alterarStatus(id, StatusRegistro.INATIVO); }

    @PatchMapping("/{id}/reativar")
    public CategoriaResponse reativar(@PathVariable Long id) { return service.alterarStatus(id, StatusRegistro.ATIVO); }
}
