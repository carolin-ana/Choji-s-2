package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final PratoRepository pratos;

    public PedidoService(PedidoRepository pedidos, UsuarioRepository usuarios, PratoRepository pratos) {
        this.pedidos = pedidos; this.usuarios = usuarios; this.pratos = pratos;
    }

    @Transactional
    public PedidoResponse criar(String email, PedidoRequest r) {
        Usuario cliente = usuarios.findByEmail(email).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        Pedido p = new Pedido();
        p.setCliente(cliente); p.setStatus(StatusPedido.RECEBIDO); p.setEnderecoEntrega(r.enderecoEntrega().trim());
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

    private PedidoResponse resposta(Pedido p) {
        var itens = p.getItens().stream().map(i -> new ItemResponse(i.getPrato().getNome(), i.getQuantidade(),
                i.getPrecoUnitario(), i.getObservacoes())).toList();
        return new PedidoResponse(p.getId(), p.getStatus(), p.getValorTotal(), p.getEnderecoEntrega(), p.getCreatedAt(), itens);
    }
}
