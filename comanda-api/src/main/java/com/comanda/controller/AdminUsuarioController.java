package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.model.Perfil;
import com.comanda.model.StatusRegistro;
import com.comanda.service.UsuarioService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// RF-041: gestão de usuários internos. Só o ADMIN acessa (tabela de endpoints do SRS).
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {
    private final UsuarioService service;
    public AdminUsuarioController(UsuarioService service) { this.service = service; }

    // Ex.: GET /api/admin/usuarios?perfil=COZINHEIRO&status=ATIVO&busca=ana&page=0&size=10
    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(@RequestParam(required = false) Perfil perfil,
                                                  @RequestParam(required = false) StatusRegistro status,
                                                  @RequestParam(required = false) String busca,
                                                  @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return service.listar(perfil, status, busca, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody NovoUsuarioRequest r) { return service.criar(r); }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarUsuarioRequest r) {
        return service.atualizar(id, r);
    }

    // RN06: "excluir" = desativar (exclusão lógica)
    @DeleteMapping("/{id}")
    public UsuarioResponse desativar(@PathVariable Long id) { return service.alterarStatus(id, StatusRegistro.INATIVO); }

    @PatchMapping("/{id}/reativar")
    public UsuarioResponse reativar(@PathVariable Long id) { return service.alterarStatus(id, StatusRegistro.ATIVO); }
}
