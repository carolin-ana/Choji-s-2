package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "ficha_tecnica_item")
public class FichaTecnicaItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private FichaTecnica fichaTecnica;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Ingrediente ingrediente;
    private BigDecimal quantidade;
    @Enumerated(EnumType.STRING) private UnidadeMedida unidade;  // sempre a unidade padrão do ingrediente
    private BigDecimal fatorCorrecao = BigDecimal.ONE;            // perda no preparo (RN08: nunca menor que 1,0)

    public FichaTecnica getFichaTecnica() { return fichaTecnica; }   public void setFichaTecnica(FichaTecnica v) { fichaTecnica = v; }
    public Ingrediente getIngrediente() { return ingrediente; }      public void setIngrediente(Ingrediente v) { ingrediente = v; }
    public BigDecimal getQuantidade() { return quantidade; }         public void setQuantidade(BigDecimal v) { quantidade = v; }
    public UnidadeMedida getUnidade() { return unidade; }            public void setUnidade(UnidadeMedida v) { unidade = v; }
    public BigDecimal getFatorCorrecao() { return fatorCorrecao; }   public void setFatorCorrecao(BigDecimal v) { fatorCorrecao = v; }
}
