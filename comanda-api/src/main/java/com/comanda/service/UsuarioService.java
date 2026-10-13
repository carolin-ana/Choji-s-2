package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-041: o ADMIN cria, edita, desativa e reativa os usuários internos (GERENTE e COZINHEIRO).
@Service
public class UsuarioService {
    // Perfis que aparecem na lista do painel. CLIENTE se cadastra sozinho e não entra aqui.
    private static final List<Perfil> INTERNOS = List.of(Perfil.ADMIN, Perfil.GERENTE, Perfil.COZINHEIRO);
    // Perfis que o ADMIN pode criar e alterar por esta tela. Contas ADMIN ficam protegidas.
    private static final List<Perfil> GERENCIAVEIS = List.of(Perfil.GERENTE, Perfil.COZINHEIRO);
    private static final int TAMANHO_MAXIMO_PAGINA = 50;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(Perfil perfil, StatusRegistro status, String busca, Pageable pageable) {
        if (perfil != null && !INTERNOS.contains(perfil))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Esta tela lista só usuários internos (admin, gerente e cozinheiro).");
        List<Perfil> perfis = perfil == null ? INTERNOS : List.of(perfil);
        List<StatusRegistro> statusAceitos = status == null ? List.of(StatusRegistro.values()) : List.of(status);
        String termo = (busca == null || busca.isBlank()) ? "%" : "%" + busca.trim().toLowerCase() + "%";
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                Sort.by("nome").and(Sort.by("id")));
        Page<Usuario> r = usuarios.filtrar(perfis, statusAceitos, termo, pagina);
        return new PaginaResponse<>(r.getContent().stream().map(this::resposta).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) { return resposta(interno(id)); }

    @Transactional
    public UsuarioResponse criar(NovoUsuarioRequest r) {
        exigirGerenciavel(r.perfil());
        String email = r.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado."); // RN10
        Usuario u = new Usuario();
        u.setNome(r.nome().trim()); u.setEmail(email); u.setPerfil(r.perfil());
        u.setTelefone(vazioParaNulo(r.telefone()));
        u.setSenhaHash(encoder.encode(r.senha())); // RNF01: só o hash BCrypt vai para o banco
        return resposta(usuarios.save(u));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, AtualizarUsuarioRequest r) {
        Usuario u = interno(id);
        exigirGerenciavel(u.getPerfil()); // não deixa mexer em contas ADMIN
        exigirGerenciavel(r.perfil());    // nem promover alguém a ADMIN ou CLIENTE
        String email = r.email().trim().toLowerCase();
        if (usuarios.existsByEmailAndIdNot(email, id)) throw new ApiException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado."); // RN10
        if (r.senha() != null && !r.senha().isBlank()) {
            if (r.senha().length() < 6) throw new ApiException(HttpStatus.BAD_REQUEST, "senha: a nova senha precisa ter ao menos 6 caracteres");
            u.setSenhaHash(encoder.encode(r.senha()));
        }
        u.setNome(r.nome().trim()); u.setEmail(email); u.setPerfil(r.perfil());
        u.setTelefone(vazioParaNulo(r.telefone()));
        u.setUpdatedAt(LocalDateTime.now());
        return resposta(u); // o @Transactional grava ao sair do método
    }

    // RN06: nunca apaga do banco; só marca como INATIVO (e aí o login deixa de funcionar)
    @Transactional
    public UsuarioResponse alterarStatus(Long id, StatusRegistro novo) {
        Usuario u = interno(id);
        exigirGerenciavel(u.getPerfil());
        u.setStatus(novo);
        u.setUpdatedAt(LocalDateTime.now());
        return resposta(u);
    }

    private Usuario interno(Long id) {
        return usuarios.findById(id).filter(u -> INTERNOS.contains(u.getPerfil()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private void exigirGerenciavel(Perfil p) {
        if (!GERENCIAVEIS.contains(p))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Por aqui só é possível gerenciar gerentes e cozinheiros.");
    }

    private static String vazioParaNulo(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    private UsuarioResponse resposta(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPerfil(), u.getStatus(), u.getCreatedAt());
    }
}
