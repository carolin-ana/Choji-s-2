package com.comanda.dto;

import com.comanda.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}
    public record RegisterRequest(@NotBlank String nome, @NotBlank @Email String email, @NotBlank @Size(min = 6) String senha,
                                  @NotBlank String telefone, @NotBlank String endereco) {}
    public record LoginResponse(String token, Perfil perfil, String nome, String endereco) {}
    public record PratoResponse(Long id, String nome, String descricao, String fotoUrl, BigDecimal precoVenda,
                                Integer tempoPreparoMin, String categoria, StatusRegistro status) {}
    public record ItemRequest(@NotNull Long pratoId, @Min(1) int quantidade, @Size(max = 255) String observacoes) {}
    public record PedidoRequest(@NotBlank String enderecoEntrega, @NotEmpty @Valid List<ItemRequest> itens) {}
    public record ItemResponse(String nome, int quantidade, BigDecimal precoUnitario, String observacoes) {}
    public record PedidoResponse(Long id, StatusPedido status, BigDecimal valorTotal, String enderecoEntrega,
                                 LocalDateTime createdAt, List<ItemResponse> itens) {}
    public record StatusResponse(StatusPedido status) {}

    // ---- Painel (bloco 5) ----
    // Formato de toda listagem paginada (RNF08): os itens da página + os números para o front montar a paginação
    public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
    public record ClienteResumo(String nome, String telefone, String email) {}
    // Uma etapa da linha do tempo: qual status, quando e quem fez
    public record HistoricoResponse(StatusPedido status, LocalDateTime createdAt, String usuario) {}
    public record PedidoAdminResponse(Long id, StatusPedido status, CanalPedido canal, BigDecimal valorTotal,
                                      String enderecoEntrega, String observacoes, String motivoCancelamento,
                                      LocalDateTime createdAt, LocalDateTime updatedAt,
                                      ClienteResumo cliente, List<ItemResponse> itens,
                                      List<HistoricoResponse> historico) {}
    public record StatusRequest(@NotNull StatusPedido status) {}
    public record CancelarRequest(@NotBlank @Size(max = 255) String motivo) {}
}
