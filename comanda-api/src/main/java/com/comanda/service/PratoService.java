package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.*;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-010: CRUD de pratos no painel, com a regra RN01
@Service
public class PratoService {
    private static final int TAMANHO_MAXIMO_PAGINA = 50;
    private static final String RN01 = "Um prato só pode ficar ativo depois de ter ficha técnica com pelo menos 1 ingrediente.";

    private final PratoRepository pratos;
    private final CategoriaRepository categorias;
    private final FichaTecnicaRepository fichas;
    private final FichaTecnicaService fichaService;

    public PratoService(PratoRepository pratos, CategoriaRepository categorias, FichaTecnicaRepository fichas,
                        FichaTecnicaService fichaService) {
        this.pratos = pratos; this.categorias = categorias; this.fichas = fichas; this.fichaService = fichaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PratoAdminResponse> listar(Long categoriaId, StatusRegistro status, String busca, Pageable pageable) {
        List<StatusRegistro> statusAceitos = status == null ? List.of(StatusRegistro.values()) : List.of(status);
        String termo = (busca == null || busca.isBlank()) ? "%" : "%" + busca.trim().toLowerCase() + "%";
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                Sort.by("nome").and(Sort.by("id")));
        Page<Prato> r = pratos.filtrarAdmin(statusAceitos, categoriaId == null, categoriaId == null ? -1L : categoriaId, termo, pagina);
        Map<Long, CustoResponse> custos = fichaService.custosPorPrato(r.getContent());
        return new PaginaResponse<>(r.getContent().stream().map(p -> resposta(p, custos.get(p.getId()))).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PratoAdminResponse buscar(Long id) {
        Prato p = prato(id);
        return resposta(p, custo(p));
    }

    @Transactional
    public PratoAdminResponse criar(PratoRequest r) {
        if (r.status() == StatusRegistro.ATIVO) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, RN01); // ainda não tem ficha
        Prato p = new Prato();
        preencher(p, r);
        return resposta(pratos.save(p), null);
    }

    @Transactional
    public PratoAdminResponse atualizar(Long id, PratoRequest r) {
        Prato p = prato(id);
        if (r.status() == StatusRegistro.ATIVO && !fichas.existsByPratoId(id))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, RN01);
        preencher(p, r);
        return resposta(p, custo(p));
    }

    // RN06: "excluir" um prato = inativar (ele some do cardápio, mas os pedidos antigos continuam apontando para ele)
    @Transactional
    public PratoAdminResponse desativar(Long id) {
        Prato p = prato(id);
        p.setStatus(StatusRegistro.INATIVO);
        return resposta(p, custo(p));
    }

    private void preencher(Prato p, PratoRequest r) {
        Categoria c = categorias.findById(r.categoriaId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Categoria não encontrada."));
        p.setNome(r.nome().trim());
        p.setDescricao(vazioParaNulo(r.descricao()));
        p.setFotoUrl(vazioParaNulo(r.fotoUrl()));
        p.setPrecoVenda(r.precoVenda());
        p.setTempoPreparoMin(r.tempoPreparoMin());
        p.setCategoria(c);
        p.setStatus(r.status());
    }

    private CustoResponse custo(Prato p) {
        return fichas.findByPratoId(p.getId()).map(f -> fichaService.custo(f, p.getPrecoVenda())).orElse(null);
    }

    private Prato prato(Long id) {
        return pratos.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prato não encontrado."));
    }

    private static String vazioParaNulo(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    private PratoAdminResponse resposta(Prato p, CustoResponse custo) {
        return new PratoAdminResponse(p.getId(), p.getNome(), p.getDescricao(), p.getFotoUrl(), p.getPrecoVenda(),
                p.getTempoPreparoMin(), p.getCategoria().getId(), p.getCategoria().getNome(), p.getStatus(), custo != null, custo);
    }
}
