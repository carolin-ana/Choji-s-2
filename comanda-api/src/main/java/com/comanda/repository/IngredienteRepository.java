package com.comanda.repository;

import com.comanda.model.Ingrediente;
import com.comanda.model.StatusRegistro;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Query("""
        select i from Ingrediente i
        where i.status in :status
          and (lower(i.nome) like :busca or lower(i.sku) like :busca)
        """)
    Page<Ingrediente> filtrar(@Param("status") List<StatusRegistro> status, @Param("busca") String busca, Pageable pageable);
}
