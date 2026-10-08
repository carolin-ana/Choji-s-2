import { Component, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { PedidoService } from '../../core/pedido.service';
import { Pedido, ROTULOS } from '../../core/models';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
  <main class="pagina">
    <a routerLink="/">← Voltar ao cardápio</a>
    <h1>Meus pedidos</h1>
    @for (p of pedidos(); track p.id) {
      <a class="caixa" style="display:block;color:inherit;text-decoration:none" [routerLink]="['/pedido', p.id]">
        <strong>Pedido #{{ p.id }}</strong> · {{ p.createdAt | date:'dd/MM/yyyy HH:mm' }}
        @for (i of p.itens; track $index) { <div>{{ i.quantidade }}× {{ i.nome }}</div> }
        <div class="linha-total"><span>{{ rotulos[p.status] }}</span><strong>{{ p.valorTotal | currency:'BRL' }}</strong></div>
      </a>
    } @empty { <p>Você ainda não fez nenhum pedido. <a routerLink="/">Ver o cardápio</a></p> }
  </main>`,
})
export class PedidosComponent {
  rotulos = ROTULOS;
  // Mais recente primeiro (RF-008)
  pedidos = toSignal(inject(PedidoService).meus(), { initialValue: [] as Pedido[] });
}
