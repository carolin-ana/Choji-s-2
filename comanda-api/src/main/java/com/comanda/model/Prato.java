package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "prato")
public class Prato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Categoria categoria;
    private String nome;
    private String descricao;
    private String fotoUrl;
    private BigDecimal precoVenda;
    private Integer tempoPreparoMin;
    @Enumerated(EnumType.STRING) private StatusRegistro status = StatusRegistro.INATIVO;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }       public void setCategoria(Categoria v) { categoria = v; }
    public String getNome() { return nome; }                    public void setNome(String v) { nome = v; }
    public String getDescricao() { return descricao; }          public void setDescricao(String v) { descricao = v; }
    public String getFotoUrl() { return fotoUrl; }              public void setFotoUrl(String v) { fotoUrl = v; }
    public BigDecimal getPrecoVenda() { return precoVenda; }    public void setPrecoVenda(BigDecimal v) { precoVenda = v; }
    public Integer getTempoPreparoMin() { return tempoPreparoMin; } public void setTempoPreparoMin(Integer v) { tempoPreparoMin = v; }
    public StatusRegistro getStatus() { return status; }        public void setStatus(StatusRegistro v) { status = v; }
}
