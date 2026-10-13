import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FiltroUsuarios, Pagina, UsuarioForm, UsuarioInterno } from './models';

// Chamadas da tela de usuários internos (/api/admin/usuarios). Só o ADMIN usa.
@Injectable({ providedIn: 'root' })
export class AdminUsuarioService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/admin/usuarios`;

  listar(f: FiltroUsuarios, page: number, size = 10): Observable<Pagina<UsuarioInterno>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (f.busca.trim()) params = params.set('busca', f.busca.trim());
    if (f.perfil) params = params.set('perfil', f.perfil);
    if (f.status) params = params.set('status', f.status);
    return this.http.get<Pagina<UsuarioInterno>>(this.base, { params });
  }
  criar(u: UsuarioForm): Observable<UsuarioInterno> { return this.http.post<UsuarioInterno>(this.base, this.corpo(u)); }
  atualizar(id: number, u: UsuarioForm): Observable<UsuarioInterno> {
    return this.http.put<UsuarioInterno>(`${this.base}/${id}`, this.corpo(u));
  }
  // RN06: "excluir" no back é desativar (o registro continua no banco)
  desativar(id: number): Observable<UsuarioInterno> { return this.http.delete<UsuarioInterno>(`${this.base}/${id}`); }
  reativar(id: number): Observable<UsuarioInterno> { return this.http.patch<UsuarioInterno>(`${this.base}/${id}/reativar`, {}); }

  // Campos vazios vão como null: na edição, senha null = manter a senha atual
  private corpo(u: UsuarioForm) {
    return { nome: u.nome.trim(), email: u.email.trim(), perfil: u.perfil,
             telefone: u.telefone.trim() || null, senha: u.senha || null };
  }
}
