package com.comanda.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// RF-021. O CNPJ fica guardado sem pontuação (14 caracteres); a tela é que coloca a máscara.
@Entity @Table(name = "fornecedor")
public class Fornecedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String razaoSocial;
    private String cnpj;
    private String telefone;
    private String email;
    private String categorias;   // categorias de produtos que ele vende, em texto (ex.: "Pescados, carnes")
    @Enumerated(EnumType.STRING) private StatusRegistro status = StatusRegistro.ATIVO;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getRazaoSocial() { return razaoSocial; }   public void setRazaoSocial(String v) { razaoSocial = v; }
    public String getCnpj() { return cnpj; }                 public void setCnpj(String v) { cnpj = v; }
    public String getTelefone() { return telefone; }         public void setTelefone(String v) { telefone = v; }
    public String getEmail() { return email; }               public void setEmail(String v) { email = v; }
    public String getCategorias() { return categorias; }     public void setCategorias(String v) { categorias = v; }
    public StatusRegistro getStatus() { return status; }     public void setStatus(StatusRegistro v) { status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
