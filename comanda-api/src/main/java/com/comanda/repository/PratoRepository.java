package com.comanda.repository;

import com.comanda.model.Prato;
import com.comanda.model.StatusRegistro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PratoRepository extends JpaRepository<Prato, Long> {
    List<Prato> findByStatus(StatusRegistro status);
    List<Prato> findByStatusAndCategoriaNome(StatusRegistro status, String categoria);
}
