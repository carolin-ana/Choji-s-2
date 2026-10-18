import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, merge, of, startWith, switchMap, tap } from 'rxjs';
import { EstoqueService } from '../../core/estoque.service';
import { ToastService } from '../../core/toast.service';
import { EstoqueSaldo, MOTIVOS_SAIDA, MotivoMovimentacao, Pagina, ROTULOS_MOTIVO, ROTULOS_UNIDADE, TipoMovimentacao } from '../../core/models';

// RF-031 (saldo em tempo real), RF-032 (alertas), RF-028 (entrada manual) e RF-030 (saída manual com motivo)
@Component({
  selector: 'app-estoque-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './estoque-admin.component.html',
})
export class EstoqueAdminComponent {
  estoque = inject(EstoqueService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  rotulosUnidade = ROTULOS_UNIDADE;
  rotulosMotivo = ROTULOS_MOTIVO;
  motivosSaida = MOTIVOS_SAIDA;

  filtros = this.fb.nonNullable.group({ busca: '', somenteAlertas: false });
  pagina = signal<Pagina<EstoqueSaldo> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);

  // Janela de entrada/saída
  movendo = signal<{ item: EstoqueSaldo; tipo: TipoMovimentacao } | null>(null);
  salvando = signal(false);
  erroForm = signal('');
  form = this.fb.group({
    motivo: this.fb.nonNullable.control<MotivoMovimentacao>('AJUSTE', Validators.required),
    quantidade: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.001)]),
    lote: this.fb.nonNullable.control('', Validators.maxLength(60)),
    validade: this.fb.nonNullable.control(''),
    custoUnitario: this.fb.control<number | null>(null, Validators.min(0)),
    observacao: this.fb.nonNullable.control('', Validators.maxLength(255)),
  });
  private recarregar$ = new Subject<void>();

  constructor() {
    merge(
      this.recarregar$.pipe(startWith(undefined)),
      this.filtros.valueChanges.pipe(debounceTime(300), tap(() => this.numeroPagina.set(0))),
    ).pipe(
      switchMap(() => {
        const f = this.filtros.getRawValue();
        // "Só abaixo do mínimo" usa o endpoint de alertas (RF-032); o normal usa o de saldo (RF-031)
        const req = f.somenteAlertas ? this.estoque.listarAlertas(this.numeroPagina())
                                     : this.estoque.saldo({ busca: f.busca, status: '' }, this.numeroPagina());
        return req.pipe(catchError(() => of(null)));
      }),
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
  verAlertas() { this.filtros.setValue({ busca: '', somenteAlertas: true }); }
  limpar() { this.filtros.setValue({ busca: '', somenteAlertas: false }); }
  temFiltro() { const f = this.filtros.getRawValue(); return !!(f.busca || f.somenteAlertas); }

  abrir(item: EstoqueSaldo, tipo: TipoMovimentacao) {
    this.form.reset({
      motivo: tipo === 'ENTRADA' ? 'AJUSTE' : 'DESPERDICIO', quantidade: null, lote: '', validade: '',
      custoUnitario: tipo === 'ENTRADA' ? item.custoUnitario : null, observacao: '',
    });
    // RF-030: na saída o motivo é obrigatório; na entrada manual é sempre "Ajuste"
    this.erroForm.set('');
    this.movendo.set({ item, tipo });
  }

  quantidadeInvalida() { const c = this.form.controls.quantidade; return c.invalid && (c.touched || c.dirty); }
  passaDoSaldo() {
    const m = this.movendo(); const q = this.form.controls.quantidade.value;
    return !!m && m.tipo === 'SAIDA' && q !== null && q > m.item.saldo;
  }

  salvar() {
    const m = this.movendo(); if (!m) return;
    if (this.form.invalid || this.passaDoSaldo()) { this.form.markAllAsTouched(); return; }
    this.salvando.set(true); this.erroForm.set('');
    const v = this.form.getRawValue();
    this.estoque.registrar({ ingredienteId: m.item.ingredienteId, tipo: m.tipo, ...v }).subscribe({
      next: r => {
        this.salvando.set(false); this.movendo.set(null);
        const un = this.rotulosUnidade[r.unidade];
        this.toast.sucesso(`${m.tipo === 'ENTRADA' ? 'Entrada' : 'Saída'} de ${r.quantidade} ${un} de ${r.ingrediente} registrada.`);
        this.estoque.atualizarAlertas();
        this.recarregar$.next();
      },
      error: e => { this.salvando.set(false); const msg = e.error?.message ?? 'Não foi possível registrar.'; this.erroForm.set(msg); this.toast.erro(msg); },
    });
  }
}
