package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Quantidade na unidade padrão do ingrediente; precoUnitario = preço por unidade padrão; subtotal = quantidade × preço
@Entity @Table(name = "pedido_compra_item")
public class PedidoCompraItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private PedidoCompra pedidoCompra;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Ingrediente ingrediente;
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;

    public Long getId() { return id; }
    public PedidoCompra getPedidoCompra() { return pedidoCompra; } public void setPedidoCompra(PedidoCompra v) { pedidoCompra = v; }
    public Ingrediente getIngrediente() { return ingrediente; }    public void setIngrediente(Ingrediente v) { ingrediente = v; }
    public BigDecimal getQuantidade() { return quantidade; }       public void setQuantidade(BigDecimal v) { quantidade = v; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; } public void setPrecoUnitario(BigDecimal v) { precoUnitario = v; }
    public BigDecimal getSubtotal() { return subtotal; }           public void setSubtotal(BigDecimal v) { subtotal = v; }
}
