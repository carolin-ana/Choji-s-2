package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.model.StatusRegistro;
import com.comanda.service.FornecedorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// RF-021 e RF-022: fornecedores + catálogo. ADMIN e GERENTE (o cozinheiro não acessa fornecedores, RF-043).
@RestController
@RequestMapping("/api/admin/fornecedores")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AdminFornecedorController {
    private final FornecedorService service;
    public AdminFornecedorController(FornecedorService service) { this.service = service; }

    @GetMapping
    public PaginaResponse<FornecedorResponse> listar(@RequestParam(required = false) StatusRegistro status,
                                                     @RequestParam(required = false) String busca,
                                                     @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(status, busca, pageable);
    }

    @GetMapping("/{id}")
    public FornecedorResponse buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public FornecedorResponse criar(@Valid @RequestBody FornecedorRequest r) { return service.criar(r); }

    @PutMapping("/{id}")
    public FornecedorResponse atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorRequest r) { return service.atualizar(id, r); }

    @DeleteMapping("/{id}") // RN06: desativa
    public FornecedorResponse desativar(@PathVariable Long id) { return service.alterarStatus(id, StatusRegistro.INATIVO); }

    @PatchMapping("/{id}/reativar")
    public FornecedorResponse reativar(@PathVariable Long id) { return service.alterarStatus(id, StatusRegistro.ATIVO); }

    // ---- Catálogo (RF-022) ----
    @GetMapping("/{id}/catalogo")
    public List<CatalogoItemResponse> catalogo(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean incluirInativos) {
        return service.catalogo(id, incluirInativos);
    }

    @PostMapping("/{id}/catalogo") @ResponseStatus(HttpStatus.CREATED)
    public CatalogoItemResponse adicionar(@PathVariable Long id, @Valid @RequestBody CatalogoItemRequest r) {
        return service.adicionarAoCatalogo(id, r);
    }

    @PutMapping("/{id}/catalogo/{itemId}")
    public CatalogoItemResponse atualizarItem(@PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody CatalogoPrecoRequest r) {
        return service.atualizarItem(id, itemId, r);
    }

    @DeleteMapping("/{id}/catalogo/{itemId}") // RN06: tira do catálogo sem apagar
    public CatalogoItemResponse removerItem(@PathVariable Long id, @PathVariable Long itemId) { return service.removerItem(id, itemId); }
}
