import { Component, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { switchMap, timer } from 'rxjs';
import { PedidoService } from '../../core/pedido.service';
import { ETAPAS, ROTULOS } from '../../core/models';

@Component({
  selector: 'app-acompanhar',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  template: `
  <main class="pagina">
    <a routerLink="/pedidos">← Meus pedidos</a>
    @if (pedido(); as p) {
      <h1>Pedido #{{ p.id }}</h1>
      <p>Total {{ p.valorTotal | currency:'BRL' }} · Entrega em {{ p.enderecoEntrega }}</p>
      @if (p.status === 'CANCELADO') { <p class="erro">Este pedido foi cancelado.</p> }
      @else {
        <ol class="timeline">
          @for (e of etapas; track e; let i = $index) {
            <li [class.feita]="i <= indice(p.status)" [class.atual]="i === indice(p.status)">{{ rotulos[e] }}</li>
          }
        </ol>
      }
    } @else { <p>Carregando pedido...</p> }
  </main>`,
})
export class AcompanharComponent {
  private api = inject(PedidoService);
  private id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  etapas = ETAPAS;
  rotulos = ROTULOS;
  // Atualiza a cada 5s para o cliente ver o status mudar
  pedido = toSignal(timer(0, 5000).pipe(switchMap(() => this.api.buscar(this.id))));
  indice(s: string) { return s === 'FINALIZADO' ? ETAPAS.length : ETAPAS.indexOf(s as any); }
}
