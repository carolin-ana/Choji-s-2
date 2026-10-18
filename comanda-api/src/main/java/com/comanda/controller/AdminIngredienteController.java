package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.model.StatusRegistro;
import com.comanda.service.IngredienteService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// RF-027 (cadastro de ingredientes, usado pela ficha técnica). ADMIN e GERENTE.
@RestController
@RequestMapping("/api/admin/ingredientes")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AdminIngredienteController {
    private final IngredienteService service;
    public AdminIngredienteController(IngredienteService service) { this.service = service; }

    @GetMapping
    public PaginaResponse<IngredienteResponse> listar(@RequestParam(required = false) StatusRegistro status,
                                                      @RequestParam(required = false) String busca,
                                                      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(status, busca, pageable);
    }

    @GetMapping("/{id}")
    public IngredienteResponse buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public IngredienteResponse criar(@Valid @RequestBody IngredienteRequest r) { return service.criar(r); }

    @PutMapping("/{id}")
    public IngredienteResponse atualizar(@PathVariable Long id, @Valid @RequestBody IngredienteRequest r) { return service.atualizar(id, r); }

    @DeleteMapping("/{id}") // RN06: desativa
    public IngredienteResponse desativar(@PathVariable Long id) { return service.alterarStatus(id, StatusRegistro.INATIVO); }

    @PatchMapping("/{id}/reativar")
    public IngredienteResponse reativar(@PathVariable Long id) { return service.alterarStatus(id, StatusRegistro.ATIVO); }
}
