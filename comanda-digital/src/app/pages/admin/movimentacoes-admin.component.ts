import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, merge, of, startWith, switchMap, tap } from 'rxjs';
import { AdminIngredienteService } from '../../core/admin-ingrediente.service';
import { EstoqueService } from '../../core/estoque.service';
import { Ingrediente, Movimentacao, Pagina, ROTULOS_MOTIVO, ROTULOS_TIPO_MOV, ROTULOS_UNIDADE, TipoMovimentacao } from '../../core/models';

// RF-033: todas as entradas e saídas com data, tipo, quantidade, motivo, usuário e referência (pedido ou compra)
@Component({
  selector: 'app-movimentacoes-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './movimentacoes-admin.component.html',
})
export class MovimentacoesAdminComponent {
  private estoque = inject(EstoqueService);
  private fb = inject(FormBuilder);
  rotulosTipo = ROTULOS_TIPO_MOV;
  rotulosMotivo = ROTULOS_MOTIVO;
  rotulosUnidade = ROTULOS_UNIDADE;
  tipos: TipoMovimentacao[] = ['ENTRADA', 'SAIDA', 'ESTORNO'];

  ingredientes = signal<Ingrediente[]>([]);
  filtros = this.fb.group({
    ingredienteId: this.fb.control<number | null>(null),
    tipo: this.fb.nonNullable.control<TipoMovimentacao | ''>(''),
    dataInicio: this.fb.nonNullable.control(''),
    dataFim: this.fb.nonNullable.control(''),
  });
  pagina = signal<Pagina<Movimentacao> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);
  private recarregar$ = new Subject<void>();

  constructor() {
    // vindo do botão "Histórico" da tela de estoque: já abre filtrado pelo ingrediente
    const id = inject(ActivatedRoute).snapshot.queryParamMap.get('ingredienteId');
    if (id) this.filtros.patchValue({ ingredienteId: Number(id) }, { emitEvent: false });
    inject(AdminIngredienteService).todos().subscribe({ next: l => this.ingredientes.set(l), error: () => {} });

    merge(
      this.recarregar$.pipe(startWith(undefined)),
      this.filtros.valueChanges.pipe(tap(() => this.numeroPagina.set(0))),
    ).pipe(
      switchMap(() => this.estoque.movimentacoes(this.filtros.getRawValue(), this.numeroPagina()).pipe(catchError(() => of(null)))),
      takeUntilDestroyed(),
    ).subscribe(pg => {
      this.carregando.set(false);
      this.erroLista.set(!pg);
      if (pg) this.pagina.set(pg);
    });
  }

  irPara(n: number) { this.numeroPagina.set(n); this.recarregar$.next(); }
  limpar() { this.filtros.reset({ ingredienteId: null, tipo: '', dataInicio: '', dataFim: '' }); }
  temFiltro() { const f = this.filtros.getRawValue(); return !!(f.ingredienteId || f.tipo || f.dataInicio || f.dataFim); }
}
