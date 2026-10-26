package com.comanda.repository;

import com.comanda.model.FichaTecnica;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {
    Optional<FichaTecnica> findByPratoId(Long pratoId);
    boolean existsByPratoId(Long pratoId);
    List<FichaTecnica> findByPratoIdIn(Collection<Long> pratoIds); // fichas de uma página inteira de pratos de uma vez

    // Fichas que usam algum destes ingredientes (para mostrar o novo food cost depois de receber uma compra)
    @Query("select distinct f from FichaTecnica f join f.itens i where i.ingrediente.id in :ids")
    List<FichaTecnica> usandoIngredientes(@Param("ids") Collection<Long> ids);
}
