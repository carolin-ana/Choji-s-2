import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Resumo, TopPrato, Vendas } from './models';

// Chamadas do dashboard (/api/admin/dashboard). Datas no formato AAAA-MM-DD.
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/admin/dashboard`;

  resumo(data?: string) {
    return this.http.get<Resumo>(`${this.base}/resumo`, { params: data ? new HttpParams().set('data', data) : undefined });
  }
  topPratos(dataInicio: string, dataFim: string) {
    return this.http.get<TopPrato[]>(`${this.base}/top-pratos`, { params: new HttpParams().set('dataInicio', dataInicio).set('dataFim', dataFim) });
  }
  vendas(dataInicio: string, dataFim: string) {
    return this.http.get<Vendas>(`${this.base}/vendas`, { params: new HttpParams().set('dataInicio', dataInicio).set('dataFim', dataFim) });
  }
}
