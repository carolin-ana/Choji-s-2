package com.comanda.model;

// COMPRA (recebimento, bloco 7), VENDA (baixa automática do pedido), ESTORNO (pedido cancelado)
// e os motivos de saída manual do RF-030: DESPERDICIO, VENCIMENTO, QUEBRA, USO_INTERNO. AJUSTE = correção de inventário.
public enum MotivoMovimentacao { COMPRA, VENDA, DESPERDICIO, VENCIMENTO, QUEBRA, USO_INTERNO, AJUSTE, ESTORNO }
