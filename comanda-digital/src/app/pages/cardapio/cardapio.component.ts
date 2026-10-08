import { Component, inject, signal, computed } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth.service';
import { CardapioService } from '../../core/cardapio.service';
import { CartService } from '../../core/cart.service';
import { EQUIPE, Prato } from '../../core/models';

@Component({
  selector: 'app-cardapio',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './cardapio.component.html',
  styleUrl: './cardapio.component.css',
})
export class CardapioComponent {
  private api = inject(CardapioService);
  cart = inject(CartService);
  auth = inject(AuthService);

  pratos = toSignal(this.api.listar(), { initialValue: [] as Prato[] });
  categorias = computed(() => [...new Set(this.pratos().map(p => p.categoria))]);
  filtro = signal<string | null>(null);
  visiveis = computed(() => this.pratos().filter(p => !this.filtro() || p.categoria === this.filtro()));
  detalhe = signal<Prato | null>(null);   // RF-002
  carrinhoAberto = signal(false);
  qtd = signal(1);
  obs = signal('');
  // Quem é da equipe vê o atalho "Painel" no lugar de "Meus pedidos"
  ehEquipe = computed(() => this.auth.temPerfil(EQUIPE));
  primeiroNome = computed(() => (this.auth.sessao()?.nome ?? '').split(' ')[0]);

  abrir(p: Prato) { this.detalhe.set(p); this.qtd.set(1); this.obs.set(''); }
  adicionar() {
    const p = this.detalhe(); if (!p) return;
    this.cart.adicionar(p, this.qtd(), this.obs().trim());
    this.detalhe.set(null); this.carrinhoAberto.set(true);
  }
  sair() { this.auth.logout(); }
}
