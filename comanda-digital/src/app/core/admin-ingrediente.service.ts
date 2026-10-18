import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { FiltroSimples, Ingrediente, IngredienteForm, Pagina } from './models';

// Chamadas do cadastro de ingredientes (/api/admin/ingredientes)
@Injectable({ providedIn: 'root' })
export class AdminIngredienteService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/admin/ingredientes`;

  listar(f: FiltroSimples, page: number, size = 20): Observable<Pagina<Ingrediente>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.busca.trim()) params = params.set('busca', f.busca.trim());
    if (f.status) params = params.set('status', f.status);
    return this.http.get<Pagina<Ingrediente>>(this.base, { params });
  }
  // Todos (ativos e inativos), para o select da ficha técnica. O back aceita até 200 por página.
  todos(): Observable<Ingrediente[]> { return this.listar({ busca: '', status: '' }, 0, 200).pipe(map(p => p.content)); }
  criar(i: IngredienteForm) { return this.http.post<Ingrediente>(this.base, this.corpo(i)); }
  atualizar(id: number, i: IngredienteForm) { return this.http.put<Ingrediente>(`${this.base}/${id}`, this.corpo(i)); }
  desativar(id: number) { return this.http.delete<Ingrediente>(`${this.base}/${id}`); }
  reativar(id: number) { return this.http.patch<Ingrediente>(`${this.base}/${id}/reativar`, {}); }

  private corpo(i: IngredienteForm) { return { ...i, nome: i.nome.trim(), sku: i.sku.trim().toUpperCase() }; }
}
