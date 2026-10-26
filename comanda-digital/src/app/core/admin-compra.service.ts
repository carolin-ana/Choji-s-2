import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Compra, CompraForm, FiltroCompras, Pagina, Recebimento } from './models';

// Chamadas dos pedidos de compra (/api/admin/compras)
@Injectable({ providedIn: 'root' })
export class AdminCompraService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/admin/compras`;

  listar(f: FiltroCompras, page: number, size = 20): Observable<Pagina<Compra>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.status) params = params.set('status', f.status);
    if (f.fornecedorId) params = params.set('fornecedorId', f.fornecedorId);
    return this.http.get<Pagina<Compra>>(this.base, { params });
  }
  buscar(id: number) { return this.http.get<Compra>(`${this.base}/${id}`); }
  criar(c: CompraForm) { return this.http.post<Compra>(this.base, this.corpo(c)); }
  atualizar(id: number, c: CompraForm) { return this.http.put<Compra>(`${this.base}/${id}`, this.corpo(c)); }
  enviar(id: number) { return this.http.patch<Compra>(`${this.base}/${id}/enviar`, {}); }
  receber(id: number, lote: string, validade: string, observacao: string) {
    return this.http.post<Recebimento>(`${this.base}/${id}/receber`,
      { lote: lote.trim() || null, validade: validade || null, observacao: observacao.trim() || null });
  }
  cancelar(id: number, motivo: string) { return this.http.patch<Compra>(`${this.base}/${id}/cancelar`, { motivo: motivo.trim() }); }

  // O total NÃO vai: quem calcula é o back
  private corpo(c: CompraForm) {
    return { fornecedorId: c.fornecedorId, observacoes: c.observacoes.trim() || null, itens: c.itens };
  }
}
