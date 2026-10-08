import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse, NovoCliente } from './models';

const SESSAO = 'comanda-sessao';
const USUARIOS = 'comanda-usuarios'; // só no modo mock

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  readonly sessao = signal<LoginResponse | null>(this.ler(SESSAO, null));
  readonly logado = computed(() => !!this.sessao());
  get token() { return this.sessao()?.token ?? null; }

  login(email: string, senha: string): Observable<LoginResponse> {
    if (!environment.useMock) // com o back de verdade: guarda a sessão (token + perfil) assim que a resposta chega
      return this.http.post<LoginResponse>(`${environment.apiUrl}/api/auth/login`, { email, senha }).pipe(tap(r => this.salvarSessao(r)));
    if (email === 'admin@email.com' && senha === 'senha123') // seed do SRS
      return this.guardar({ token: 'mock-jwt', perfil: 'ADMIN', nome: 'Admin' });
    const u = this.ler<NovoCliente[]>(USUARIOS, []).find(x => x.email === email && x.senha === senha);
    return u ? this.guardar({ token: 'mock-jwt', perfil: 'CLIENTE', nome: u.nome, endereco: u.endereco })
             : throwError(() => ({ status: 401 }));
  }
  registrar(d: NovoCliente): Observable<LoginResponse> {
    if (!environment.useMock)
      return this.http.post<LoginResponse>(`${environment.apiUrl}/api/auth/register`, d).pipe(tap(r => this.salvarSessao(r)));
    const lista = this.ler<NovoCliente[]>(USUARIOS, []);
    if (lista.some(x => x.email === d.email)) return throwError(() => ({ status: 409 })); // RN10
    this.gravar(USUARIOS, [...lista, d]);
    return this.guardar({ token: 'mock-jwt', perfil: 'CLIENTE', nome: d.nome, endereco: d.endereco });
  }
  guardar(r: LoginResponse): Observable<LoginResponse> { this.salvarSessao(r); return of(r); }
  private salvarSessao(r: LoginResponse) { this.sessao.set(r); this.gravar(SESSAO, r); }
  temPerfil(perfis: string[]) { const s = this.sessao(); return !!s && perfis.includes(s.perfil); }
  logout() { this.sessao.set(null); try { localStorage.removeItem(SESSAO); } catch {} }

  private gravar(k: string, v: unknown) { try { localStorage.setItem(k, JSON.stringify(v)); } catch {} }
  private ler<T>(k: string, padrao: T): T { try { return JSON.parse(localStorage.getItem(k) ?? 'null') ?? padrao; } catch { return padrao; } }
}
