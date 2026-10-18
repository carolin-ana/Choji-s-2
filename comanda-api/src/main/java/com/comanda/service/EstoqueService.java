package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Bloco 8 (RF-028 a RF-033) e bloco 6 (RF-017, RF-018, RN03). Toda regra de estoque fica aqui.
@Service
public class EstoqueService {
    private static final List<TipoMovimentacao> SOMAM = List.of(TipoMovimentacao.ENTRADA, TipoMovimentacao.ESTORNO);
    private static final List<TipoMovimentacao> SUBTRAEM = List.of(TipoMovimentacao.SAIDA);
    private static final Set<MotivoMovimentacao> MOTIVOS_SAIDA_MANUAL = EnumSet.of(MotivoMovimentacao.DESPERDICIO,
            MotivoMovimentacao.VENCIMENTO, MotivoMovimentacao.QUEBRA, MotivoMovimentacao.USO_INTERNO, MotivoMovimentacao.AJUSTE);
    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final EstoqueMovimentacaoRepository movimentacoes;
    private final IngredienteRepository ingredientes;
    private final FichaTecnicaRepository fichas;
    private final UsuarioRepository usuarios;

    public EstoqueService(EstoqueMovimentacaoRepository movimentacoes, IngredienteRepository ingredientes,
                          FichaTecnicaRepository fichas, UsuarioRepository usuarios) {
        this.movimentacoes = movimentacoes; this.ingredientes = ingredientes; this.fichas = fichas; this.usuarios = usuarios;
    }

    // ================= Saldo e alertas (RF-031 e RF-032) =================

    @Transactional(readOnly = true)
    public PaginaResponse<EstoqueSaldoResponse> listarSaldo(StatusRegistro status, String busca, Pageable pageable) {
        List<StatusRegistro> statusAceitos = status == null ? List.of(StatusRegistro.values()) : List.of(status);
        String termo = (busca == null || busca.isBlank()) ? "%" : "%" + busca.trim().toLowerCase() + "%";
        Page<Ingrediente> r = ingredientes.filtrar(statusAceitos, termo, PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA), Sort.by("nome").and(Sort.by("id"))));
        Map<Long, BigDecimal> saldos = saldos(r.getContent().stream().map(Ingrediente::getId).toList());
        return new PaginaResponse<>(r.getContent().stream().map(i -> resposta(i, saldos.getOrDefault(i.getId(), BigDecimal.ZERO))).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    // Ingredientes ativos com saldo abaixo do mínimo, do mais crítico para o menos crítico
    @Transactional(readOnly = true)
    public PaginaResponse<EstoqueSaldoResponse> alertas(Pageable pageable) {
        Map<Long, BigDecimal> saldos = saldosDeTodos();
        List<EstoqueSaldoResponse> lista = ingredientes.findByStatus(StatusRegistro.ATIVO).stream()
                .map(i -> resposta(i, saldos.getOrDefault(i.getId(), BigDecimal.ZERO)))
                .filter(EstoqueSaldoResponse::abaixoMinimo)
                // o mais crítico primeiro: menor saldo em proporção ao mínimo
                .sorted(Comparator.comparing((EstoqueSaldoResponse s) -> s.saldo().divide(s.estoqueMinimo(), 4, RoundingMode.HALF_UP)))
                .toList();
        int size = Math.max(1, Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA));
        int de = Math.min(pageable.getPageNumber() * size, lista.size());
        int ate = Math.min(de + size, lista.size());
        int totalPaginas = (lista.size() + size - 1) / size;
        return new PaginaResponse<>(lista.subList(de, ate), pageable.getPageNumber(), size, lista.size(), totalPaginas);
    }

    // RF-031: saldo = (entradas + estornos) - saídas
    public Map<Long, BigDecimal> saldos(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return combinar(movimentacoes.somar(SOMAM, ids), movimentacoes.somar(SUBTRAEM, ids));
    }

    private Map<Long, BigDecimal> saldosDeTodos() {
        return combinar(movimentacoes.somarTodos(SOMAM), movimentacoes.somarTodos(SUBTRAEM));
    }

    private Map<Long, BigDecimal> combinar(List<Object[]> mais, List<Object[]> menos) {
        Map<Long, BigDecimal> r = new HashMap<>();
        for (Object[] l : mais) r.merge((Long) l[0], (BigDecimal) l[1], BigDecimal::add);
        for (Object[] l : menos) r.merge((Long) l[0], ((BigDecimal) l[1]).negate(), BigDecimal::add);
        return r;
    }

    // ================= Movimentações (RF-028, RF-030 e RF-033) =================

    @Transactional(readOnly = true)
    public PaginaResponse<MovimentacaoResponse> listarMovimentacoes(Long ingredienteId, TipoMovimentacao tipo,
                                                                    LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        List<TipoMovimentacao> tipos = tipo == null ? List.of(TipoMovimentacao.values()) : List.of(tipo);
        LocalDateTime inicio = dataInicio == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim == null ? LocalDateTime.of(2999, 1, 1, 0, 0) : dataFim.plusDays(1).atStartOfDay();
        if (!inicio.isBefore(fim)) throw new ApiException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser depois da data final.");
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<EstoqueMovimentacao> r = movimentacoes.filtrar(ingredienteId == null, ingredienteId == null ? -1L : ingredienteId,
                tipos, inicio, fim, pagina);
        return new PaginaResponse<>(r.getContent().stream().map(this::resposta).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    // POST /api/admin/estoque/movimentacao: entrada de ajuste ou saída manual (perda)
    @Transactional
    public MovimentacaoResponse registrarManual(MovimentacaoRequest r, String email) {
        Usuario quem = usuarios.findByEmail(email).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        Ingrediente ing = ingredientes.findById(r.ingredienteId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Ingrediente não encontrado."));
        if (r.tipo() == TipoMovimentacao.ESTORNO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "O estorno é feito automaticamente quando um pedido é cancelado.");
        if (r.tipo() == TipoMovimentacao.ENTRADA && r.motivo() != MotivoMovimentacao.AJUSTE)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "A entrada manual usa o motivo AJUSTE. Entradas de compra são registradas no recebimento do pedido de compra.");
        if (r.tipo() == TipoMovimentacao.SAIDA && !MOTIVOS_SAIDA_MANUAL.contains(r.motivo()))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Escolha o motivo da saída: desperdício, vencimento, quebra, uso interno ou ajuste.");
        if (r.tipo() == TipoMovimentacao.SAIDA) {
            BigDecimal saldo = saldos(List.of(ing.getId())).getOrDefault(ing.getId(), BigDecimal.ZERO);
            if (r.quantidade().compareTo(saldo) > 0)
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente: há " + qtd(saldo, ing) + " de " + ing.getNome() + ".");
        }
        EstoqueMovimentacao m = nova(ing, r.tipo(), r.motivo(), r.quantidade(), quem);
        if (r.tipo() == TipoMovimentacao.ENTRADA) { // lote, validade e custo só fazem sentido na entrada
            m.setLote(vazioParaNulo(r.lote()));
            m.setValidade(r.validade());
            if (r.custoUnitario() != null) m.setCustoUnitario(r.custoUnitario());
        }
        m.setObservacao(vazioParaNulo(r.observacao()));
        return resposta(movimentacoes.save(m));
    }

    // ================= Integração com pedidos (bloco 6) =================

    // RN03: chamado ao CRIAR o pedido. Só confere; não mexe no estoque.
    @Transactional(readOnly = true)
    public void exigirDisponivel(List<PedidoItem> itens) {
        exigirSaldo(necessidade(itens), "Não há estoque suficiente para este pedido. Falta: ");
    }

    // RF-017: chamado quando o pedido vai para CONFIRMADO, dentro da MESMA transação da mudança de status.
    // Se faltar algum ingrediente, a exceção desfaz tudo (o status não muda e nada é baixado).
    @Transactional
    public void baixarPedido(Pedido p, Usuario quem) {
        Map<Long, Necessidade> nec = necessidade(p.getItens());
        exigirSaldo(nec, "Não é possível confirmar: falta estoque de ");
        for (Necessidade n : nec.values()) {
            EstoqueMovimentacao m = nova(n.ingrediente(), TipoMovimentacao.SAIDA, MotivoMovimentacao.VENDA, n.quantidade(), quem);
            m.setPedido(p);
            m.setObservacao("Baixa do pedido #" + p.getId());
            movimentacoes.save(m);
        }
    }

    // RF-018: chamado no cancelamento. Devolve exatamente o que foi baixado para este pedido (se foi baixado).
    @Transactional
    public void estornarPedido(Pedido p, Usuario quem) {
        if (!movimentacoes.findByPedidoIdAndMotivo(p.getId(), MotivoMovimentacao.ESTORNO).isEmpty()) return; // já estornado
        Map<Long, Necessidade> baixado = new LinkedHashMap<>();
        for (EstoqueMovimentacao v : movimentacoes.findByPedidoIdAndMotivo(p.getId(), MotivoMovimentacao.VENDA))
            baixado.merge(v.getIngrediente().getId(), new Necessidade(v.getIngrediente(), v.getQuantidade()),
                    (a, b) -> new Necessidade(a.ingrediente(), a.quantidade().add(b.quantidade())));
        for (Necessidade n : baixado.values()) {
            EstoqueMovimentacao m = nova(n.ingrediente(), TipoMovimentacao.ESTORNO, MotivoMovimentacao.ESTORNO, n.quantidade(), quem);
            m.setPedido(p);
            m.setObservacao("Estorno do pedido #" + p.getId() + " (cancelado)");
            movimentacoes.save(m);
        }
    }

    private record Necessidade(Ingrediente ingrediente, BigDecimal quantidade) {}

    // Quanto de cada ingrediente o pedido consome: quantidade da ficha × fator de correção ÷ rendimento × porções pedidas
    private Map<Long, Necessidade> necessidade(List<PedidoItem> itens) {
        Set<Long> pratoIds = itens.stream().map(i -> i.getPrato().getId()).collect(Collectors.toSet());
        Map<Long, FichaTecnica> fichaPorPrato = fichas.findByPratoIdIn(pratoIds).stream()
                .collect(Collectors.toMap(f -> f.getPrato().getId(), f -> f));
        Map<Long, Necessidade> total = new LinkedHashMap<>();
        for (PedidoItem item : itens) {
            FichaTecnica f = fichaPorPrato.get(item.getPrato().getId());
            if (f == null) continue; // prato sem ficha não consome estoque (pela RN01 ele nem fica ativo)
            BigDecimal rendimento = BigDecimal.valueOf(f.getRendimento() == null || f.getRendimento() < 1 ? 1 : f.getRendimento());
            for (FichaTecnicaItem fi : f.getItens()) {
                BigDecimal q = fi.getQuantidade().multiply(fi.getFatorCorrecao()).multiply(BigDecimal.valueOf(item.getQuantidade()))
                        .divide(rendimento, 3, RoundingMode.HALF_UP);
                total.merge(fi.getIngrediente().getId(), new Necessidade(fi.getIngrediente(), q),
                        (a, b) -> new Necessidade(a.ingrediente(), a.quantidade().add(b.quantidade())));
            }
        }
        return total;
    }

    // RN03: 422 dizendo exatamente o que falta
    private void exigirSaldo(Map<Long, Necessidade> nec, String inicio) {
        if (nec.isEmpty()) return;
        Map<Long, BigDecimal> saldos = saldos(nec.keySet());
        List<String> faltas = new ArrayList<>();
        for (Necessidade n : nec.values()) {
            BigDecimal saldo = saldos.getOrDefault(n.ingrediente().getId(), BigDecimal.ZERO);
            if (n.quantidade().compareTo(saldo) > 0)
                faltas.add(n.ingrediente().getNome() + " (precisa de " + qtd(n.quantidade(), n.ingrediente())
                        + ", há " + qtd(saldo.max(BigDecimal.ZERO), n.ingrediente()) + ")");
        }
        if (!faltas.isEmpty()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, inicio + String.join("; ", faltas) + ".");
    }

    // ================= Apoio =================

    private EstoqueMovimentacao nova(Ingrediente ing, TipoMovimentacao tipo, MotivoMovimentacao motivo, BigDecimal quantidade, Usuario quem) {
        EstoqueMovimentacao m = new EstoqueMovimentacao();
        m.setIngrediente(ing); m.setTipo(tipo); m.setMotivo(motivo); m.setQuantidade(quantidade);
        m.setCustoUnitario(ing.getCustoUnitario()); m.setUsuario(quem);
        return m;
    }

    private static String qtd(BigDecimal v, Ingrediente i) {
        String unidade = switch (i.getUnidadePadrao()) { case G -> "g"; case ML -> "ml"; case UN -> "un"; case KG -> "kg"; case L -> "L"; };
        return v.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',') + " " + unidade;
    }

    private static String vazioParaNulo(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    private EstoqueSaldoResponse resposta(Ingrediente i, BigDecimal saldo) {
        return new EstoqueSaldoResponse(i.getId(), i.getNome(), i.getSku(), i.getUnidadePadrao(), saldo, i.getEstoqueMinimo(),
                i.getCustoUnitario(), saldo.compareTo(i.getEstoqueMinimo()) < 0, i.getStatus());
    }

    private MovimentacaoResponse resposta(EstoqueMovimentacao m) {
        Ingrediente i = m.getIngrediente();
        return new MovimentacaoResponse(m.getId(), m.getCreatedAt(), i.getId(), i.getNome(), i.getUnidadePadrao(), m.getTipo(),
                m.getMotivo(), m.getQuantidade(), m.getLote(), m.getValidade(), m.getCustoUnitario(),
                m.getPedido() == null ? null : m.getPedido().getId(), m.getPedidoCompraId(),
                m.getUsuario() == null ? null : m.getUsuario().getNome(), m.getObservacao());
    }
}
