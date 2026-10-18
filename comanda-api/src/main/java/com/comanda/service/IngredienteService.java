package com.comanda.service;

import static com.comanda.dto.Dtos.*;

import com.comanda.exception.ApiException;
import com.comanda.model.Ingrediente;
import com.comanda.model.StatusRegistro;
import com.comanda.repository.FichaTecnicaItemRepository;
import com.comanda.repository.IngredienteRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-027: cadastro de ingredientes. Mudar o custo unitário muda na hora o custo das fichas que usam o ingrediente.
@Service
public class IngredienteService {
    private static final int TAMANHO_MAXIMO_PAGINA = 200;
    private final IngredienteRepository ingredientes;
    private final FichaTecnicaItemRepository itensFicha;

    public IngredienteService(IngredienteRepository ingredientes, FichaTecnicaItemRepository itensFicha) {
        this.ingredientes = ingredientes; this.itensFicha = itensFicha;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<IngredienteResponse> listar(StatusRegistro status, String busca, Pageable pageable) {
        List<StatusRegistro> statusAceitos = status == null ? List.of(StatusRegistro.values()) : List.of(status);
        String termo = (busca == null || busca.isBlank()) ? "%" : "%" + busca.trim().toLowerCase() + "%";
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA),
                Sort.by("nome").and(Sort.by("id")));
        Page<Ingrediente> r = ingredientes.filtrar(statusAceitos, termo, pagina);
        return new PaginaResponse<>(r.getContent().stream().map(this::resposta).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional(readOnly = true)
    public IngredienteResponse buscar(Long id) { return resposta(ingrediente(id)); }

    @Transactional
    public IngredienteResponse criar(IngredienteRequest r) {
        String sku = r.sku().trim().toUpperCase();
        if (ingredientes.existsBySkuIgnoreCase(sku)) throw new ApiException(HttpStatus.CONFLICT, "Já existe um ingrediente com este SKU.");
        Ingrediente i = new Ingrediente();
        preencher(i, r, sku);
        return resposta(ingredientes.save(i));
    }

    @Transactional
    public IngredienteResponse atualizar(Long id, IngredienteRequest r) {
        Ingrediente i = ingrediente(id);
        String sku = r.sku().trim().toUpperCase();
        if (ingredientes.existsBySkuIgnoreCaseAndIdNot(sku, id)) throw new ApiException(HttpStatus.CONFLICT, "Já existe um ingrediente com este SKU.");
        // as quantidades das fichas estão na unidade antiga: trocar a unidade deixaria todos os custos errados
        if (i.getUnidadePadrao() != r.unidadePadrao() && itensFicha.existsByIngredienteId(id))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Este ingrediente já é usado em fichas técnicas, então a unidade não pode mudar.");
        preencher(i, r, sku);
        return resposta(i);
    }

    @Transactional
    public IngredienteResponse alterarStatus(Long id, StatusRegistro novo) {
        Ingrediente i = ingrediente(id);
        i.setStatus(novo);
        return resposta(i);
    }

    private void preencher(Ingrediente i, IngredienteRequest r, String sku) {
        i.setNome(r.nome().trim());
        i.setSku(sku);
        i.setUnidadePadrao(r.unidadePadrao());
        i.setEstoqueMinimo(r.estoqueMinimo());
        i.setCustoUnitario(r.custoUnitario());
    }

    private Ingrediente ingrediente(Long id) {
        return ingredientes.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ingrediente não encontrado."));
    }

    private IngredienteResponse resposta(Ingrediente i) {
        return new IngredienteResponse(i.getId(), i.getNome(), i.getSku(), i.getUnidadePadrao(), i.getEstoqueMinimo(),
                i.getCustoUnitario(), i.getStatus());
    }
}
