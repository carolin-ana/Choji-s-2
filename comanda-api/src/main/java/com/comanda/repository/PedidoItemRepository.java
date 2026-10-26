package com.comanda.repository;

import com.comanda.model.PedidoItem;
import com.comanda.model.StatusPedido;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {
    // RF-035: quanto cada prato vendeu no período, do mais vendido para o menos vendido.
    // Cada linha = [id do prato, nome, categoria, quantidade vendida, faturamento].
    @Query("""
        select pr.id, pr.nome, c.nome, sum(i.quantidade), sum(i.precoUnitario * i.quantidade)
        from PedidoItem i join i.pedido p join i.prato pr join pr.categoria c
        where p.status in :status and p.createdAt >= :inicio and p.createdAt < :fim
        group by pr.id, pr.nome, c.nome
        order by sum(i.quantidade) desc, sum(i.precoUnitario * i.quantidade) desc, pr.nome
        """)
    List<Object[]> vendidosPorPrato(@Param("status") Collection<StatusPedido> status, @Param("inicio") LocalDateTime inicio,
                                    @Param("fim") LocalDateTime fim);
}
