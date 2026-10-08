package com.comanda.repository;

import com.comanda.model.Pedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteEmailOrderByCreatedAtDesc(String email);
}
