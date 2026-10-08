package com.comanda.repository;

import com.comanda.model.CanalPedido;
import com.comanda.model.Pedido;
import com.comanda.model.StatusPedido;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteEmailOrderByCreatedAtDesc(String email);

    // RF-019: lista do painel com filtros + paginação. O Service sempre manda valores preenchidos
    // (todos os status / todos os canais / datas bem largas quando o filtro vem vazio).
    @EntityGraph(attributePaths = "cliente") // já traz o cliente junto, sem uma consulta extra por pedido
    @Query("""
        select p from Pedido p
        where p.status in :status
          and p.canal in :canal
          and p.createdAt >= :inicio
          and p.createdAt < :fim
        """)
    Page<Pedido> filtrar(@Param("status") List<StatusPedido> status, @Param("canal") List<CanalPedido> canal,
                         @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, Pageable pageable);
}
