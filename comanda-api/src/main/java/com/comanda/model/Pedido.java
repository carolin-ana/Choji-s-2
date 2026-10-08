package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "pedido")
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Usuario cliente;
    @Enumerated(EnumType.STRING) private StatusPedido status;
    private BigDecimal valorTotal;
    private String enderecoEntrega;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL) private List<PedidoItem> itens = new ArrayList<>();

    public Long getId() { return id; }
    public Usuario getCliente() { return cliente; }            public void setCliente(Usuario v) { cliente = v; }
    public StatusPedido getStatus() { return status; }         public void setStatus(StatusPedido v) { status = v; }
    public BigDecimal getValorTotal() { return valorTotal; }   public void setValorTotal(BigDecimal v) { valorTotal = v; }
    public String getEnderecoEntrega() { return enderecoEntrega; } public void setEnderecoEntrega(String v) { enderecoEntrega = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<PedidoItem> getItens() { return itens; }
}
