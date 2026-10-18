import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { Categoria, CategoriaForm, Ficha, FichaForm, FiltroPratos, FiltroSimples, Pagina, PratoAdmin, PratoForm } from './models';

// Chamadas do cardápio do painel: categorias, pratos e ficha técnica (blocos 3 e 4)
@Injectable({ providedIn: 'root' })
export class AdminCardapioService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/api/admin`;

  // ---- Categorias (RF-009) ----
  listarCategorias(f: FiltroSimples, page: number, size = 20): Observable<Pagina<Categoria>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.busca.trim()) params = params.set('busca', f.busca.trim());
    if (f.status) params = params.set('status', f.status);
    return this.http.get<Pagina<Categoria>>(`${this.api}/categorias`, { params });
  }
  // Todas as categorias (para os selects). O back aceita até 100 por página.
  todasCategorias(): Observable<Categoria[]> {
    return this.listarCategorias({ busca: '', status: '' }, 0, 100).pipe(map(p => p.content));
  }
  criarCategoria(c: CategoriaForm) { return this.http.post<Categoria>(`${this.api}/categorias`, this.categoria(c)); }
  atualizarCategoria(id: number, c: CategoriaForm) { return this.http.put<Categoria>(`${this.api}/categorias/${id}`, this.categoria(c)); }
  desativarCategoria(id: number) { return this.http.delete<Categoria>(`${this.api}/categorias/${id}`); }
  reativarCategoria(id: number) { return this.http.patch<Categoria>(`${this.api}/categorias/${id}/reativar`, {}); }

  // ---- Pratos (RF-010) ----
  listarPratos(f: FiltroPratos, page: number, size = 10): Observable<Pagina<PratoAdmin>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.busca.trim()) params = params.set('busca', f.busca.trim());
    if (f.status) params = params.set('status', f.status);
    if (f.categoriaId) params = params.set('categoriaId', f.categoriaId);
    return this.http.get<Pagina<PratoAdmin>>(`${this.api}/pratos`, { params });
  }
  buscarPrato(id: number) { return this.http.get<PratoAdmin>(`${this.api}/pratos/${id}`); }
  criarPrato(p: PratoForm) { return this.http.post<PratoAdmin>(`${this.api}/pratos`, this.prato(p)); }
  atualizarPrato(id: number, p: PratoForm) { return this.http.put<PratoAdmin>(`${this.api}/pratos/${id}`, this.prato(p)); }
  desativarPrato(id: number) { return this.http.delete<PratoAdmin>(`${this.api}/pratos/${id}`); }

  // ---- Ficha técnica (RF-011 a RF-014) ----
  // Prato sem ficha: o back responde 404, que aqui vira null (não é erro, é "ainda não tem")
  buscarFicha(pratoId: number): Observable<Ficha | null> {
    return this.http.get<Ficha>(`${this.api}/pratos/${pratoId}/ficha`).pipe(
      catchError(e => (e.status === 404 ? of(null) : throwError(() => e))));
  }
  criarFicha(pratoId: number, f: FichaForm) { return this.http.post<Ficha>(`${this.api}/pratos/${pratoId}/ficha`, f); }
  atualizarFicha(pratoId: number, f: FichaForm) { return this.http.put<Ficha>(`${this.api}/pratos/${pratoId}/ficha`, f); }
  // O back calcula o custo da ficha em edição sem salvar (RF-012: custo "em tempo real", calculado no Service)
  simularFicha(pratoId: number, f: FichaForm) { return this.http.post<Ficha>(`${this.api}/pratos/${pratoId}/ficha/simular`, f); }

  private categoria(c: CategoriaForm) { return { nome: c.nome.trim(), descricao: c.descricao.trim() || null, ordem: c.ordem }; }
  private prato(p: PratoForm) {
    return { ...p, nome: p.nome.trim(), descricao: p.descricao.trim() || null, fotoUrl: p.fotoUrl.trim() || null };
  }
}
