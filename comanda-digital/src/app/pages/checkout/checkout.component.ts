import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { PedidoService } from '../../core/pedido.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  template: `
  <main class="pagina">
    <a routerLink="/">← Voltar ao cardápio</a>
    <h1>Finalizar pedido</h1>
    @if (!cart.itens().length) {
      <p>Seu carrinho está vazio. <a routerLink="/">Escolha um prato no cardápio.</a></p>
    } @else {
      @for (i of cart.itens(); track $index) {
        <div class="caixa">
          <strong>{{ i.quantidade }}× {{ i.prato.nome }}</strong>
          @if (i.observacoes) { <div>Obs.: {{ i.observacoes }}</div> }
          <div>{{ i.prato.precoVenda * i.quantidade | currency:'BRL' }}</div>
        </div>
      }
      <div class="linha-total"><strong>Total</strong><strong>{{ cart.total() | currency:'BRL' }}</strong></div>
      <div class="form">
        <label>Endereço de entrega
          <input [value]="endereco()" (input)="endereco.set($any($event.target).value)" />
        </label>
        <p>Pagamento simulado: o pedido será marcado como pago.</p>
        @if (erro()) { <p class="erro">{{ erro() }}</p> }
        <button class="btn" (click)="confirmar()" [disabled]="enviando()">{{ enviando() ? 'Enviando...' : 'Confirmar pedido' }}</button>
      </div>
    }
  </main>`,
})
export class CheckoutComponent {
  cart = inject(CartService);
  private auth = inject(AuthService);
  private api = inject(PedidoService);
  private router = inject(Router);
  endereco = signal(this.auth.sessao()?.endereco ?? '');
  enviando = signal(false);
  erro = signal('');

  confirmar() {
    if (!this.endereco().trim()) { this.erro.set('Informe o endereço de entrega.'); return; }
    this.enviando.set(true); this.erro.set('');
    this.api.criar(this.cart.itens(), this.endereco().trim()).subscribe({
      next: p => { this.cart.limpar(); this.router.navigate(['/pedido', p.id]); },
      error: e => { this.enviando.set(false); this.erro.set(e.error?.message ?? 'Não foi possível confirmar o pedido.'); },
    });
  }
}
