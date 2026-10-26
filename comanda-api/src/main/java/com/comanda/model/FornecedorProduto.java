package com.comanda.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// RF-022: "este fornecedor vende este ingrediente a este preço".
// O preço é sempre por UNIDADE PADRÃO do ingrediente (por g, ml, un...), igual ao custo unitário do ingrediente,
// para a cotação comparar maçã com maçã. unidadeVenda é só a descrição da embalagem (ex.: "pacote de 5 kg").
@Entity @Table(name = "fornecedor_produto")
public class FornecedorProduto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Fornecedor fornecedor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Ingrediente ingrediente;
    private BigDecimal preco;
    private String unidadeVenda;
    @Enumerated(EnumType.STRING) private StatusRegistro status = StatusRegistro.ATIVO; // RN06: sair do catálogo = INATIVO
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Fornecedor getFornecedor() { return fornecedor; }     public void setFornecedor(Fornecedor v) { fornecedor = v; }
    public Ingrediente getIngrediente() { return ingrediente; }  public void setIngrediente(Ingrediente v) { ingrediente = v; }
    public BigDecimal getPreco() { return preco; }               public void setPreco(BigDecimal v) { preco = v; }
    public String getUnidadeVenda() { return unidadeVenda; }     public void setUnidadeVenda(String v) { unidadeVenda = v; }
    public StatusRegistro getStatus() { return status; }         public void setStatus(StatusRegistro v) { status = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
