import { Injectable, computed, signal } from '@angular/core';
import { ItemCarrinho, Prato } from './models';

const KEY = 'comanda-carrinho';

@Injectable({ providedIn: 'root' })
export class CartService {
  readonly itens = signal<ItemCarrinho[]>(this.carregar());
  readonly total = computed(() => this.itens().reduce((s, i) => s + i.prato.precoVenda * i.quantidade, 0));
  readonly quantidade = computed(() => this.itens().reduce((s, i) => s + i.quantidade, 0));

  adicionar(prato: Prato, quantidade = 1, observacoes = '') {
    const lista = [...this.itens()];
    const i = lista.findIndex(x => x.prato.id === prato.id && x.observacoes === observacoes);
    if (i >= 0) lista[i] = { ...lista[i], quantidade: lista[i].quantidade + quantidade };
    else lista.push({ prato, quantidade, observacoes });
    this.salvar(lista);
  }
  alterarQuantidade(i: number, quantidade: number) {
    const lista = [...this.itens()];
    if (quantidade <= 0) lista.splice(i, 1); else lista[i] = { ...lista[i], quantidade };
    this.salvar(lista);
  }
  remover(i: number) { this.alterarQuantidade(i, 0); }
  limpar() { this.salvar([]); }

  private salvar(l: ItemCarrinho[]) {
    this.itens.set(l);
    try { localStorage.setItem(KEY, JSON.stringify(l)); } catch {}
  }
  private carregar(): ItemCarrinho[] {
    try { return JSON.parse(localStorage.getItem(KEY) ?? '[]'); } catch { return []; }
  }
}
