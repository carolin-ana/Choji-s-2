package com.comanda.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// RF-011: um prato tem no máximo uma ficha técnica (prato_id é UNIQUE no banco)
@Entity @Table(name = "ficha_tecnica")
public class FichaTecnica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "prato_id") private Prato prato;
    private Integer rendimento = 1;   // quantas porções a receita rende
    private String modoPreparo;       // RF-014
    private LocalDateTime createdAt = LocalDateTime.now();
    // orphanRemoval: item tirado da lista é apagado do banco junto (faz parte da ficha, não é um cadastro próprio)
    @OneToMany(mappedBy = "fichaTecnica", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("id ASC")
    private List<FichaTecnicaItem> itens = new ArrayList<>();

    public Long getId() { return id; }
    public Prato getPrato() { return prato; }                 public void setPrato(Prato v) { prato = v; }
    public Integer getRendimento() { return rendimento; }     public void setRendimento(Integer v) { rendimento = v; }
    public String getModoPreparo() { return modoPreparo; }    public void setModoPreparo(String v) { modoPreparo = v; }
    public List<FichaTecnicaItem> getItens() { return itens; }
}
