package com.comanda.repository;

import com.comanda.model.EstoqueMovimentacao;
import com.comanda.model.MotivoMovimentacao;
import com.comanda.model.TipoMovimentacao;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstoqueMovimentacaoRepository extends JpaRepository<EstoqueMovimentacao, Long> {
    // RF-031: soma das quantidades por ingrediente, só dos tipos pedidos. Cada linha = [id do ingrediente, soma].
    @Query("""
        select m.ingrediente.id, sum(m.quantidade) from EstoqueMovimentacao m
        where m.tipo in :tipos and m.ingrediente.id in :ingredientes
        group by m.ingrediente.id
        """)
    List<Object[]> somar(@Param("tipos") Collection<TipoMovimentacao> tipos, @Param("ingredientes") Collection<Long> ingredientes);

    @Query("select m.ingrediente.id, sum(m.quantidade) from EstoqueMovimentacao m where m.tipo in :tipos group by m.ingrediente.id")
    List<Object[]> somarTodos(@Param("tipos") Collection<TipoMovimentacao> tipos);

    // RF-033: histórico com filtros. Sem filtro de ingrediente: todosIngredientes = true.
    @EntityGraph(attributePaths = {"ingrediente", "usuario"})
    @Query("""
        select m from EstoqueMovimentacao m
        where (:todosIngredientes = true or m.ingrediente.id = :ingredienteId)
          and m.tipo in :tipos
          and m.createdAt >= :inicio and m.createdAt < :fim
        """)
    Page<EstoqueMovimentacao> filtrar(@Param("todosIngredientes") boolean todosIngredientes, @Param("ingredienteId") Long ingredienteId,
                                      @Param("tipos") Collection<TipoMovimentacao> tipos, @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim, Pageable pageable);

    // Movimentações de um pedido (para o estorno no cancelamento)
    @EntityGraph(attributePaths = "ingrediente")
    List<EstoqueMovimentacao> findByPedidoIdAndMotivo(Long pedidoId, MotivoMovimentacao motivo);
}
