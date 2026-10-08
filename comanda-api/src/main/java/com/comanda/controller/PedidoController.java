package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@Valid @RequestBody PedidoRequest r, Authentication auth) { return service.criar(auth.getName(), r); }

    @GetMapping("/meus")
    public List<PedidoResponse> meus(Authentication auth) { return service.meus(auth.getName()); }

    @GetMapping("/{id}/status")
    public StatusResponse status(@PathVariable Long id, Authentication auth) { return service.status(id, auth.getName()); }
}
