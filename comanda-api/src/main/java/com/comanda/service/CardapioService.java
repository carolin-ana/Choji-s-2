package com.comanda.service;

import static com.comanda.dto.Dtos.PratoResponse;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.PratoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardapioService {
    private final PratoRepository pratos;
    public CardapioService(PratoRepository pratos) { this.pratos = pratos; }

    @Transactional(readOnly = true)
    public List<PratoResponse> listar(String categoria) { // RN09: só pratos ATIVO
        var lista = (categoria == null || categoria.isBlank())
            ? pratos.findByStatus(StatusRegistro.ATIVO)
            : pratos.findByStatusAndCategoriaNome(StatusRegistro.ATIVO, categoria);
        return lista.stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public PratoResponse buscar(Long id) {
        return pratos.findById(id).filter(p -> p.getStatus() == StatusRegistro.ATIVO).map(this::resposta)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prato não encontrado."));
    }

    private PratoResponse resposta(Prato p) {
        return new PratoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getFotoUrl(), p.getPrecoVenda(),
                p.getTempoPreparoMin(), p.getCategoria().getNome(), p.getStatus());
    }
}
