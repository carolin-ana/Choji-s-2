package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {
    // RF-016: a ordem do ciclo. CANCELADO fica de fora porque tem endpoint próprio (exige motivo).
    private static final List<StatusPedido> FLUXO = List.of(StatusPedido.RECEBIDO, StatusPedido.CONFIRMADO,
            StatusPedido.EM_PREPARO, StatusPedido.PRONTO, StatusPedido.SAIU_ENTREGA, StatusPedido.FINALIZADO);
    private static final int TAMANHO_MAXIMO_PAGINA = 50;

    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final PratoRepository pratos;

    public PedidoService(PedidoRepository pedidos, UsuarioRepository usuarios, PratoRepository pratos) {
        this.pedidos = pedidos; this.usuarios = usuarios; this.pratos = pratos;
    }

    // ================= Cliente =================

    @Transactional
    public PedidoResponse criar(String email, PedidoRequest r) {
        Usuario cliente = usuarios.findByEmail(email).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        Pedido p = new Pedido();
        p.setCliente(cliente); p.setStatus(StatusPedido.RECEBIDO); p.setEnderecoEntrega(r.enderecoEntrega().trim());
        p.getHistorico().add(new PedidoHistorico(p, StatusPedido.RECEBIDO, cliente, p.getCreatedAt())); // 1ª etapa da linha do tempo
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest i : r.itens()) {
            // o preço vem do banco, nunca do front
            Prato prato = pratos.findById(i.pratoId()).filter(x -> x.getStatus() == StatusRegistro.ATIVO)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Prato indisponível (id " + i.pratoId() + ")."));
            PedidoItem item = new PedidoItem();
            item.setPedido(p); item.setPrato(prato); item.setQuantidade(i.quantidade());
            item.setPrecoUnitario(prato.getPrecoVenda()); item.setObservacoes(i.observacoes());
            p.getItens().add(item);
            total = total.add(prato.getPrecoVenda().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        p.setValorTotal(total);
        return resposta(pedidos.save(p));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> meus(String email) {
        return pedidos.findByClienteEmailOrderByCreatedAtDesc(email).stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public StatusResponse status(Long id, String email) {
        return pedidos.findById(id).filter(p -> p.getCliente().getEmail().equals(email))
            .map(p -> new StatusResponse(p.getStatus()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    // ================= Painel (ADMIN / GERENTE / COZINHEIRO) =================

    // RF-015 e RF-019: todos os pedidos, mais recentes primeiro, com filtros opcionais e paginação
    @Transactional(readOnly = true)
    public PaginaResponse<PedidoAdminResponse> listarAdmin(StatusPedido status, CanalPedido canal,
                                                           LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        // filtro vazio = "todos"
        List<StatusPedido> statusAceitos = status == null ? List.of(StatusPedido.values()) : List.of(status);
        List<CanalPedido> canaisAceitos = canal == null ? List.of(CanalPedido.values()) : List.of(canal);
        LocalDateTime inicio = dataInicio == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : dataInicio.atStartOfDay();
        // "até o dia X" inclui o dia X inteiro: por isso o limite é o começo do dia seguinte
        LocalDateTime fim = dataFim == null ? LocalDateTime.of(2999, 1, 1, 0, 0) : dataFim.plusDays(1).atStartOfDay();
        if (!inicio.isBefore(fim)) throw new ApiException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser depois da data final.");

        // a ordem é sempre "mais recentes primeiro" e o tamanho da página tem um teto
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Pedido> r = pedidos.filtrar(statusAceitos, canaisAceitos, inicio, fim, pagina);
        List<PedidoAdminResponse> conteudo = r.getContent().stream().map(this::respostaAdmin).toList();
        return new PaginaResponse<>(conteudo, r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    // RF-020: detalhe do pedido
    @Transactional(readOnly = true)
    public PedidoAdminResponse buscarAdmin(Long id) { return respostaAdmin(buscar(id)); }

    // RF-016: só anda um passo por vez, na ordem do FLUXO
    @Transactional
    public PedidoAdminResponse mudarStatus(Long id, StatusPedido novo, String email) {
        Usuario quem = funcionario(email);
        Pedido p = buscar(id);
        StatusPedido atual = p.getStatus();
        if (novo == StatusPedido.CANCELADO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Para cancelar, use a ação de cancelamento e informe o motivo.");
        if (atual == StatusPedido.CANCELADO || atual == StatusPedido.FINALIZADO)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Este pedido já foi encerrado (" + atual + ").");
        StatusPedido proximo = FLUXO.get(FLUXO.indexOf(atual) + 1);
        if (novo != proximo)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Um pedido " + atual + " só pode ir para " + proximo + ".");
        // SRS 1.3: o cozinheiro só cuida do preparo (CONFIRMADO > EM_PREPARO > PRONTO)
        if (quem.getPerfil() == Perfil.COZINHEIRO && novo != StatusPedido.EM_PREPARO && novo != StatusPedido.PRONTO)
            throw new ApiException(HttpStatus.FORBIDDEN, "O cozinheiro só pode iniciar o preparo e marcar o pedido como pronto.");

        // BLOCO 6 (RF-017): quando novo == CONFIRMADO, a baixa de estoque entra AQUI, dentro desta mesma transação.
        registrar(p, novo, quem);
        return respostaAdmin(p); // o @Transactional grava a alteração ao sair do método
    }

    // RF-018 + RN04: motivo obrigatório; só GERENTE ou ADMIN cancela, em qualquer etapa antes de FINALIZADO
    @Transactional
    public PedidoAdminResponse cancelar(Long id, String motivo, String email) {
        Usuario quem = funcionario(email);
        Pedido p = buscar(id);
        StatusPedido atual = p.getStatus();
        if (atual == StatusPedido.CANCELADO) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Este pedido já está cancelado.");
        if (atual == StatusPedido.FINALIZADO) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Um pedido finalizado não pode ser cancelado.");
        if (quem.getPerfil() != Perfil.ADMIN && quem.getPerfil() != Perfil.GERENTE)
            throw new ApiException(HttpStatus.FORBIDDEN, "Só o gerente ou o admin pode cancelar um pedido.");

        // BLOCO 6 (RF-018): o estorno do estoque entra AQUI, dentro desta mesma transação.
        p.setMotivoCancelamento(motivo.trim());
        registrar(p, StatusPedido.CANCELADO, quem);
        return respostaAdmin(p);
    }

    // ================= Apoio =================

    // Muda o status e anota no histórico quando foi e quem fez (RF-020)
    private void registrar(Pedido p, StatusPedido novo, Usuario quem) {
        LocalDateTime agora = LocalDateTime.now();
        p.setStatus(novo);
        p.setUpdatedAt(agora);
        p.getHistorico().add(new PedidoHistorico(p, novo, quem, agora));
    }

    private Pedido buscar(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    // Quem está logado no painel. Lê do banco (e não só do token) para respeitar um usuário desativado.
    private Usuario funcionario(String email) {
        return usuarios.findByEmail(email).filter(u -> u.getStatus() == StatusRegistro.ATIVO)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    private List<ItemResponse> itens(Pedido p) {
        return p.getItens().stream().map(i -> new ItemResponse(i.getPrato().getNome(), i.getQuantidade(),
                i.getPrecoUnitario(), i.getObservacoes())).toList();
    }

    private PedidoResponse resposta(Pedido p) {
        return new PedidoResponse(p.getId(), p.getStatus(), p.getValorTotal(), p.getEnderecoEntrega(), p.getCreatedAt(), itens(p));
    }

    private PedidoAdminResponse respostaAdmin(Pedido p) {
        Usuario c = p.getCliente();
        List<HistoricoResponse> historico = p.getHistorico().stream().map(h -> new HistoricoResponse(h.getStatus(), h.getCreatedAt(),
                h.getUsuario() == null ? null : h.getUsuario().getNome())).toList();
        return new PedidoAdminResponse(p.getId(), p.getStatus(), p.getCanal(), p.getValorTotal(), p.getEnderecoEntrega(),
                p.getObservacoes(), p.getMotivoCancelamento(), p.getCreatedAt(), p.getUpdatedAt(),
                new ClienteResumo(c.getNome(), c.getTelefone(), c.getEmail()), itens(p), historico);
    }
}
