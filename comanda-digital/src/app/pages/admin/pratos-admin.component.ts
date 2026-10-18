import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, merge, of, startWith, switchMap, tap } from 'rxjs';
import { AdminCardapioService } from '../../core/admin-cardapio.service';
import { ToastService } from '../../core/toast.service';
import { Categoria, Pagina, PratoAdmin, ROTULOS_FAIXA, ROTULOS_STATUS_REGISTRO, StatusRegistro } from '../../core/models';

// RF-010: lista de pratos do painel, com custo e food cost de cada um (RF-012/013)
@Component({
  selector: 'app-pratos-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DecimalPipe],
  templateUrl: './pratos-admin.component.html',
})
export class PratosAdminComponent {
  private api = inject(AdminCardapioService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  rotulosStatus = ROTULOS_STATUS_REGISTRO;
  rotulosFaixa = ROTULOS_FAIXA;

  categorias = signal<Categoria[]>([]);
  filtros = this.fb.group({
    busca: this.fb.nonNullable.control(''),
    categoriaId: this.fb.control<number | null>(null),
    status: this.fb.nonNullable.control<StatusRegistro | ''>(''),
  });
  pagina = signal<Pagina<PratoAdmin> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);
  desativando = signal<PratoAdmin | null>(null);
  ocupado = signal<number | null>(null);
  private recarregar$ = new Subject<void>();

  constructor() {
    this.api.todasCategorias().subscribe({ next: c => this.categorias.set(c), error: () => {} });
    merge(
      this.recarregar$.pipe(startWith(undefined)),
      this.filtros.valueChanges.pipe(debounceTime(300), tap(() => this.numeroPagina.set(0))),
    ).pipe(
      switchMap(() => this.api.listarPratos(this.filtros.getRawValue(), this.numeroPagina()).pipe(catchError(() => of(null)))),
      takeUntilDestroyed(),
    ).subscribe(pg => {
      this.carregando.set(false);
      this.erroLista.set(!pg);
      if (!pg) return;
      if (pg.content.length === 0 && pg.page > 0) { this.irPara(Math.max(pg.totalPages - 1, 0)); return; }
      this.pagina.set(pg);
    });
  }

  irPara(n: number) { this.numeroPagina.set(n); this.recarregar$.next(); }
  limpar() { this.filtros.reset({ busca: '', categoriaId: null, status: '' }); }
  temFiltro() { const f = this.filtros.getRawValue(); return !!(f.busca || f.categoriaId || f.status); }

  confirmarDesativacao() {
    const p = this.desativando(); if (!p) return;
    this.ocupado.set(p.id);
    this.api.desativarPrato(p.id).subscribe({
      next: r => { this.ocupado.set(null); this.desativando.set(null); this.toast.sucesso(`${r.nome} saiu do cardápio.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível desativar.'); },
    });
  }
}
