package com.comanda.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "categoria")
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String nome;
    private String descricao;
    private Integer ordem = 0;
    @Enumerated(EnumType.STRING) private StatusRegistro status = StatusRegistro.ATIVO;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }                 public void setNome(String v) { nome = v; }
    public String getDescricao() { return descricao; }       public void setDescricao(String v) { descricao = v; }
    public Integer getOrdem() { return ordem; }              public void setOrdem(Integer v) { ordem = v; }
    public StatusRegistro getStatus() { return status; }     public void setStatus(StatusRegistro v) { status = v; }
}
