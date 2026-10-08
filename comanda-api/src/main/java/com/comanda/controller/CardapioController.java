package com.comanda.controller;

import com.comanda.dto.Dtos.PratoResponse;
import com.comanda.service.CardapioService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cardapio")
public class CardapioController {
    private final CardapioService service;
    public CardapioController(CardapioService service) { this.service = service; }

    @GetMapping
    public List<PratoResponse> listar(@RequestParam(required = false) String categoria) { return service.listar(categoria); }

    @GetMapping("/{id}")
    public PratoResponse buscar(@PathVariable Long id) { return service.buscar(id); }
}
