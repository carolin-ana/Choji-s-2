package com.comanda.repository;

import com.comanda.model.Perfil;
import com.comanda.model.StatusRegistro;
import com.comanda.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id); // e-mail já usado por OUTRO usuário (edição)

    // RF-041: lista paginada de usuários internos. O Service sempre manda valores preenchidos
    // (todos os perfis internos / todos os status / busca "%" quando o filtro vem vazio).
    @Query("""
        select u from Usuario u
        where u.perfil in :perfis
          and u.status in :status
          and (lower(u.nome) like :busca or lower(u.email) like :busca)
        """)
    Page<Usuario> filtrar(@Param("perfis") List<Perfil> perfis, @Param("status") List<StatusRegistro> status,
                          @Param("busca") String busca, Pageable pageable);
}
