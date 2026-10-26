package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// RF-024 e RF-025: pedido de compra ao fornecedor
@Entity @Table(name = "pedido_compra")
public class PedidoCompra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Fornecedor fornecedor;
    @Enumerated(EnumType.STRING) private StatusCompra status = StatusCompra.RASCUNHO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private String observacoes;
    private String motivoCancelamento;
    @ManyToOne(fetch = FetchType.LAZY) private Usuario usuario;   // quem criou
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime enviadoEm;
    private LocalDateTime recebidoEm;
    // orphanRemoval: no rascunho, trocar os itens apaga as linhas antigas (o pedido ainda não valia nada)
    @OneToMany(mappedBy = "pedidoCompra", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("id ASC")
    private List<PedidoCompraItem> itens = new ArrayList<>();

    public Long getId() { return id; }
    public Fornecedor getFornecedor() { return fornecedor; }      public void setFornecedor(Fornecedor v) { fornecedor = v; }
    public StatusCompra getStatus() { return status; }            public void setStatus(StatusCompra v) { status = v; }
    public BigDecimal getValorTotal() { return valorTotal; }      public void setValorTotal(BigDecimal v) { valorTotal = v; }
    public String getObservacoes() { return observacoes; }        public void setObservacoes(String v) { observacoes = v; }
    public String getMotivoCancelamento() { return motivoCancelamento; } public void setMotivoCancelamento(String v) { motivoCancelamento = v; }
    public Usuario getUsuario() { return usuario; }               public void setUsuario(Usuario v) { usuario = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }     public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }     public void setEnviadoEm(LocalDateTime v) { enviadoEm = v; }
    public LocalDateTime getRecebidoEm() { return recebidoEm; }   public void setRecebidoEm(LocalDateTime v) { recebidoEm = v; }
    public List<PedidoCompraItem> getItens() { return itens; }
}
