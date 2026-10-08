package com.comanda.security;

import com.comanda.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey chave;
    private final long horas;

    public JwtService(@Value("${app.jwt.secret}") String segredo, @Value("${app.jwt.expiration-hours}") long horas) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8)); // mínimo 32 caracteres
        this.horas = horas;
    }

    public String gerar(Usuario u) {
        Date agora = new Date();
        return Jwts.builder().subject(u.getEmail()).claim("role", u.getPerfil().name())
                .issuedAt(agora).expiration(new Date(agora.getTime() + horas * 3_600_000))
                .signWith(chave).compact();
    }

    public Claims ler(String token) { return Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload(); }
}
