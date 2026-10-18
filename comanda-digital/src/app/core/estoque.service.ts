import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { EstoqueSaldo, FiltroMovimentacoes, FiltroSimples, Movimentacao, MovimentacaoForm, Pagina } from './models';

// Chamadas do estoque (/api/admin/estoque) + o número de alertas que aparece no menu (RF-032)
@Injectable({ providedIn: 'root' })
export class EstoqueService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private base = `${environment.apiUrl}/api/admin/estoque`;

  // Quantos ingredientes estão abaixo do mínimo (badge vermelho do menu)
  readonly alertas = signal(0);

  saldo(f: FiltroSimples, page: number, size = 20): Observable<Pagina<EstoqueSaldo>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.busca.trim()) params = params.set('busca', f.busca.trim());
    if (f.status) params = params.set('status', f.status);
    return this.http.get<Pagina<EstoqueSaldo>>(`${this.base}/saldo`, { params });
  }
  listarAlertas(page: number, size = 20): Observable<Pagina<EstoqueSaldo>> {
    return this.http.get<Pagina<EstoqueSaldo>>(`${this.base}/alertas`, { params: new HttpParams().set('page', page).set('size', size) });
  }
  movimentacoes(f: FiltroMovimentacoes, page: number, size = 20): Observable<Pagina<Movimentacao>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.ingredienteId) params = params.set('ingredienteId', f.ingredienteId);
    if (f.tipo) params = params.set('tipo', f.tipo);
    if (f.dataInicio) params = params.set('dataInicio', f.dataInicio);
    if (f.dataFim) params = params.set('dataFim', f.dataFim);
    return this.http.get<Pagina<Movimentacao>>(`${this.base}/movimentacoes`, { params });
  }
  registrar(m: MovimentacaoForm): Observable<Movimentacao> {
    const entrada = m.tipo === 'ENTRADA';
    return this.http.post<Movimentacao>(`${this.base}/movimentacao`, {
      ingredienteId: m.ingredienteId, tipo: m.tipo, motivo: m.motivo, quantidade: m.quantidade,
      lote: entrada ? m.lote.trim() || null : null, validade: entrada ? m.validade || null : null,
      custoUnitario: entrada ? m.custoUnitario : null, observacao: m.observacao.trim() || null,
    });
  }

  // Só admin e gerente podem ver o estoque; para o cozinheiro o badge fica zerado
  atualizarAlertas() {
    if (!this.auth.temPerfil(['ADMIN', 'GERENTE'])) { this.alertas.set(0); return; }
    this.listarAlertas(0, 1).subscribe({ next: p => this.alertas.set(p.totalElements), error: () => {} });
  }
}
