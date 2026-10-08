package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "pedido_item")
public class PedidoItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Pedido pedido;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Prato prato;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private String observacoes;

    public Prato getPrato() { return prato; }                  public void setPrato(Prato v) { prato = v; }
    public void setPedido(Pedido v) { pedido = v; }
    public Integer getQuantidade() { return quantidade; }      public void setQuantidade(Integer v) { quantidade = v; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; } public void setPrecoUnitario(BigDecimal v) { precoUnitario = v; }
    public String getObservacoes() { return observacoes; }     public void setObservacoes(String v) { observacoes = v; }
}
