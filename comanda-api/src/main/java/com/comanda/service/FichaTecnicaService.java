package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.*;
import com.comanda.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-011 a RF-013 + RN02 e RN08. O cálculo do custo fica SÓ aqui (o front nunca calcula, só mostra).
@Service
public class FichaTecnicaService {
    private static final BigDecimal LIMITE_VERDE = new BigDecimal("30");
    private static final BigDecimal LIMITE_AMARELO = new BigDecimal("35");
    private static final BigDecimal CEM = new BigDecimal("100");

    private final FichaTecnicaRepository fichas;
    private final PratoRepository pratos;
    private final IngredienteRepository ingredientes;

    public FichaTecnicaService(FichaTecnicaRepository fichas, PratoRepository pratos, IngredienteRepository ingredientes) {
        this.fichas = fichas; this.pratos = pratos; this.ingredientes = ingredientes;
    }

    @Transactional(readOnly = true)
    public FichaResponse buscar(Long pratoId) {
        Prato p = prato(pratoId);
        return resposta(p, ficha(pratoId));
    }

    // GET /api/admin/pratos/{id}/custo
    @Transactional(readOnly = true)
    public CustoResponse custoDoPrato(Long pratoId) {
        Prato p = prato(pratoId);
        return custo(ficha(pratoId), p.getPrecoVenda());
    }

    @Transactional
    public FichaResponse criar(Long pratoId, FichaRequest r) {
        Prato p = prato(pratoId);
        if (fichas.existsByPratoId(pratoId))
            throw new ApiException(HttpStatus.CONFLICT, "Este prato já tem ficha técnica. Use a edição.");
        FichaTecnica f = new FichaTecnica();
        f.setPrato(p);
        preencher(f, r, Set.of());
        return resposta(p, fichas.save(f));
    }

    @Transactional
    public FichaResponse atualizar(Long pratoId, FichaRequest r) {
        Prato p = prato(pratoId);
        FichaTecnica f = ficha(pratoId);
        preencher(f, r, idsDosIngredientes(f));
        return resposta(p, f); // o @Transactional grava ao sair do método
    }

    // Calcula o custo de uma ficha que ainda está sendo editada, sem salvar nada.
    // A tela chama isto enquanto a pessoa digita, para mostrar o custo "em tempo real" (RF-012).
    @Transactional(readOnly = true)
    public FichaResponse simular(Long pratoId, FichaRequest r) {
        Prato p = prato(pratoId);
        Set<Long> jaNaFicha = fichas.findByPratoId(pratoId).map(this::idsDosIngredientes).orElse(Set.of());
        FichaTecnica rascunho = new FichaTecnica(); // objeto só em memória, nunca vai para o banco
        rascunho.setPrato(p);
        preencher(rascunho, r, jaNaFicha);
        return resposta(p, rascunho);
    }

    // Custo de vários pratos de uma vez (lista do painel). Prato sem ficha não aparece no mapa.
    @Transactional(readOnly = true)
    public Map<Long, CustoResponse> custosPorPrato(List<Prato> lista) {
        if (lista.isEmpty()) return Map.of();
        Map<Long, BigDecimal> precos = lista.stream().collect(Collectors.toMap(Prato::getId, Prato::getPrecoVenda));
        return fichas.findByPratoIdIn(precos.keySet()).stream().collect(Collectors.toMap(
                f -> f.getPrato().getId(), f -> custo(f, precos.get(f.getPrato().getId()))));
    }

    // RF-012: custo = SUM(quantidade × fator de correção × custo unitário) / rendimento
    // RF-013: food cost = custo da porção / preço de venda × 100
    public CustoResponse custo(FichaTecnica f, BigDecimal precoVenda) {
        BigDecimal total = f.getItens().stream().map(this::custoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        int rendimento = (f.getRendimento() == null || f.getRendimento() < 1) ? 1 : f.getRendimento();
        BigDecimal porcao = total.divide(BigDecimal.valueOf(rendimento), 2, RoundingMode.HALF_UP);
        BigDecimal foodCost = precoVenda.signum() > 0
                ? porcao.multiply(CEM).divide(precoVenda, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        FaixaFoodCost faixa = faixa(foodCost);
        String alerta = faixa == FaixaFoodCost.VERMELHO // RN02: só avisa, não impede de salvar
                ? "Food cost de " + foodCost.toPlainString().replace('.', ',') + "% está acima de 35%: revise a ficha técnica ou o preço de venda."
                : null;
        return new CustoResponse(total, rendimento, porcao, precoVenda, foodCost, faixa, alerta);
    }

    // Até 30% verde, até 35% amarelo, acima vermelho (usado também pelo food cost médio do dashboard)
    public static FaixaFoodCost faixa(BigDecimal foodCost) {
        return foodCost.compareTo(LIMITE_VERDE) <= 0 ? FaixaFoodCost.VERDE
                : foodCost.compareTo(LIMITE_AMARELO) <= 0 ? FaixaFoodCost.AMARELO : FaixaFoodCost.VERMELHO;
    }

    private BigDecimal custoItem(FichaTecnicaItem i) {
        return i.getQuantidade().multiply(i.getFatorCorrecao()).multiply(i.getIngrediente().getCustoUnitario())
                .setScale(4, RoundingMode.HALF_UP);
    }

    // Troca os itens da ficha pelos que vieram na requisição. Ingrediente inativo só pode
    // continuar se já estava na ficha (para não travar a edição de fichas antigas).
    private void preencher(FichaTecnica f, FichaRequest r, Set<Long> permitidosInativos) {
        Set<Long> vistos = new HashSet<>();
        List<FichaTecnicaItem> novos = new ArrayList<>();
        for (FichaItemRequest i : r.itens()) {
            if (!vistos.add(i.ingredienteId()))
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "O mesmo ingrediente aparece duas vezes na ficha.");
            Ingrediente ing = ingredientes.findById(i.ingredienteId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Ingrediente não encontrado (id " + i.ingredienteId() + ")."));
            if (ing.getStatus() != StatusRegistro.ATIVO && !permitidosInativos.contains(ing.getId()))
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "O ingrediente " + ing.getNome() + " está inativo.");
            FichaTecnicaItem item = new FichaTecnicaItem();
            item.setFichaTecnica(f);
            item.setIngrediente(ing);
            item.setQuantidade(i.quantidade());
            item.setUnidade(ing.getUnidadePadrao());
            item.setFatorCorrecao(i.fatorCorrecao());
            novos.add(item);
        }
        f.setRendimento(r.rendimento());
        f.setModoPreparo(r.modoPreparo() == null || r.modoPreparo().isBlank() ? null : r.modoPreparo().trim());
        f.getItens().clear();
        f.getItens().addAll(novos);
    }

    private Set<Long> idsDosIngredientes(FichaTecnica f) {
        return f.getItens().stream().map(i -> i.getIngrediente().getId()).collect(Collectors.toSet());
    }

    private Prato prato(Long id) {
        return pratos.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prato não encontrado."));
    }

    private FichaTecnica ficha(Long pratoId) {
        return fichas.findByPratoId(pratoId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Este prato ainda não tem ficha técnica."));
    }

    private FichaResponse resposta(Prato p, FichaTecnica f) {
        List<FichaItemResponse> itens = f.getItens().stream().map(i -> new FichaItemResponse(
                i.getIngrediente().getId(), i.getIngrediente().getNome(), i.getUnidade(), i.getQuantidade(),
                i.getFatorCorrecao(), i.getIngrediente().getCustoUnitario(), custoItem(i))).toList();
        return new FichaResponse(p.getId(), p.getNome(), f.getRendimento(), f.getModoPreparo(), itens, custo(f, p.getPrecoVenda()));
    }
}
