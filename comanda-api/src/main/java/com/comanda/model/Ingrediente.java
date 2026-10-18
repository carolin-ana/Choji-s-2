package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// RF-027: o custo unitário é o preço de UMA unidade padrão (1 g, 1 ml, 1 un, 1 kg ou 1 L)
@Entity @Table(name = "ingrediente")
public class Ingrediente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String nome;
    private String sku;
    @Enumerated(EnumType.STRING) private UnidadeMedida unidadePadrao;
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;
    private BigDecimal custoUnitario = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) private StatusRegistro status = StatusRegistro.ATIVO;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }                         public void setNome(String v) { nome = v; }
    public String getSku() { return sku; }                           public void setSku(String v) { sku = v; }
    public UnidadeMedida getUnidadePadrao() { return unidadePadrao; } public void setUnidadePadrao(UnidadeMedida v) { unidadePadrao = v; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }   public void setEstoqueMinimo(BigDecimal v) { estoqueMinimo = v; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }   public void setCustoUnitario(BigDecimal v) { custoUnitario = v; }
    public StatusRegistro getStatus() { return status; }             public void setStatus(StatusRegistro v) { status = v; }
}
