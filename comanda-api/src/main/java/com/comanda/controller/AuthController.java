package com.comanda.controller;

import static com.comanda.dto.Dtos.*;

import com.comanda.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) { return service.login(r); }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest r) { return service.registrar(r); }
}
