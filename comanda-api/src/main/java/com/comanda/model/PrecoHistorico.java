package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// RF-026: cada preço que um fornecedor já cobrou por um ingrediente (nunca se altera nem apaga)
@Entity @Table(name = "preco_historico")
public class PrecoHistorico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Ingrediente ingrediente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Fornecedor fornecedor;
    private BigDecimal preco;
    @Enumerated(EnumType.STRING) private OrigemPreco origem;
    private Long pedidoCompraId;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Ingrediente getIngrediente() { return ingrediente; }  public void setIngrediente(Ingrediente v) { ingrediente = v; }
    public Fornecedor getFornecedor() { return fornecedor; }     public void setFornecedor(Fornecedor v) { fornecedor = v; }
    public BigDecimal getPreco() { return preco; }               public void setPreco(BigDecimal v) { preco = v; }
    public OrigemPreco getOrigem() { return origem; }            public void setOrigem(OrigemPreco v) { origem = v; }
    public Long getPedidoCompraId() { return pedidoCompraId; }   public void setPedidoCompraId(Long v) { pedidoCompraId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
