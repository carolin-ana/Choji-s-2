import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
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
        <a routerLink="/admin/pedidos" routerLinkActive="ativo">Pedidos</a>
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
  nome = computed(() => this.auth.sessao()?.nome ?? '');
  perfil = computed(() => { const s = this.auth.sessao(); return s ? ROTULOS_PERFIL[s.perfil] : ''; });

  sair() { this.auth.logout(); this.router.navigate(['/login']); }
}
