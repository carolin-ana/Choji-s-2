package com.comanda.repository;

import com.comanda.model.Prato;
import com.comanda.model.StatusRegistro;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PratoRepository extends JpaRepository<Prato, Long> {
    // RN09: o cardápio público só mostra prato ATIVO de categoria ATIVA, na ordem das categorias
    @Query("""
        select p from Prato p join fetch p.categoria c
        where p.status = :ativo and c.status = :ativo
        order by c.ordem, c.nome, p.nome
        """)
    List<Prato> cardapio(@Param("ativo") StatusRegistro ativo);

    @Query("""
        select p from Prato p join fetch p.categoria c
        where p.status = :ativo and c.status = :ativo and c.nome = :categoria
        order by p.nome
        """)
    List<Prato> cardapioPorCategoria(@Param("ativo") StatusRegistro ativo, @Param("categoria") String categoria);

    // Lista do painel (RF-010) com filtros e paginação. Sem filtro de categoria: todasCategorias = true.
    @EntityGraph(attributePaths = "categoria")
    @Query("""
        select p from Prato p join p.categoria c
        where p.status in :status
          and (:todasCategorias = true or c.id = :categoriaId)
          and lower(p.nome) like :busca
        """)
    Page<Prato> filtrarAdmin(@Param("status") List<StatusRegistro> status, @Param("todasCategorias") boolean todasCategorias,
                             @Param("categoriaId") Long categoriaId, @Param("busca") String busca, Pageable pageable);
}
