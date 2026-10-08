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
    @Enumerated(EnumType.STRING) private CanalPedido canal = CanalPedido.SITE;
    private BigDecimal valorTotal;
    private String enderecoEntrega;
    private String observacoes;
    private String motivoCancelamento;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL) private List<PedidoItem> itens = new ArrayList<>();
    // cascade = ALL: ao salvar o pedido, as linhas novas do histórico são gravadas junto
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL) @OrderBy("createdAt ASC, id ASC")
    private List<PedidoHistorico> historico = new ArrayList<>();

    public Long getId() { return id; }
    public Usuario getCliente() { return cliente; }            public void setCliente(Usuario v) { cliente = v; }
    public StatusPedido getStatus() { return status; }         public void setStatus(StatusPedido v) { status = v; }
    public CanalPedido getCanal() { return canal; }            public void setCanal(CanalPedido v) { canal = v; }
    public BigDecimal getValorTotal() { return valorTotal; }   public void setValorTotal(BigDecimal v) { valorTotal = v; }
    public String getEnderecoEntrega() { return enderecoEntrega; } public void setEnderecoEntrega(String v) { enderecoEntrega = v; }
    public String getObservacoes() { return observacoes; }     public void setObservacoes(String v) { observacoes = v; }
    public String getMotivoCancelamento() { return motivoCancelamento; } public void setMotivoCancelamento(String v) { motivoCancelamento = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }  public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
    public List<PedidoItem> getItens() { return itens; }
    public List<PedidoHistorico> getHistorico() { return historico; }
}
