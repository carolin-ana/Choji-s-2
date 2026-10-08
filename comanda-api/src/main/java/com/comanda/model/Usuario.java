package com.comanda.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String nome;
    @Column(unique = true) private String email;
    private String senhaHash;
    @Enumerated(EnumType.STRING) private Perfil perfil;
    private String telefone;
    private String endereco;
    @Enumerated(EnumType.STRING) private StatusRegistro status = StatusRegistro.ATIVO;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }           public void setNome(String v) { nome = v; }
    public String getEmail() { return email; }         public void setEmail(String v) { email = v; }
    public String getSenhaHash() { return senhaHash; } public void setSenhaHash(String v) { senhaHash = v; }
    public Perfil getPerfil() { return perfil; }       public void setPerfil(Perfil v) { perfil = v; }
    public String getEndereco() { return endereco; }   public void setEndereco(String v) { endereco = v; }
    public String getTelefone() { return telefone; }   public void setTelefone(String v) { telefone = v; }
    public StatusRegistro getStatus() { return status; }
}
