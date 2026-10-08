import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { ETAPAS, ItemCarrinho, Pedido } from './models';

const KEY = 'comanda-pedidos'; // só no modo mock

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private http = inject(HttpClient);

  criar(itens: ItemCarrinho[], enderecoEntrega: string): Observable<Pedido> {
    if (!environment.useMock)
      return this.http.post<Pedido>(`${environment.apiUrl}/api/pedidos`, {
        enderecoEntrega,
        itens: itens.map(i => ({ pratoId: i.prato.id, quantidade: i.quantidade, observacoes: i.observacoes })),
      });
    const lista = this.lerMock();
    const pedido: Pedido = {
      id: Math.max(1000, ...lista.map(p => p.id)) + 1, status: 'RECEBIDO', enderecoEntrega,
      createdAt: new Date().toISOString(),
      valorTotal: itens.reduce((s, i) => s + i.prato.precoVenda * i.quantidade, 0),
      itens: itens.map(i => ({ nome: i.prato.nome, quantidade: i.quantidade, precoUnitario: i.prato.precoVenda, observacoes: i.observacoes })),
    };
    try { localStorage.setItem(KEY, JSON.stringify([pedido, ...lista])); } catch {}
    return of(pedido);
  }

  meus(): Observable<Pedido[]> {
    if (!environment.useMock) return this.http.get<Pedido[]>(`${environment.apiUrl}/api/pedidos/meus`);
    return of(this.lerMock().map(p => this.simular(p)));
  }
  buscar(id: number): Observable<Pedido | undefined> {
    return this.meus().pipe(map(l => l.find(p => p.id === id)));
  }

  private lerMock(): Pedido[] { try { return JSON.parse(localStorage.getItem(KEY) ?? '[]'); } catch { return []; } }
  // Mock: o status avança sozinho a cada 15s, só para ver a timeline funcionando
  private simular(p: Pedido): Pedido {
    const i = Math.min(Math.floor((Date.now() - new Date(p.createdAt).getTime()) / 15000), ETAPAS.length - 1);
    return { ...p, status: ETAPAS[i] };
  }
}
