package com.comanda.repository;

import com.comanda.model.Fornecedor;
import com.comanda.model.StatusRegistro;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    boolean existsByCnpj(String cnpj);
    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    // busca pela razão social, pelo CNPJ (sem pontuação) ou pelas categorias
    @Query("""
        select f from Fornecedor f
        where f.status in :status
          and (lower(f.razaoSocial) like :busca or lower(f.cnpj) like :buscaCnpj or lower(coalesce(f.categorias, '')) like :busca)
        """)
    Page<Fornecedor> filtrar(@Param("status") List<StatusRegistro> status, @Param("busca") String busca,
                             @Param("buscaCnpj") String buscaCnpj, Pageable pageable);
}
