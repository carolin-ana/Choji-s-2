package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.UsuarioRepository;
import com.comanda.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios; this.encoder = encoder; this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest r) {
        Usuario u = usuarios.findByEmail(r.email().trim().toLowerCase())
            .filter(x -> x.getStatus() == StatusRegistro.ATIVO && encoder.matches(r.senha(), x.getSenhaHash()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        return resposta(u);
    }

    public LoginResponse registrar(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado."); // RN10
        Usuario u = new Usuario();
        u.setNome(r.nome().trim()); u.setEmail(email); u.setSenhaHash(encoder.encode(r.senha()));
        u.setTelefone(r.telefone()); u.setEndereco(r.endereco().trim()); u.setPerfil(Perfil.CLIENTE); // cadastro público é sempre CLIENTE
        return resposta(usuarios.save(u));
    }

    private LoginResponse resposta(Usuario u) { return new LoginResponse(jwt.gerar(u), u.getPerfil(), u.getNome(), u.getEndereco()); }
}
