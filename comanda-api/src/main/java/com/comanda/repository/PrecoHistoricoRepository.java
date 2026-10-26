package com.comanda.repository;

import com.comanda.model.PrecoHistorico;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrecoHistoricoRepository extends JpaRepository<PrecoHistorico, Long> {
    // RF-026: evolução do preço de um ingrediente, em todos os fornecedores, da mais antiga para a mais nova
    @EntityGraph(attributePaths = "fornecedor")
    List<PrecoHistorico> findByIngredienteIdOrderByCreatedAtAscIdAsc(Long ingredienteId);
}
