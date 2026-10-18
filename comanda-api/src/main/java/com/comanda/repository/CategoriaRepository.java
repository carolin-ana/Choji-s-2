package com.comanda.repository;

import com.comanda.model.Categoria;
import com.comanda.model.StatusRegistro;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    @Query("select c from Categoria c where c.status in :status and lower(c.nome) like :busca")
    Page<Categoria> filtrar(@Param("status") List<StatusRegistro> status, @Param("busca") String busca, Pageable pageable);
}
