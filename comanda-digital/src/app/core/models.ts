export interface Prato { id: number; nome: string; descricao: string; fotoUrl: string; precoVenda: number; tempoPreparoMin: number; categoria: string; status: 'ATIVO'|'INATIVO'|'PAUSADO'; }
export interface ItemCarrinho { prato: Prato; quantidade: number; observacoes: string; }
export type Perfil = 'ADMIN'|'GERENTE'|'COZINHEIRO'|'CLIENTE';
// Quem entra no painel administrativo (o CLIENTE fica só nas telas públicas)
export const EQUIPE: Perfil[] = ['ADMIN', 'GERENTE', 'COZINHEIRO'];
export const ROTULOS_PERFIL: Record<Perfil, string> = { ADMIN: 'Admin', GERENTE: 'Gerente', COZINHEIRO: 'Cozinheiro', CLIENTE: 'Cliente' };
export interface LoginResponse { token: string; perfil: Perfil; nome: string; endereco?: string; }
export interface NovoCliente { nome: string; email: string; senha: string; telefone: string; endereco: string; }

export type StatusPedido = 'RECEBIDO'|'CONFIRMADO'|'EM_PREPARO'|'PRONTO'|'SAIU_ENTREGA'|'FINALIZADO'|'CANCELADO';
export const ETAPAS: StatusPedido[] = ['RECEBIDO', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO', 'SAIU_ENTREGA'];
export const ROTULOS: Record<StatusPedido, string> = {
  RECEBIDO: 'Recebido', CONFIRMADO: 'Confirmado', EM_PREPARO: 'Em preparo', PRONTO: 'Pronto',
  SAIU_ENTREGA: 'Saiu para entrega', FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado',
};
export interface PedidoItem { nome: string; quantidade: number; precoUnitario: number; observacoes: string; }
export interface Pedido { id: number; status: StatusPedido; valorTotal: number; enderecoEntrega: string; createdAt: string; itens: PedidoItem[]; }

// ---- Painel (bloco 5) ----
// Ciclo completo que o painel mostra (RF-016). O cliente só vê até SAIU_ENTREGA (ETAPAS).
export const CICLO: StatusPedido[] = [...ETAPAS, 'FINALIZADO'];
export const TODOS_STATUS: StatusPedido[] = [...CICLO, 'CANCELADO'];
export type CanalPedido = 'SITE'|'TELEFONE'|'WHATSAPP';
export const ROTULOS_CANAL: Record<CanalPedido, string> = { SITE: 'Site', TELEFONE: 'Telefone', WHATSAPP: 'WhatsApp' };
export const CANAIS: CanalPedido[] = ['SITE', 'TELEFONE', 'WHATSAPP'];
export interface ClienteResumo { nome: string; telefone: string | null; email: string; }
// Uma etapa da linha do tempo: qual status, quando aconteceu e quem fez (usuario pode vir vazio em registros antigos)
export interface HistoricoStatus { status: StatusPedido; createdAt: string; usuario: string | null; }
// Mesmo formato do PedidoAdminResponse do back
export interface PedidoAdmin extends Pedido {
  canal: CanalPedido; observacoes: string | null; motivoCancelamento: string | null; updatedAt: string; cliente: ClienteResumo;
  historico: HistoricoStatus[];
}
// Mesmo formato do PaginaResponse do back: toda listagem paginada vem assim
export interface Pagina<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface FiltroPedidos { status: StatusPedido | ''; canal: CanalPedido | ''; dataInicio: string; dataFim: string; }

// ---- Usuários internos (bloco 10, RF-041) ----
export type StatusRegistro = 'ATIVO'|'INATIVO'|'PAUSADO';
export const ROTULOS_STATUS_REGISTRO: Record<StatusRegistro, string> = { ATIVO: 'Ativo', INATIVO: 'Inativo', PAUSADO: 'Pausado' };
// Mesmo formato do UsuarioResponse do back (a senha nunca vem)
export interface UsuarioInterno { id: number; nome: string; email: string; telefone: string | null; perfil: Perfil; status: StatusRegistro; createdAt: string; }
// O que o formulário manda para criar/editar. Na edição, senha vazia = manter a atual.
export interface UsuarioForm { nome: string; email: string; telefone: string; perfil: Perfil; senha: string; }
export interface FiltroUsuarios { busca: string; perfil: Perfil | ''; status: StatusRegistro | ''; }

// ---- Cardápio do admin (blocos 3 e 4) ----
export type UnidadeMedida = 'G'|'ML'|'UN'|'KG'|'L';
export const UNIDADES: UnidadeMedida[] = ['G', 'ML', 'UN', 'KG', 'L'];
export const ROTULOS_UNIDADE: Record<UnidadeMedida, string> = { G: 'g', ML: 'ml', UN: 'un', KG: 'kg', L: 'L' };
export type FaixaFoodCost = 'VERDE'|'AMARELO'|'VERMELHO';
export const ROTULOS_FAIXA: Record<FaixaFoodCost, string> = { VERDE: 'Saudável (até 30%)', AMARELO: 'Atenção (30% a 35%)', VERMELHO: 'Alto (acima de 35%)' };
export interface FiltroSimples { busca: string; status: StatusRegistro | ''; }

export interface Categoria { id: number; nome: string; descricao: string | null; ordem: number; status: StatusRegistro; }
export interface CategoriaForm { nome: string; descricao: string; ordem: number; }

// custoUnitario = preço de UMA unidade padrão (1 g, 1 ml, 1 un...)
export interface Ingrediente { id: number; nome: string; sku: string; unidadePadrao: UnidadeMedida; estoqueMinimo: number; custoUnitario: number; status: StatusRegistro; }
export interface IngredienteForm { nome: string; sku: string; unidadePadrao: UnidadeMedida; estoqueMinimo: number; custoUnitario: number; }

// Mesmo formato do CustoResponse do back: o front só MOSTRA, quem calcula é o Service do Spring (RF-012)
export interface Custo { custoTotal: number; rendimento: number; custoPorcao: number; precoVenda: number; foodCost: number; faixa: FaixaFoodCost; alerta: string | null; }
export interface PratoAdmin {
  id: number; nome: string; descricao: string | null; fotoUrl: string | null; precoVenda: number; tempoPreparoMin: number;
  categoriaId: number; categoria: string; status: StatusRegistro; temFicha: boolean; custo: Custo | null;
}
export interface PratoForm { nome: string; descricao: string; fotoUrl: string; precoVenda: number | null; tempoPreparoMin: number | null; categoriaId: number | null; status: StatusRegistro; }
export interface FiltroPratos extends FiltroSimples { categoriaId: number | null; }

export interface FichaItem { ingredienteId: number; ingrediente: string; unidade: UnidadeMedida; quantidade: number; fatorCorrecao: number; custoUnitario: number; custo: number; }
export interface Ficha { pratoId: number; prato: string; rendimento: number; modoPreparo: string | null; itens: FichaItem[]; custo: Custo; }
export interface FichaForm { rendimento: number | null; modoPreparo: string; itens: { ingredienteId: number | null; quantidade: number | null; fatorCorrecao: number | null }[]; }

// ---- Estoque (blocos 6 e 8) ----
export type TipoMovimentacao = 'ENTRADA'|'SAIDA'|'ESTORNO';
export const ROTULOS_TIPO_MOV: Record<TipoMovimentacao, string> = { ENTRADA: 'Entrada', SAIDA: 'Saída', ESTORNO: 'Estorno' };
export type MotivoMovimentacao = 'COMPRA'|'VENDA'|'DESPERDICIO'|'VENCIMENTO'|'QUEBRA'|'USO_INTERNO'|'AJUSTE'|'ESTORNO';
export const ROTULOS_MOTIVO: Record<MotivoMovimentacao, string> = {
  COMPRA: 'Compra', VENDA: 'Venda (pedido)', DESPERDICIO: 'Desperdício', VENCIMENTO: 'Vencimento', QUEBRA: 'Quebra',
  USO_INTERNO: 'Uso interno', AJUSTE: 'Ajuste de inventário', ESTORNO: 'Estorno (pedido cancelado)',
};
// RF-030: motivos aceitos numa saída manual
export const MOTIVOS_SAIDA: MotivoMovimentacao[] = ['DESPERDICIO', 'VENCIMENTO', 'QUEBRA', 'USO_INTERNO', 'AJUSTE'];
export interface EstoqueSaldo {
  ingredienteId: number; ingrediente: string; sku: string; unidade: UnidadeMedida; saldo: number; estoqueMinimo: number;
  custoUnitario: number; abaixoMinimo: boolean; status: StatusRegistro;
}
export interface Movimentacao {
  id: number; createdAt: string; ingredienteId: number; ingrediente: string; unidade: UnidadeMedida; tipo: TipoMovimentacao;
  motivo: MotivoMovimentacao; quantidade: number; lote: string | null; validade: string | null; custoUnitario: number | null;
  pedidoId: number | null; pedidoCompraId: number | null; usuario: string | null; observacao: string | null;
}
export interface MovimentacaoForm {
  ingredienteId: number; tipo: TipoMovimentacao; motivo: MotivoMovimentacao; quantidade: number | null;
  lote: string; validade: string; custoUnitario: number | null; observacao: string;
}
export interface FiltroMovimentacoes { ingredienteId: number | null; tipo: TipoMovimentacao | ''; dataInicio: string; dataFim: string; }
