package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.Categoria;
import com.comanda.model.StatusRegistro;
import com.comanda.repository.CategoriaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-009: criar, editar, listar e desativar categorias
@Service
public class CategoriaService {
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private final CategoriaRepository categorias;

    public CategoriaService(CategoriaRepository categorias) { this.categorias = categorias; }

    @Transactional(readOnly = true)
    public PaginaResponse<CategoriaResponse> listar(StatusRegistro status, String busca, Pageable pageable) {
        List<StatusRegistro> statusAceitos = status == null ? List.of(StatusRegistro.values()) : List.of(status);
        String termo = (busca == null || busca.isBlank()) ? "%" : "%" + busca.trim().toLowerCase() + "%";
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                Sort.by("ordem").and(Sort.by("nome")));
        Page<Categoria> r = categorias.filtrar(statusAceitos, termo, pagina);
        return new PaginaResponse<>(r.getContent().stream().map(this::resposta).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Long id) { return resposta(categoria(id)); }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest r) {
        if (categorias.existsByNomeIgnoreCase(r.nome().trim()))
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma categoria com este nome.");
        Categoria c = new Categoria();
        preencher(c, r);
        return resposta(categorias.save(c));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest r) {
        Categoria c = categoria(id);
        if (categorias.existsByNomeIgnoreCaseAndIdNot(r.nome().trim(), id))
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma categoria com este nome.");
        preencher(c, r);
        return resposta(c);
    }

    // RN06: desativar em vez de apagar. Os pratos dela somem do cardápio público (RN09), mas continuam no painel.
    @Transactional
    public CategoriaResponse alterarStatus(Long id, StatusRegistro novo) {
        Categoria c = categoria(id);
        c.setStatus(novo);
        return resposta(c);
    }

    private void preencher(Categoria c, CategoriaRequest r) {
        c.setNome(r.nome().trim());
        c.setDescricao(r.descricao() == null || r.descricao().isBlank() ? null : r.descricao().trim());
        c.setOrdem(r.ordem());
    }

    private Categoria categoria(Long id) {
        return categorias.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Categoria não encontrada."));
    }

    private CategoriaResponse resposta(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao(), c.getOrdem(), c.getStatus());
    }
}
