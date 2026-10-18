package com.comanda.repository;

import com.comanda.model.FichaTecnicaItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaTecnicaItemRepository extends JpaRepository<FichaTecnicaItem, Long> {
    boolean existsByIngredienteId(Long ingredienteId); // o ingrediente já é usado em alguma ficha?
}
