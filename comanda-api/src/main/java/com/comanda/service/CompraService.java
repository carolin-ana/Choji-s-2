package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-024 (pedido de compra: RASCUNHO > ENVIADO > RECEBIDO, com total calculado) e RF-025 (recebimento).
// RN05: ao receber, o custo unitário de cada ingrediente passa a ser o preço pago nesta compra.
@Service
public class CompraService {
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private final PedidoCompraRepository compras;
    private final FornecedorRepository fornecedores;
    private final FornecedorProdutoRepository produtos;
    private final IngredienteRepository ingredientes;
    private final PrecoHistoricoRepository historico;
    private final FichaTecnicaRepository fichas;
    private final UsuarioRepository usuarios;
    private final EstoqueService estoque;
    private final FichaTecnicaService fichaService;

    public CompraService(PedidoCompraRepository compras, FornecedorRepository fornecedores, FornecedorProdutoRepository produtos,
                         IngredienteRepository ingredientes, PrecoHistoricoRepository historico, FichaTecnicaRepository fichas,
                         UsuarioRepository usuarios, EstoqueService estoque, FichaTecnicaService fichaService) {
        this.compras = compras; this.fornecedores = fornecedores; this.produtos = produtos; this.ingredientes = ingredientes;
        this.historico = historico; this.fichas = fichas; this.usuarios = usuarios; this.estoque = estoque; this.fichaService = fichaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CompraResponse> listar(StatusCompra status, Long fornecedorId, Pageable pageable) {
        List<StatusCompra> statusAceitos = status == null ? List.of(StatusCompra.values()) : List.of(status);
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<PedidoCompra> r = compras.filtrar(statusAceitos, fornecedorId == null, fornecedorId == null ? -1L : fornecedorId, pagina);
        return new PaginaResponse<>(r.getContent().stream().map(this::resposta).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CompraResponse buscar(Long id) { return resposta(compra(id)); }

    // Novo pedido sempre começa como RASCUNHO
    @Transactional
    public CompraResponse criar(CompraRequest r, String email) {
        PedidoCompra c = new PedidoCompra();
        c.setUsuario(usuario(email));
        preencher(c, r);
        return resposta(compras.save(c));
    }

    // Só o rascunho pode ser editado (depois de enviado, o fornecedor já recebeu o pedido)
    @Transactional
    public CompraResponse atualizar(Long id, CompraRequest r) {
        PedidoCompra c = compra(id);
        if (c.getStatus() != StatusCompra.RASCUNHO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Só dá para editar pedidos em rascunho. Este está " + rotulo(c.getStatus()) + ".");
        preencher(c, r);
        c.setUpdatedAt(LocalDateTime.now());
        return resposta(c);
    }

    // RASCUNHO > ENVIADO
    @Transactional
    public CompraResponse enviar(Long id) {
        PedidoCompra c = compra(id);
        if (c.getStatus() != StatusCompra.RASCUNHO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Só um rascunho pode ser enviado. Este pedido está " + rotulo(c.getStatus()) + ".");
        if (c.getItens().isEmpty()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Adicione pelo menos 1 item antes de enviar.");
        if (c.getFornecedor().getStatus() != StatusRegistro.ATIVO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "O fornecedor está inativo. Reative-o ou troque o fornecedor do rascunho.");
        c.setStatus(StatusCompra.ENVIADO);
        c.setEnviadoEm(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return resposta(c);
    }

    // RF-025: ENVIADO > RECEBIDO. Tudo na mesma transação: se algo falhar, nada entra no estoque e o status não muda.
    @Transactional
    public RecebimentoResponse receber(Long id, ReceberRequest r, String email) {
        PedidoCompra c = compra(id);
        if (c.getStatus() == StatusCompra.RASCUNHO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Envie o pedido ao fornecedor antes de registrar o recebimento.");
        if (c.getStatus() != StatusCompra.ENVIADO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Este pedido está " + rotulo(c.getStatus()) + " e não pode ser recebido.");
        Usuario quem = usuario(email);
        String lote = r == null ? null : r.lote();
        java.time.LocalDate validade = r == null ? null : r.validade();
        String obs = r == null || r.observacao() == null || r.observacao().isBlank() ? null : r.observacao().trim();

        List<CustoAlteradoResponse> custos = new ArrayList<>();
        for (PedidoCompraItem item : c.getItens()) {
            Ingrediente ing = item.getIngrediente();
            BigDecimal anterior = ing.getCustoUnitario();
            // entrada no estoque com motivo COMPRA, ligada a este pedido de compra
            estoque.entradaCompra(ing, item.getQuantidade(), item.getPrecoUnitario(), c.getId(), lote, validade, quem,
                    limitar("Recebimento do pedido de compra #" + c.getId() + (obs == null ? "" : " · " + obs), 255));
            ing.setCustoUnitario(item.getPrecoUnitario()); // RN05
            PrecoHistorico h = new PrecoHistorico();      // RF-026
            h.setIngrediente(ing); h.setFornecedor(c.getFornecedor()); h.setPreco(item.getPrecoUnitario());
            h.setOrigem(OrigemPreco.COMPRA); h.setPedidoCompraId(c.getId());
            historico.save(h);
            custos.add(new CustoAlteradoResponse(ing.getId(), ing.getNome(), ing.getUnidadePadrao(), anterior, item.getPrecoUnitario()));
        }
        c.setStatus(StatusCompra.RECEBIDO);
        c.setRecebidoEm(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());

        // Fluxo 8.3, passo 5: o food cost é sempre calculado com o custo atual, então já mudou. Aqui só mostramos o resultado.
        Set<Long> ids = c.getItens().stream().map(i -> i.getIngrediente().getId()).collect(Collectors.toSet());
        List<PratoAfetadoResponse> pratos = fichas.usandoIngredientes(ids).stream()
                .map(f -> {
                    CustoResponse custo = fichaService.custo(f, f.getPrato().getPrecoVenda());
                    return new PratoAfetadoResponse(f.getPrato().getId(), f.getPrato().getNome(), custo.foodCost(), custo.faixa());
                })
                .sorted(Comparator.comparing(PratoAfetadoResponse::foodCost).reversed())
                .toList();
        return new RecebimentoResponse(resposta(c), custos, pratos);
    }

    // Cancela antes de receber. Depois de recebido, a mercadoria já está no estoque: devolução vira saída de ajuste.
    @Transactional
    public CompraResponse cancelar(Long id, String motivo) {
        PedidoCompra c = compra(id);
        if (c.getStatus() == StatusCompra.RECEBIDO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Pedido já recebido não pode ser cancelado: a mercadoria já entrou no estoque. Em caso de devolução, registre uma saída de ajuste no estoque.");
        if (c.getStatus() == StatusCompra.CANCELADO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Este pedido já está cancelado.");
        c.setStatus(StatusCompra.CANCELADO);
        c.setMotivoCancelamento(motivo.trim());
        c.setUpdatedAt(LocalDateTime.now());
        return resposta(c);
    }

    // Monta os itens a partir do catálogo do fornecedor. O total é sempre calculado aqui (nunca vem do front).
    private void preencher(PedidoCompra c, CompraRequest r) {
        Fornecedor f = fornecedores.findById(r.fornecedorId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Fornecedor não encontrado."));
        if (f.getStatus() != StatusRegistro.ATIVO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, f.getRazaoSocial() + " está inativo. Escolha um fornecedor ativo.");
        Map<Long, FornecedorProduto> catalogo = produtos.catalogo(f.getId(), List.of(StatusRegistro.ATIVO)).stream()
                .collect(Collectors.toMap(p -> p.getIngrediente().getId(), Function.identity()));

        Set<Long> vistos = new HashSet<>();
        List<PedidoCompraItem> novos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CompraItemRequest ir : r.itens()) {
            FornecedorProduto p = catalogo.get(ir.ingredienteId());
            if (p == null) {
                String nome = ingredientes.findById(ir.ingredienteId()).map(Ingrediente::getNome).orElse("O ingrediente #" + ir.ingredienteId());
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, nome + " não está no catálogo de " + f.getRazaoSocial() + ".");
            }
            if (!vistos.add(ir.ingredienteId()))
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, p.getIngrediente().getNome() + " aparece em mais de uma linha. Junte as quantidades numa linha só.");
            BigDecimal preco = ir.precoUnitario() != null ? ir.precoUnitario() : p.getPreco();
            PedidoCompraItem item = new PedidoCompraItem();
            item.setPedidoCompra(c);
            item.setIngrediente(p.getIngrediente());
            item.setQuantidade(ir.quantidade());
            item.setPrecoUnitario(preco);
            item.setSubtotal(ir.quantidade().multiply(preco).setScale(2, RoundingMode.HALF_UP));
            total = total.add(item.getSubtotal());
            novos.add(item);
        }
        c.setFornecedor(f);
        c.setObservacoes(r.observacoes() == null || r.observacoes().isBlank() ? null : r.observacoes().trim());
        c.getItens().clear();          // orphanRemoval apaga as linhas antigas do rascunho
        c.getItens().addAll(novos);
        c.setValorTotal(total);
    }

    private PedidoCompra compra(Long id) {
        return compras.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido de compra não encontrado."));
    }

    private Usuario usuario(String email) {
        return usuarios.findByEmail(email).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    private static String limitar(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }

    private static String rotulo(StatusCompra s) {
        return switch (s) { case RASCUNHO -> "em rascunho"; case ENVIADO -> "enviado"; case RECEBIDO -> "recebido"; case CANCELADO -> "cancelado"; };
    }

    private CompraResponse resposta(PedidoCompra c) {
        List<CompraItemResponse> itens = c.getItens().stream().map(i -> new CompraItemResponse(i.getIngrediente().getId(),
                i.getIngrediente().getNome(), i.getIngrediente().getUnidadePadrao(), i.getQuantidade(), i.getPrecoUnitario(),
                i.getSubtotal())).toList();
        return new CompraResponse(c.getId(), c.getStatus(), c.getFornecedor().getId(), c.getFornecedor().getRazaoSocial(),
                c.getValorTotal(), c.getObservacoes(), c.getMotivoCancelamento(),
                c.getUsuario() == null ? null : c.getUsuario().getNome(), c.getCreatedAt(), c.getEnviadoEm(), c.getRecebidoEm(), itens);
    }
}
