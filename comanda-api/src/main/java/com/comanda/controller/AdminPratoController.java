package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.model.StatusRegistro;
import com.comanda.service.FichaTecnicaService;
import com.comanda.service.PratoService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// RF-010 a RF-014 (blocos 3 e 4): pratos, ficha técnica, custo e food cost. ADMIN e GERENTE.
@RestController
@RequestMapping("/api/admin/pratos")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class AdminPratoController {
    private final PratoService pratos;
    private final FichaTecnicaService fichas;

    public AdminPratoController(PratoService pratos, FichaTecnicaService fichas) { this.pratos = pratos; this.fichas = fichas; }

    // Ex.: GET /api/admin/pratos?categoriaId=1&status=ATIVO&busca=lamen&page=0&size=10
    @GetMapping
    public PaginaResponse<PratoAdminResponse> listar(@RequestParam(required = false) Long categoriaId,
                                                     @RequestParam(required = false) StatusRegistro status,
                                                     @RequestParam(required = false) String busca,
                                                     @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return pratos.listar(categoriaId, status, busca, pageable);
    }

    @GetMapping("/{id}")
    public PratoAdminResponse buscar(@PathVariable Long id) { return pratos.buscar(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PratoAdminResponse criar(@Valid @RequestBody PratoRequest r) { return pratos.criar(r); }

    @PutMapping("/{id}")
    public PratoAdminResponse atualizar(@PathVariable Long id, @Valid @RequestBody PratoRequest r) { return pratos.atualizar(id, r); }

    @DeleteMapping("/{id}") // RN06: inativa
    public PratoAdminResponse desativar(@PathVariable Long id) { return pratos.desativar(id); }

    // RF-012 e RF-013: custo calculado + food cost
    @GetMapping("/{id}/custo")
    public CustoResponse custo(@PathVariable Long id) { return fichas.custoDoPrato(id); }

    // RF-011 e RF-014: ficha técnica (ingredientes, rendimento e modo de preparo)
    @GetMapping("/{id}/ficha")
    public FichaResponse ficha(@PathVariable Long id) { return fichas.buscar(id); }

    @PostMapping("/{id}/ficha") @ResponseStatus(HttpStatus.CREATED)
    public FichaResponse criarFicha(@PathVariable Long id, @Valid @RequestBody FichaRequest r) { return fichas.criar(id, r); }

    @PutMapping("/{id}/ficha")
    public FichaResponse atualizarFicha(@PathVariable Long id, @Valid @RequestBody FichaRequest r) { return fichas.atualizar(id, r); }

    // Calcula o custo de uma ficha em edição sem salvar (a tela mostra o custo enquanto a pessoa digita)
    @PostMapping("/{id}/ficha/simular")
    public FichaResponse simular(@PathVariable Long id, @Valid @RequestBody FichaRequest r) { return fichas.simular(id, r); }
}
