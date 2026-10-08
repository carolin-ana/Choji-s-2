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
