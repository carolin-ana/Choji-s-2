import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { CatalogoForm, CatalogoItem, Cotacao, FiltroSimples, Fornecedor, FornecedorForm, Pagina, PrecoHistorico } from './models';
import { normalizarCnpj } from './cnpj';

// Chamadas de fornecedores + catálogo (/api/admin/fornecedores) e da cotação (/api/admin/cotacao)
@Injectable({ providedIn: 'root' })
export class AdminFornecedorService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/admin/fornecedores`;
  private baseCotacao = `${environment.apiUrl}/api/admin/cotacao`;

  listar(f: FiltroSimples, page: number, size = 20): Observable<Pagina<Fornecedor>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.busca.trim()) params = params.set('busca', f.busca.trim());
    if (f.status) params = params.set('status', f.status);
    return this.http.get<Pagina<Fornecedor>>(this.base, { params });
  }
  // Todos os fornecedores (para os selects). O back aceita até 100 por página.
  todos(): Observable<Fornecedor[]> { return this.listar({ busca: '', status: '' }, 0, 100).pipe(map(p => p.content)); }
  buscar(id: number) { return this.http.get<Fornecedor>(`${this.base}/${id}`); }
  criar(f: FornecedorForm) { return this.http.post<Fornecedor>(this.base, this.corpo(f)); }
  atualizar(id: number, f: FornecedorForm) { return this.http.put<Fornecedor>(`${this.base}/${id}`, this.corpo(f)); }
  desativar(id: number) { return this.http.delete<Fornecedor>(`${this.base}/${id}`); }
  reativar(id: number) { return this.http.patch<Fornecedor>(`${this.base}/${id}/reativar`, {}); }

  // ---- Catálogo (RF-022) ----
  catalogo(id: number): Observable<CatalogoItem[]> { return this.http.get<CatalogoItem[]>(`${this.base}/${id}/catalogo`); }
  adicionarItem(id: number, i: CatalogoForm) {
    return this.http.post<CatalogoItem>(`${this.base}/${id}/catalogo`,
      { ingredienteId: i.ingredienteId, preco: i.preco, unidadeVenda: i.unidadeVenda.trim() || null });
  }
  atualizarItem(id: number, itemId: number, i: CatalogoForm) {
    return this.http.put<CatalogoItem>(`${this.base}/${id}/catalogo/${itemId}`, { preco: i.preco, unidadeVenda: i.unidadeVenda.trim() || null });
  }
  removerItem(id: number, itemId: number) { return this.http.delete<CatalogoItem>(`${this.base}/${id}/catalogo/${itemId}`); }

  // ---- Cotação (RF-023) e histórico de preços (RF-026) ----
  cotacao(ingredienteId: number) { return this.http.get<Cotacao>(`${this.baseCotacao}/${ingredienteId}`); }
  historico(ingredienteId: number) { return this.http.get<PrecoHistorico[]>(`${this.baseCotacao}/${ingredienteId}/historico`); }

  private corpo(f: FornecedorForm) {
    return { razaoSocial: f.razaoSocial.trim(), cnpj: normalizarCnpj(f.cnpj), telefone: f.telefone.trim() || null,
             email: f.email.trim() || null, categorias: f.categorias.trim() || null };
  }
}
