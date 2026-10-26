package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.Prato;
import com.comanda.model.StatusPedido;
import com.comanda.repository.PedidoItemRepository;
import com.comanda.repository.PedidoRepository;
import com.comanda.repository.PratoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Bloco 9 (RF-034 a RF-037). "Venda" = pedido confirmado pela cozinha (CONFIRMADO em diante, menos CANCELADO):
// é no CONFIRMADO que o estoque é baixado, então faturamento e food cost olham para os mesmos pedidos.
// Pedidos ainda RECEBIDO (aguardando confirmação) e CANCELADO aparecem à parte, só como contagem.
@Service
public class DashboardService {
    static final List<StatusPedido> VENDIDOS = List.of(StatusPedido.CONFIRMADO, StatusPedido.EM_PREPARO, StatusPedido.PRONTO,
            StatusPedido.SAIU_ENTREGA, StatusPedido.FINALIZADO);
    private static final int MAX_DIAS = 366;
    private static final int TOP = 5;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final PedidoRepository pedidos;
    private final PedidoItemRepository itens;
    private final PratoRepository pratos;
    private final FichaTecnicaService fichas;

    public DashboardService(PedidoRepository pedidos, PedidoItemRepository itens, PratoRepository pratos, FichaTecnicaService fichas) {
        this.pedidos = pedidos; this.itens = itens; this.pratos = pratos; this.fichas = fichas;
    }

    // RF-034: cards do dia (padrão: hoje), comparando com o dia anterior
    @Transactional(readOnly = true)
    public ResumoResponse resumo(LocalDate data) {
        LocalDate dia = data == null ? LocalDate.now() : data;
        Totais hoje = totais(dia, dia), ontem = totais(dia.minusDays(1), dia.minusDays(1));
        BigDecimal foodCost = foodCostMedio(vendidosPorPrato(dia, dia));
        LocalDateTime ini = dia.atStartOfDay(), fim = dia.plusDays(1).atStartOfDay();
        return new ResumoResponse(dia, hoje.faturamento(), hoje.pedidos(), ticket(hoje.faturamento(), hoje.pedidos()),
                foodCost, foodCost == null ? null : FichaTecnicaService.faixa(foodCost),
                ontem.faturamento(), ontem.pedidos(),
                pedidos.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(StatusPedido.RECEBIDO, ini, fim),
                pedidos.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(StatusPedido.CANCELADO, ini, fim));
    }

    // RF-035: os 5 pratos mais vendidos (em quantidade) no período escolhido
    @Transactional(readOnly = true)
    public List<TopPratoResponse> topPratos(LocalDate dataInicio, LocalDate dataFim) {
        Periodo p = periodo(dataInicio, dataFim);
        List<Vendido> lista = vendidosPorPrato(p.inicio(), p.fim());
        long total = lista.stream().mapToLong(Vendido::quantidade).sum();
        return lista.stream().limit(TOP).map(v -> new TopPratoResponse(v.pratoId(), v.prato(), v.categoria(), v.quantidade(),
                v.faturamento(), total == 0 ? BigDecimal.ZERO
                        : BigDecimal.valueOf(v.quantidade()).multiply(CEM).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)))
                .toList();
    }

    // RF-037: faturamento de cada dia do período (dias sem venda aparecem com zero, para a linha não "pular")
    @Transactional(readOnly = true)
    public VendasResponse vendas(LocalDate dataInicio, LocalDate dataFim) {
        Periodo p = periodo(dataInicio, dataFim);
        Map<LocalDate, BigDecimal> fatPorDia = new TreeMap<>();
        Map<LocalDate, Long> qtdPorDia = new HashMap<>();
        for (LocalDate d = p.inicio(); !d.isAfter(p.fim()); d = d.plusDays(1)) { fatPorDia.put(d, BigDecimal.ZERO); qtdPorDia.put(d, 0L); }
        for (Object[] l : pedidos.vendas(VENDIDOS, p.inicio().atStartOfDay(), p.fim().plusDays(1).atStartOfDay())) {
            LocalDate d = ((LocalDateTime) l[0]).toLocalDate();
            fatPorDia.merge(d, (BigDecimal) l[1], BigDecimal::add);
            qtdPorDia.merge(d, 1L, Long::sum);
        }
        List<VendaDiaResponse> dias = new ArrayList<>();
        BigDecimal fat = BigDecimal.ZERO; long qtd = 0;
        for (Map.Entry<LocalDate, BigDecimal> e : fatPorDia.entrySet()) {
            long n = qtdPorDia.get(e.getKey());
            dias.add(new VendaDiaResponse(e.getKey(), n, e.getValue(), ticket(e.getValue(), n)));
            fat = fat.add(e.getValue()); qtd += n;
        }
        BigDecimal foodCost = foodCostMedio(vendidosPorPrato(p.inicio(), p.fim()));
        return new VendasResponse(p.inicio(), p.fim(), fat, qtd, ticket(fat, qtd), foodCost,
                foodCost == null ? null : FichaTecnicaService.faixa(foodCost), dias);
    }

    // ================= Apoio =================

    private record Totais(BigDecimal faturamento, long pedidos) {}
    private record Periodo(LocalDate inicio, LocalDate fim) {}
    private record Vendido(Long pratoId, String prato, String categoria, long quantidade, BigDecimal faturamento) {}

    private Totais totais(LocalDate inicio, LocalDate fim) {
        BigDecimal fat = BigDecimal.ZERO; long n = 0;
        for (Object[] l : pedidos.vendas(VENDIDOS, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay())) {
            fat = fat.add((BigDecimal) l[1]); n++;
        }
        return new Totais(fat, n);
    }

    private List<Vendido> vendidosPorPrato(LocalDate inicio, LocalDate fim) {
        return itens.vendidosPorPrato(VENDIDOS, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay()).stream()
                .map(l -> new Vendido((Long) l[0], (String) l[1], (String) l[2], ((Number) l[3]).longValue(), decimal(l[4])))
                .toList();
    }

    // Food cost médio ponderado pelo que foi vendido: Σ(custo da porção × quantidade) ÷ faturamento × 100.
    // Usa o custo ATUAL de cada prato (ficha técnica × custo unitário de hoje). Prato sem ficha fica de fora.
    private BigDecimal foodCostMedio(List<Vendido> vendidos) {
        if (vendidos.isEmpty()) return null;
        List<Prato> lista = pratos.findAllById(vendidos.stream().map(Vendido::pratoId).toList());
        Map<Long, CustoResponse> custos = fichas.custosPorPrato(lista);
        BigDecimal custo = BigDecimal.ZERO, receita = BigDecimal.ZERO;
        for (Vendido v : vendidos) {
            CustoResponse c = custos.get(v.pratoId());
            if (c == null) continue;
            custo = custo.add(c.custoPorcao().multiply(BigDecimal.valueOf(v.quantidade())));
            receita = receita.add(v.faturamento());
        }
        return receita.signum() == 0 ? null : custo.multiply(CEM).divide(receita, 1, RoundingMode.HALF_UP);
    }

    // Padrão: últimos 30 dias até hoje
    private Periodo periodo(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = dataFim == null ? LocalDate.now() : dataFim;
        LocalDate inicio = dataInicio == null ? fim.minusDays(29) : dataInicio;
        if (inicio.isAfter(fim)) throw new ApiException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser depois da data final.");
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAX_DIAS)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escolha um período de até " + MAX_DIAS + " dias.");
        return new Periodo(inicio, fim);
    }

    private static BigDecimal ticket(BigDecimal faturamento, long pedidos) {
        return pedidos == 0 ? BigDecimal.ZERO : faturamento.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(Object o) {
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }
}
