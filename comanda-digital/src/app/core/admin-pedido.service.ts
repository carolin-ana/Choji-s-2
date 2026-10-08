import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FiltroPedidos, Pagina, PedidoAdmin, StatusPedido } from './models';

// Chamadas do painel (/api/admin/pedidos). Não tem modo mock: o painel sempre fala com o back de verdade.
@Injectable({ providedIn: 'root' })
export class AdminPedidoService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/admin/pedidos`;

  // RF-019: filtros opcionais + paginação. Só manda na URL o filtro que estiver preenchido.
  listar(f: FiltroPedidos, page: number, size = 10): Observable<Pagina<PedidoAdmin>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.status) params = params.set('status', f.status);
    if (f.canal) params = params.set('canal', f.canal);
    if (f.dataInicio) params = params.set('dataInicio', f.dataInicio);
    if (f.dataFim) params = params.set('dataFim', f.dataFim);
    return this.http.get<Pagina<PedidoAdmin>>(this.base, { params });
  }
  buscar(id: number): Observable<PedidoAdmin> { return this.http.get<PedidoAdmin>(`${this.base}/${id}`); }
  mudarStatus(id: number, status: StatusPedido): Observable<PedidoAdmin> {
    return this.http.patch<PedidoAdmin>(`${this.base}/${id}/status`, { status });
  }
  cancelar(id: number, motivo: string): Observable<PedidoAdmin> {
    return this.http.patch<PedidoAdmin>(`${this.base}/${id}/cancelar`, { motivo });
  }
}
