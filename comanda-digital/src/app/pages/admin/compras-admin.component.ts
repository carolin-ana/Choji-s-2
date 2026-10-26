import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, merge, of, startWith, switchMap, tap } from 'rxjs';
import { AdminCompraService } from '../../core/admin-compra.service';
import { AdminFornecedorService } from '../../core/admin-fornecedor.service';
import { Compra, Fornecedor, Pagina, ROTULOS_STATUS_COMPRA, STATUS_COMPRA, StatusCompra } from '../../core/models';

// RF-024: lista paginada dos pedidos de compra, com filtro por situação e fornecedor
@Component({
  selector: 'app-compras-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './compras-admin.component.html',
})
export class ComprasAdminComponent {
  private api = inject(AdminCompraService);
  private fb = inject(FormBuilder);
  rotulos = ROTULOS_STATUS_COMPRA;
  statusOpcoes = STATUS_COMPRA;

  fornecedores = signal<Fornecedor[]>([]);
  filtros = this.fb.group({
    status: this.fb.nonNullable.control<StatusCompra | ''>(''),
    fornecedorId: this.fb.control<number | null>(null),
  });
  pagina = signal<Pagina<Compra> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);
  private recarregar$ = new Subject<void>();

  constructor() {
    inject(AdminFornecedorService).todos().subscribe({ next: l => this.fornecedores.set(l), error: () => {} });
    merge(
      this.recarregar$.pipe(startWith(undefined)),
      this.filtros.valueChanges.pipe(tap(() => this.numeroPagina.set(0))),
    ).pipe(
      switchMap(() => this.api.listar(this.filtros.getRawValue(), this.numeroPagina()).pipe(catchError(() => of(null)))),
      takeUntilDestroyed(),
    ).subscribe(pg => {
      this.carregando.set(false);
      this.erroLista.set(!pg);
      if (pg) this.pagina.set(pg);
    });
  }

  irPara(n: number) { this.numeroPagina.set(n); this.recarregar$.next(); }
  limpar() { this.filtros.reset({ status: '', fornecedorId: null }); }
  temFiltro() { const f = this.filtros.getRawValue(); return !!(f.status || f.fornecedorId); }
  resumoItens(c: Compra) {
    const nomes = c.itens.map(i => i.ingrediente);
    return nomes.length <= 2 ? nomes.join(', ') : `${nomes.slice(0, 2).join(', ')} e mais ${nomes.length - 2}`;
  }
}
