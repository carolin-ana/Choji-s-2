package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.FornecedorProdutoRepository;
import com.comanda.repository.FornecedorRepository;
import com.comanda.repository.IngredienteRepository;
import com.comanda.repository.PrecoHistoricoRepository;
import com.comanda.validation.Cnpj;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-021 (cadastro de fornecedores) e RF-022 (catálogo: que ingredientes cada fornecedor vende e a que preço)
@Service
public class FornecedorService {
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private final FornecedorRepository fornecedores;
    private final FornecedorProdutoRepository produtos;
    private final IngredienteRepository ingredientes;
    private final PrecoHistoricoRepository historico;

    public FornecedorService(FornecedorRepository fornecedores, FornecedorProdutoRepository produtos,
                             IngredienteRepository ingredientes, PrecoHistoricoRepository historico) {
        this.fornecedores = fornecedores; this.produtos = produtos; this.ingredientes = ingredientes; this.historico = historico;
    }

    // ================= Fornecedores (RF-021) =================

    @Transactional(readOnly = true)
    public PaginaResponse<FornecedorResponse> listar(StatusRegistro status, String busca, Pageable pageable) {
        List<StatusRegistro> statusAceitos = status == null ? List.of(StatusRegistro.values()) : List.of(status);
        boolean semBusca = busca == null || busca.isBlank();
        String termo = semBusca ? "%" : "%" + busca.trim().toLowerCase() + "%";
        // quem digita o CNPJ com pontuação também encontra: compara sem os pontos, barra e traço
        String soCnpj = semBusca ? "" : Cnpj.normalizar(busca).toLowerCase();
        String termoCnpj = soCnpj.isEmpty() ? termo : "%" + soCnpj + "%";
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                Sort.by("razaoSocial").and(Sort.by("id")));
        Page<Fornecedor> r = fornecedores.filtrar(statusAceitos, termo, termoCnpj, pagina);
        Map<Long, Long> itens = contarItens(r.getContent().stream().map(Fornecedor::getId).toList());
        return new PaginaResponse<>(r.getContent().stream().map(f -> resposta(f, itens.getOrDefault(f.getId(), 0L))).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscar(Long id) {
        Fornecedor f = fornecedor(id);
        return resposta(f, contarItens(List.of(id)).getOrDefault(id, 0L));
    }

    @Transactional
    public FornecedorResponse criar(FornecedorRequest r) {
        String cnpj = Cnpj.normalizar(r.cnpj());
        if (fornecedores.existsByCnpj(cnpj)) throw new ApiException(HttpStatus.CONFLICT, "Já existe um fornecedor com este CNPJ.");
        Fornecedor f = new Fornecedor();
        preencher(f, r, cnpj);
        return resposta(fornecedores.save(f), 0L);
    }

    @Transactional
    public FornecedorResponse atualizar(Long id, FornecedorRequest r) {
        Fornecedor f = fornecedor(id);
        String cnpj = Cnpj.normalizar(r.cnpj());
        if (fornecedores.existsByCnpjAndIdNot(cnpj, id)) throw new ApiException(HttpStatus.CONFLICT, "Já existe um fornecedor com este CNPJ.");
        preencher(f, r, cnpj);
        return buscar(id);
    }

    // RN06: "excluir" = desativar. Fornecedor inativo some da cotação e não recebe pedidos novos.
    @Transactional
    public FornecedorResponse alterarStatus(Long id, StatusRegistro novo) {
        Fornecedor f = fornecedor(id);
        f.setStatus(novo);
        return buscar(id);
    }

    private void preencher(Fornecedor f, FornecedorRequest r, String cnpj) {
        // a anotação @CnpjValido já validou; conferir de novo aqui deixa a regra no Service (RN07) mesmo se o DTO mudar
        if (!Cnpj.valido(cnpj)) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CNPJ inválido (confira os dígitos).");
        f.setRazaoSocial(r.razaoSocial().trim());
        f.setCnpj(cnpj);
        f.setTelefone(vazioParaNulo(r.telefone()));
        f.setEmail(vazioParaNulo(r.email()));
        f.setCategorias(vazioParaNulo(r.categorias()));
    }

    // ================= Catálogo (RF-022) =================

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> catalogo(Long fornecedorId, boolean incluirInativos) {
        fornecedor(fornecedorId);
        List<StatusRegistro> status = incluirInativos ? List.of(StatusRegistro.values()) : List.of(StatusRegistro.ATIVO);
        return produtos.catalogo(fornecedorId, status).stream().map(this::resposta).toList();
    }

    @Transactional
    public CatalogoItemResponse adicionarAoCatalogo(Long fornecedorId, CatalogoItemRequest r) {
        Fornecedor f = fornecedor(fornecedorId);
        Ingrediente ing = ingredientes.findById(r.ingredienteId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Ingrediente não encontrado."));
        if (ing.getStatus() != StatusRegistro.ATIVO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ing.getNome() + " está inativo. Reative o ingrediente antes de colocá-lo no catálogo.");
        // UNIQUE(fornecedor, ingrediente): se já esteve no catálogo e foi tirado, volta (RN06: a linha nunca é apagada)
        FornecedorProduto p = produtos.findByFornecedorIdAndIngredienteId(fornecedorId, ing.getId()).orElse(null);
        if (p != null && p.getStatus() == StatusRegistro.ATIVO)
            throw new ApiException(HttpStatus.CONFLICT, ing.getNome() + " já está no catálogo deste fornecedor. Edite o preço na lista.");
        if (p == null) { p = new FornecedorProduto(); p.setFornecedor(f); p.setIngrediente(ing); }
        p.setStatus(StatusRegistro.ATIVO);
        p.setPreco(r.preco());
        p.setUnidadeVenda(vazioParaNulo(r.unidadeVenda()));
        p.setUpdatedAt(LocalDateTime.now());
        produtos.save(p);
        registrarPreco(p, OrigemPreco.CATALOGO, null);
        return resposta(p);
    }

    @Transactional
    public CatalogoItemResponse atualizarItem(Long fornecedorId, Long itemId, CatalogoPrecoRequest r) {
        FornecedorProduto p = item(fornecedorId, itemId);
        boolean mudouPreco = p.getPreco().compareTo(r.preco()) != 0;
        p.setPreco(r.preco());
        p.setUnidadeVenda(vazioParaNulo(r.unidadeVenda()));
        p.setUpdatedAt(LocalDateTime.now());
        if (mudouPreco) registrarPreco(p, OrigemPreco.CATALOGO, null); // RF-026: só guarda quando o preço muda de fato
        return resposta(p);
    }

    // RN06: sai do catálogo (INATIVO), mas o histórico de preços continua
    @Transactional
    public CatalogoItemResponse removerItem(Long fornecedorId, Long itemId) {
        FornecedorProduto p = item(fornecedorId, itemId);
        p.setStatus(StatusRegistro.INATIVO);
        p.setUpdatedAt(LocalDateTime.now());
        return resposta(p);
    }

    // RF-026: toda mudança de preço vira um ponto no gráfico de evolução
    void registrarPreco(FornecedorProduto p, OrigemPreco origem, Long pedidoCompraId) {
        PrecoHistorico h = new PrecoHistorico();
        h.setIngrediente(p.getIngrediente()); h.setFornecedor(p.getFornecedor());
        h.setPreco(p.getPreco()); h.setOrigem(origem); h.setPedidoCompraId(pedidoCompraId);
        historico.save(h);
    }

    // ================= Apoio =================

    private Map<Long, Long> contarItens(List<Long> ids) {
        Map<Long, Long> r = new HashMap<>();
        if (ids.isEmpty()) return r;
        for (Object[] l : produtos.contarAtivos(ids, StatusRegistro.ATIVO)) r.put((Long) l[0], (Long) l[1]);
        return r;
    }

    private Fornecedor fornecedor(Long id) {
        return fornecedores.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado."));
    }

    private FornecedorProduto item(Long fornecedorId, Long itemId) {
        return produtos.findById(itemId).filter(p -> p.getFornecedor().getId().equals(fornecedorId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item do catálogo não encontrado."));
    }

    private static String vazioParaNulo(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    private FornecedorResponse resposta(Fornecedor f, long itens) {
        return new FornecedorResponse(f.getId(), f.getRazaoSocial(), f.getCnpj(), Cnpj.formatar(f.getCnpj()), f.getTelefone(),
                f.getEmail(), f.getCategorias(), f.getStatus(), itens);
    }

    private CatalogoItemResponse resposta(FornecedorProduto p) {
        Ingrediente i = p.getIngrediente();
        return new CatalogoItemResponse(p.getId(), i.getId(), i.getNome(), i.getSku(), i.getUnidadePadrao(), p.getPreco(),
                p.getUnidadeVenda(), i.getCustoUnitario(), p.getStatus(), p.getUpdatedAt());
    }
}
