package com.comanda.dto;

import com.comanda.model.*;
import com.comanda.validation.CnpjValido;
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

    // ---- Usuários internos (bloco 10, RF-041) ----
    // Nunca devolve senha nem hash (RNF01)
    public record UsuarioResponse(Long id, String nome, String email, String telefone, Perfil perfil,
                                  StatusRegistro status, LocalDateTime createdAt) {}
    public record NovoUsuarioRequest(@NotBlank @Size(max = 120) String nome, @NotBlank @Email @Size(max = 160) String email,
                                     @NotBlank @Size(min = 6, max = 72) String senha, @NotNull Perfil perfil,
                                     @Size(max = 30) String telefone) {}
    // senha vazia/nula na edição = mantém a senha atual
    public record AtualizarUsuarioRequest(@NotBlank @Size(max = 120) String nome, @NotBlank @Email @Size(max = 160) String email,
                                          @Size(max = 72) String senha, @NotNull Perfil perfil,
                                          @Size(max = 30) String telefone) {}

    // ---- Cardápio do admin (blocos 3 e 4) ----
    public record CategoriaRequest(@NotBlank @Size(max = 80) String nome, @Size(max = 255) String descricao,
                                   @NotNull @Min(0) @Max(999) Integer ordem) {}
    public record CategoriaResponse(Long id, String nome, String descricao, Integer ordem, StatusRegistro status) {}

    public record IngredienteRequest(@NotBlank @Size(max = 120) String nome, @NotBlank @Size(max = 40) String sku,
                                     @NotNull UnidadeMedida unidadePadrao,
                                     @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 3) BigDecimal estoqueMinimo,
                                     @NotNull @DecimalMin("0") @Digits(integer = 6, fraction = 4) BigDecimal custoUnitario) {}
    public record IngredienteResponse(Long id, String nome, String sku, UnidadeMedida unidadePadrao, BigDecimal estoqueMinimo,
                                      BigDecimal custoUnitario, StatusRegistro status) {}

    public record PratoRequest(@NotBlank @Size(max = 120) String nome, @Size(max = 500) String descricao, @Size(max = 255) String fotoUrl,
                               @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precoVenda,
                               @NotNull @Min(1) @Max(600) Integer tempoPreparoMin, @NotNull Long categoriaId,
                               @NotNull StatusRegistro status) {}
    // custo = null quando o prato ainda não tem ficha técnica
    public record PratoAdminResponse(Long id, String nome, String descricao, String fotoUrl, BigDecimal precoVenda,
                                     Integer tempoPreparoMin, Long categoriaId, String categoria, StatusRegistro status,
                                     boolean temFicha, CustoResponse custo) {}

    // RN08: fator de correção nunca menor que 1,0
    public record FichaItemRequest(@NotNull Long ingredienteId,
                                   @NotNull @DecimalMin(value = "0.001", message = "a quantidade precisa ser maior que zero")
                                   @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
                                   @NotNull @DecimalMin(value = "1.0", message = "o fator de correção não pode ser menor que 1,0")
                                   @Digits(integer = 3, fraction = 2) BigDecimal fatorCorrecao) {}
    public record FichaRequest(@NotNull @Min(1) @Max(1000) Integer rendimento, @Size(max = 5000) String modoPreparo,
                               @NotEmpty(message = "a ficha precisa de pelo menos 1 ingrediente") @Valid List<FichaItemRequest> itens) {}
    public record FichaItemResponse(Long ingredienteId, String ingrediente, UnidadeMedida unidade, BigDecimal quantidade,
                                    BigDecimal fatorCorrecao, BigDecimal custoUnitario, BigDecimal custo) {}
    // RF-012 e RF-013. alerta preenchido quando o food cost passa de 35% (RN02: avisa, não bloqueia)
    public record CustoResponse(BigDecimal custoTotal, Integer rendimento, BigDecimal custoPorcao, BigDecimal precoVenda,
                                BigDecimal foodCost, FaixaFoodCost faixa, String alerta) {}
    public record FichaResponse(Long pratoId, String prato, Integer rendimento, String modoPreparo,
                                List<FichaItemResponse> itens, CustoResponse custo) {}

    // ---- Estoque (blocos 6 e 8) ----
    public record EstoqueSaldoResponse(Long ingredienteId, String ingrediente, String sku, UnidadeMedida unidade, BigDecimal saldo,
                                       BigDecimal estoqueMinimo, BigDecimal custoUnitario, boolean abaixoMinimo, StatusRegistro status) {}
    // Movimentação manual: entrada de ajuste (RF-028) ou saída com motivo obrigatório (RF-030)
    public record MovimentacaoRequest(@NotNull Long ingredienteId, @NotNull TipoMovimentacao tipo, @NotNull MotivoMovimentacao motivo,
                                      @NotNull @DecimalMin(value = "0.001", message = "a quantidade precisa ser maior que zero")
                                      @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
                                      @Size(max = 60) String lote, java.time.LocalDate validade,
                                      @DecimalMin("0") @Digits(integer = 6, fraction = 4) BigDecimal custoUnitario,
                                      @Size(max = 255) String observacao) {}
    public record MovimentacaoResponse(Long id, LocalDateTime createdAt, Long ingredienteId, String ingrediente, UnidadeMedida unidade,
                                       TipoMovimentacao tipo, MotivoMovimentacao motivo, BigDecimal quantidade, String lote,
                                       java.time.LocalDate validade, BigDecimal custoUnitario, Long pedidoId, Long pedidoCompraId,
                                       String usuario, String observacao) {}

    // ---- Fornecedores e compras (bloco 7) ----
    // RN07: o CNPJ pode vir com ou sem pontuação; o Service guarda só os 14 caracteres
    public record FornecedorRequest(@NotBlank @Size(max = 150) String razaoSocial, @NotBlank @CnpjValido String cnpj,
                                    @Size(max = 30) String telefone, @Email @Size(max = 160) String email,
                                    @Size(max = 255) String categorias) {}
    public record FornecedorResponse(Long id, String razaoSocial, String cnpj, String cnpjFormatado, String telefone, String email,
                                     String categorias, StatusRegistro status, long itensCatalogo) {}
    // RF-022: preço por unidade padrão do ingrediente (por g, ml, un...); unidadeVenda descreve a embalagem
    public record CatalogoItemRequest(@NotNull Long ingredienteId,
                                      @NotNull @DecimalMin(value = "0.0001", message = "o preço precisa ser maior que zero")
                                      @Digits(integer = 6, fraction = 4) BigDecimal preco,
                                      @Size(max = 60) String unidadeVenda) {}
    public record CatalogoPrecoRequest(@NotNull @DecimalMin(value = "0.0001", message = "o preço precisa ser maior que zero")
                                       @Digits(integer = 6, fraction = 4) BigDecimal preco,
                                       @Size(max = 60) String unidadeVenda) {}
    public record CatalogoItemResponse(Long id, Long ingredienteId, String ingrediente, String sku, UnidadeMedida unidade,
                                       BigDecimal preco, String unidadeVenda, BigDecimal custoAtual, StatusRegistro status,
                                       LocalDateTime updatedAt) {}
    // RF-023: diferencaPercentual = quanto este preço está acima do mais barato (0 para o mais barato)
    public record CotacaoItemResponse(Long fornecedorId, String fornecedor, String telefone, String email, BigDecimal preco,
                                      String unidadeVenda, BigDecimal diferencaPercentual, boolean maisBarato, LocalDateTime atualizadoEm) {}
    public record CotacaoResponse(Long ingredienteId, String ingrediente, String sku, UnidadeMedida unidade, BigDecimal custoAtual,
                                  BigDecimal saldo, BigDecimal estoqueMinimo, boolean abaixoMinimo, List<CotacaoItemResponse> fornecedores) {}
    // RF-026
    public record PrecoHistoricoResponse(Long fornecedorId, String fornecedor, BigDecimal preco, OrigemPreco origem,
                                         Long pedidoCompraId, LocalDateTime data) {}
    // RF-024: precoUnitario vazio = usa o preço do catálogo do fornecedor
    public record CompraItemRequest(@NotNull Long ingredienteId,
                                    @NotNull @DecimalMin(value = "0.001", message = "a quantidade precisa ser maior que zero")
                                    @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
                                    @DecimalMin(value = "0.0001", message = "o preço precisa ser maior que zero")
                                    @Digits(integer = 6, fraction = 4) BigDecimal precoUnitario) {}
    public record CompraRequest(@NotNull Long fornecedorId, @Size(max = 500) String observacoes,
                                @NotEmpty(message = "o pedido precisa de pelo menos 1 item") @Valid List<CompraItemRequest> itens) {}
    public record CompraItemResponse(Long ingredienteId, String ingrediente, UnidadeMedida unidade, BigDecimal quantidade,
                                     BigDecimal precoUnitario, BigDecimal subtotal) {}
    public record CompraResponse(Long id, StatusCompra status, Long fornecedorId, String fornecedor, BigDecimal valorTotal,
                                 String observacoes, String motivoCancelamento, String usuario, LocalDateTime createdAt,
                                 LocalDateTime enviadoEm, LocalDateTime recebidoEm, List<CompraItemResponse> itens) {}
    // RF-025: lote e validade valem para todos os itens recebidos (todos opcionais)
    public record ReceberRequest(@Size(max = 60) String lote, java.time.LocalDate validade, @Size(max = 255) String observacao) {}
    public record CustoAlteradoResponse(Long ingredienteId, String ingrediente, UnidadeMedida unidade,
                                        BigDecimal custoAnterior, BigDecimal custoNovo) {}
    public record PratoAfetadoResponse(Long pratoId, String prato, BigDecimal foodCost, FaixaFoodCost faixa) {}
    // O que mudou com o recebimento: custos novos (RN05) e o food cost recalculado dos pratos que usam esses ingredientes
    public record RecebimentoResponse(CompraResponse compra, List<CustoAlteradoResponse> custos, List<PratoAfetadoResponse> pratos) {}
}
