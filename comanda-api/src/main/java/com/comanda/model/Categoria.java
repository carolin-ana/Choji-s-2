package com.comanda.model;

import jakarta.persistence.*;

@Entity @Table(name = "categoria")
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String nome;
    public Long getId() { return id; }
    public String getNome() { return nome; }
}
