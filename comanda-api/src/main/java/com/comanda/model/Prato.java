package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "prato")
public class Prato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Categoria categoria;
    private String nome;
    private String descricao;
    private String fotoUrl;
    private BigDecimal precoVenda;
    private Integer tempoPreparoMin;
    @Enumerated(EnumType.STRING) private StatusRegistro status;

    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getFotoUrl() { return fotoUrl; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public Integer getTempoPreparoMin() { return tempoPreparoMin; }
    public StatusRegistro getStatus() { return status; }
}
