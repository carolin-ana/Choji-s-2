package com.comanda.repository;

import com.comanda.model.FornecedorProduto;
import com.comanda.model.StatusRegistro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FornecedorProdutoRepository extends JpaRepository<FornecedorProduto, Long> {
    // RF-022: catálogo de um fornecedor, em ordem alfabética de ingrediente
    @EntityGraph(attributePaths = "ingrediente")
    @Query("select p from FornecedorProduto p where p.fornecedor.id = :fornecedorId and p.status in :status order by p.ingrediente.nome")
    List<FornecedorProduto> catalogo(@Param("fornecedorId") Long fornecedorId, @Param("status") List<StatusRegistro> status);

    Optional<FornecedorProduto> findByFornecedorIdAndIngredienteId(Long fornecedorId, Long ingredienteId);

    // RF-023: quem vende este ingrediente (fornecedor e item do catálogo ativos), do mais barato para o mais caro
    @EntityGraph(attributePaths = "fornecedor")
    @Query("""
        select p from FornecedorProduto p
        where p.ingrediente.id = :ingredienteId and p.status = :ativo and p.fornecedor.status = :ativo
        order by p.preco asc, p.fornecedor.razaoSocial asc
        """)
    List<FornecedorProduto> cotacao(@Param("ingredienteId") Long ingredienteId, @Param("ativo") StatusRegistro ativo);

    // quantos itens ativos cada fornecedor tem no catálogo (coluna da lista). Cada linha = [id do fornecedor, total].
    @Query("""
        select p.fornecedor.id, count(p) from FornecedorProduto p
        where p.fornecedor.id in :ids and p.status = :ativo
        group by p.fornecedor.id
        """)
    List<Object[]> contarAtivos(@Param("ids") List<Long> ids, @Param("ativo") StatusRegistro ativo);
}
