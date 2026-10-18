package com.comanda.repository;

import com.comanda.model.FichaTecnica;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {
    Optional<FichaTecnica> findByPratoId(Long pratoId);
    boolean existsByPratoId(Long pratoId);
    List<FichaTecnica> findByPratoIdIn(Collection<Long> pratoIds); // fichas de uma página inteira de pratos de uma vez
}
