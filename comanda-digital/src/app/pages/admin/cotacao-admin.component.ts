import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, of, startWith, switchMap, tap } from 'rxjs';
import { AdminFornecedorService } from '../../core/admin-fornecedor.service';
import { AdminIngredienteService } from '../../core/admin-ingrediente.service';
import { Cotacao, Ingrediente, PrecoHistorico, ROTULOS_ORIGEM_PRECO, ROTULOS_UNIDADE } from '../../core/models';
import { GraficoPrecosComponent } from './grafico-precos.component';

// RF-023 (cotação comparativa: todos os fornecedores do ingrediente, do mais barato para o mais caro)
// e RF-026 (gráfico de evolução do preço). Fluxo 8.3 do SRS: alerta de estoque > cotação > pedido de compra.
@Component({
  selector: 'app-cotacao-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe, GraficoPrecosComponent],
  templateUrl: './cotacao-admin.component.html',
})
export class CotacaoAdminComponent {
  private api = inject(AdminFornecedorService);
  private router = inject(Router);
  rotulosUnidade = ROTULOS_UNIDADE;
  rotulosOrigem = ROTULOS_ORIGEM_PRECO;

  ingredientes = signal<Ingrediente[]>([]);
  escolhido = new FormControl<number | null>(null);
  cotacao = signal<Cotacao | null>(null);
  historico = signal<PrecoHistorico[]>([]);
  carregando = signal(false);
  erro = signal('');
  verTabela = signal(false);

  constructor() {
    inject(AdminIngredienteService).todos().subscribe({
      next: l => this.ingredientes.set(l.filter(i => i.status === 'ATIVO')),
      error: () => this.erro.set('Não foi possível carregar os ingredientes. Confira se o servidor está no ar.'),
    });
    // ?ingredienteId=14 (vindo do estoque ou do catálogo) já abre a cotação dele
    const inicial = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('ingredienteId')) || null;
    this.escolhido.valueChanges.pipe(
      startWith(inicial),
      tap(id => {
        this.cotacao.set(null); this.historico.set([]); this.erro.set('');
        this.carregando.set(!!id);
        this.router.navigate([], { queryParams: { ingredienteId: id }, replaceUrl: true });
      }),
      switchMap(id => !id ? of(null) : forkJoin({ c: this.api.cotacao(id), h: this.api.historico(id) }).pipe(catchError(e => {
        this.erro.set(e.status === 404 ? 'Ingrediente não encontrado.' : 'Não foi possível carregar a cotação.');
        return of(null);
      }))),
      takeUntilDestroyed(),
    ).subscribe(r => {
      this.carregando.set(false);
      if (r) { this.cotacao.set(r.c); this.historico.set(r.h); }
    });
    if (inicial) this.escolhido.setValue(inicial, { emitEvent: false });
  }

  // Quanto o preço do fornecedor está abaixo (negativo) ou acima do custo atual do ingrediente
  vsCusto(c: Cotacao, preco: number): number | null { return c.custoAtual > 0 ? (preco - c.custoAtual) / c.custoAtual * 100 : null; }
  historicoRecente() { return [...this.historico()].reverse(); }
}
