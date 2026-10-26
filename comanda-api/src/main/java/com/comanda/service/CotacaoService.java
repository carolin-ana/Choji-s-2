package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.FornecedorProduto;
import com.comanda.model.Ingrediente;
import com.comanda.model.StatusRegistro;
import com.comanda.repository.FornecedorProdutoRepository;
import com.comanda.repository.IngredienteRepository;
import com.comanda.repository.PrecoHistoricoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-023 (cotação comparativa) e RF-026 (histórico de preços)
@Service
public class CotacaoService {
    private static final BigDecimal CEM = new BigDecimal("100");
    private final IngredienteRepository ingredientes;
    private final FornecedorProdutoRepository produtos;
    private final PrecoHistoricoRepository historico;
    private final EstoqueService estoque;

    public CotacaoService(IngredienteRepository ingredientes, FornecedorProdutoRepository produtos,
                          PrecoHistoricoRepository historico, EstoqueService estoque) {
        this.ingredientes = ingredientes; this.produtos = produtos; this.historico = historico; this.estoque = estoque;
    }

    // Todos os fornecedores ativos que vendem o ingrediente, do mais barato para o mais caro,
    // junto com o saldo atual (fluxo 8.3 do SRS: vê o alerta, cota e compra)
    @Transactional(readOnly = true)
    public CotacaoResponse cotacao(Long ingredienteId) {
        Ingrediente ing = ingrediente(ingredienteId);
        List<FornecedorProduto> lista = produtos.cotacao(ingredienteId, StatusRegistro.ATIVO);
        BigDecimal menor = lista.isEmpty() ? null : lista.get(0).getPreco();
        List<CotacaoItemResponse> itens = lista.stream().map(p -> new CotacaoItemResponse(
                p.getFornecedor().getId(), p.getFornecedor().getRazaoSocial(), p.getFornecedor().getTelefone(),
                p.getFornecedor().getEmail(), p.getPreco(), p.getUnidadeVenda(),
                p.getPreco().subtract(menor).multiply(CEM).divide(menor, 1, RoundingMode.HALF_UP),
                p.getPreco().compareTo(menor) == 0, p.getUpdatedAt())).toList();
        BigDecimal saldo = estoque.saldos(List.of(ing.getId())).getOrDefault(ing.getId(), BigDecimal.ZERO);
        return new CotacaoResponse(ing.getId(), ing.getNome(), ing.getSku(), ing.getUnidadePadrao(), ing.getCustoUnitario(),
                saldo, ing.getEstoqueMinimo(), saldo.compareTo(ing.getEstoqueMinimo()) < 0, itens);
    }

    @Transactional(readOnly = true)
    public List<PrecoHistoricoResponse> historico(Long ingredienteId) {
        ingrediente(ingredienteId);
        return historico.findByIngredienteIdOrderByCreatedAtAscIdAsc(ingredienteId).stream()
                .map(h -> new PrecoHistoricoResponse(h.getFornecedor().getId(), h.getFornecedor().getRazaoSocial(), h.getPreco(),
                        h.getOrigem(), h.getPedidoCompraId(), h.getCreatedAt()))
                .toList();
    }

    private Ingrediente ingrediente(Long id) {
        return ingredientes.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ingrediente não encontrado."));
    }
}
