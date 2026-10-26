package com.comanda.repository;

import com.comanda.model.PedidoCompra;
import com.comanda.model.StatusCompra;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {
    // Lista paginada (RNF08). Sem filtro de fornecedor: todosFornecedores = true.
    @EntityGraph(attributePaths = {"fornecedor", "usuario"})
    @Query("""
        select c from PedidoCompra c
        where c.status in :status and (:todosFornecedores = true or c.fornecedor.id = :fornecedorId)
        """)
    Page<PedidoCompra> filtrar(@Param("status") Collection<StatusCompra> status, @Param("todosFornecedores") boolean todosFornecedores,
                               @Param("fornecedorId") Long fornecedorId, Pageable pageable);
}
