package com.comanda.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Uma linha para cada mudança de status de um pedido (RF-020: linha do tempo com data e hora)
@Entity @Table(name = "pedido_status_historico")
public class PedidoHistorico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Pedido pedido;
    @Enumerated(EnumType.STRING) private StatusPedido status;
    @ManyToOne(fetch = FetchType.LAZY) private Usuario usuario; // quem fez a mudança (pode ser vazio em registros antigos)
    private LocalDateTime createdAt;

    protected PedidoHistorico() {} // o JPA exige um construtor sem argumentos

    public PedidoHistorico(Pedido pedido, StatusPedido status, Usuario usuario, LocalDateTime quando) {
        this.pedido = pedido; this.status = status; this.usuario = usuario; this.createdAt = quando;
    }

    public StatusPedido getStatus() { return status; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
