package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// RF-033: toda entrada e saída fica registrada (nunca se altera nem apaga uma movimentação)
@Entity @Table(name = "estoque_movimentacao")
public class EstoqueMovimentacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Ingrediente ingrediente;
    @Enumerated(EnumType.STRING) private TipoMovimentacao tipo;
    private BigDecimal quantidade;                 // sempre positiva, na unidade padrão do ingrediente
    @Enumerated(EnumType.STRING) private MotivoMovimentacao motivo;
    private String lote;
    private LocalDate validade;
    private BigDecimal custoUnitario;
    private Long pedidoCompraId;                   // preenchido na entrada de COMPRA (chave estrangeira criada na V7)
    @ManyToOne(fetch = FetchType.LAZY) private Pedido pedido;     // preenchido na baixa (VENDA) e no ESTORNO
    @ManyToOne(fetch = FetchType.LAZY) private Usuario usuario;   // quem fez
    private String observacao;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Ingrediente getIngrediente() { return ingrediente; }   public void setIngrediente(Ingrediente v) { ingrediente = v; }
    public TipoMovimentacao getTipo() { return tipo; }            public void setTipo(TipoMovimentacao v) { tipo = v; }
    public BigDecimal getQuantidade() { return quantidade; }      public void setQuantidade(BigDecimal v) { quantidade = v; }
    public MotivoMovimentacao getMotivo() { return motivo; }      public void setMotivo(MotivoMovimentacao v) { motivo = v; }
    public String getLote() { return lote; }                      public void setLote(String v) { lote = v; }
    public LocalDate getValidade() { return validade; }           public void setValidade(LocalDate v) { validade = v; }
    public BigDecimal getCustoUnitario() { return custoUnitario; } public void setCustoUnitario(BigDecimal v) { custoUnitario = v; }
    public Long getPedidoCompraId() { return pedidoCompraId; }    public void setPedidoCompraId(Long v) { pedidoCompraId = v; }
    public Pedido getPedido() { return pedido; }                  public void setPedido(Pedido v) { pedido = v; }
    public Usuario getUsuario() { return usuario; }               public void setUsuario(Usuario v) { usuario = v; }
    public String getObservacao() { return observacao; }          public void setObservacao(String v) { observacao = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
