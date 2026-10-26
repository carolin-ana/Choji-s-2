import { Component, DestroyRef, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { EstoqueService } from '../../core/estoque.service';
import { ROTULOS_PERFIL } from '../../core/models';

// Moldura do painel: barra de cima com o menu + a tela escolhida (rota filha) embaixo.
// Cada tela nova do painel ganha um link aqui no menu.
@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
  <div class="painel">
    <header class="painel-topo">
      <strong class="painel-marca">Comanda Digital <span>Painel</span></strong>
      <nav class="painel-menu" aria-label="Menu do painel">
        @if (gerencia()) { <a routerLink="/admin/dashboard" routerLinkActive="ativo">Dashboard</a> }
        <a routerLink="/admin/pedidos" routerLinkActive="ativo">Pedidos</a>
        @if (gerencia()) {
          <a routerLink="/admin/pratos" routerLinkActive="ativo">Pratos</a>
          <a routerLink="/admin/categorias" routerLinkActive="ativo">Categorias</a>
          <a routerLink="/admin/ingredientes" routerLinkActive="ativo">Ingredientes</a>
          <a routerLink="/admin/estoque" routerLinkActive="ativo">Estoque
            @if (estoque.alertas() > 0) {
              <span class="badge-alerta" [title]="estoque.alertas() + ' ingrediente(s) abaixo do mínimo'">{{ estoque.alertas() }}</span>
            }
          </a>
          <a routerLink="/admin/fornecedores" routerLinkActive="ativo">Fornecedores</a>
          <a routerLink="/admin/cotacao" routerLinkActive="ativo">Cotação</a>
          <a routerLink="/admin/compras" routerLinkActive="ativo">Compras</a>
        }
        @if (ehAdmin()) { <a routerLink="/admin/usuarios" routerLinkActive="ativo">Usuários</a> }
      </nav>
      <div class="painel-usuario">
        <span>{{ nome() }} · {{ perfil() }}</span>
        <a routerLink="/">Ver cardápio</a>
        <button type="button" class="btn-sec" (click)="sair()">Sair</button>
      </div>
    </header>
    <main class="painel-conteudo"><router-outlet /></main>
  </div>`,
})
export class AdminLayoutComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  estoque = inject(EstoqueService);
  nome = computed(() => this.auth.sessao()?.nome ?? '');
  gerencia = computed(() => this.auth.temPerfil(['ADMIN', 'GERENTE'])); // cardápio e ingredientes: admin e gerente
  ehAdmin = computed(() => this.auth.temPerfil(['ADMIN'])); // menu "Usuários" só para o ADMIN
  perfil = computed(() => { const s = this.auth.sessao(); return s ? ROTULOS_PERFIL[s.perfil] : ''; });

  constructor() {
    // RF-032: badge vermelho no menu com quantos ingredientes estão abaixo do mínimo (atualiza a cada minuto)
    const t = setInterval(() => this.estoque.atualizarAlertas(), 60_000);
    this.estoque.atualizarAlertas();
    inject(DestroyRef).onDestroy(() => clearInterval(t));
  }

  sair() { this.auth.logout(); this.router.navigate(['/login']); }
}
