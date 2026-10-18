import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, merge, of, startWith, switchMap, tap } from 'rxjs';
import { AdminIngredienteService } from '../../core/admin-ingrediente.service';
import { ToastService } from '../../core/toast.service';
import { Ingrediente, Pagina, ROTULOS_UNIDADE, StatusRegistro, UNIDADES, UnidadeMedida } from '../../core/models';

// RF-027: cadastro de ingredientes. O custo unitário daqui alimenta o custo das fichas técnicas.
@Component({
  selector: 'app-ingredientes-admin',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, DecimalPipe],
  templateUrl: './ingredientes-admin.component.html',
})
export class IngredientesAdminComponent {
  private api = inject(AdminIngredienteService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  unidades = UNIDADES;
  rotulosUnidade = ROTULOS_UNIDADE;

  filtros = this.fb.nonNullable.group({ busca: '', status: '' as StatusRegistro | '' });
  pagina = signal<Pagina<Ingrediente> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);

  formAberto = signal(false);
  editando = signal<Ingrediente | null>(null);
  salvando = signal(false);
  erroForm = signal('');
  form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    sku: ['', [Validators.required, Validators.maxLength(40)]],
    unidadePadrao: ['G' as UnidadeMedida, Validators.required],
    custoUnitario: [0, [Validators.required, Validators.min(0)]],
    estoqueMinimo: [0, [Validators.required, Validators.min(0)]],
  });
  desativando = signal<Ingrediente | null>(null);
  ocupado = signal<number | null>(null);
  private recarregar$ = new Subject<void>();

  constructor() {
    merge(
      this.recarregar$.pipe(startWith(undefined)),
      this.filtros.valueChanges.pipe(debounceTime(300), tap(() => this.numeroPagina.set(0))),
    ).pipe(
      switchMap(() => this.api.listar(this.filtros.getRawValue(), this.numeroPagina()).pipe(catchError(() => of(null)))),
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
  limpar() { this.filtros.reset(); }
  temFiltro() { const f = this.filtros.getRawValue(); return !!(f.busca || f.status); }
  invalido(c: 'nome' | 'sku' | 'unidadePadrao' | 'custoUnitario' | 'estoqueMinimo') {
    const x = this.form.controls[c]; return x.invalid && (x.touched || x.dirty);
  }

  abrirNovo() {
    this.editando.set(null);
    this.form.reset({ nome: '', sku: '', unidadePadrao: 'G', custoUnitario: 0, estoqueMinimo: 0 });
    this.erroForm.set(''); this.formAberto.set(true);
  }
  abrirEdicao(i: Ingrediente) {
    this.editando.set(i);
    this.form.reset({ nome: i.nome, sku: i.sku, unidadePadrao: i.unidadePadrao, custoUnitario: i.custoUnitario, estoqueMinimo: i.estoqueMinimo });
    this.erroForm.set(''); this.formAberto.set(true);
  }

  salvar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.salvando.set(true); this.erroForm.set('');
    const atual = this.editando();
    const dados = this.form.getRawValue();
    const req = atual ? this.api.atualizar(atual.id, dados) : this.api.criar(dados);
    req.subscribe({
      next: i => {
        this.salvando.set(false); this.formAberto.set(false);
        this.toast.sucesso(atual ? `${i.nome} atualizado. O custo das fichas que usam este ingrediente já foi recalculado.` : `${i.nome} cadastrado.`);
        this.recarregar$.next();
      },
      error: e => { this.salvando.set(false); const m = e.error?.message ?? 'Não foi possível salvar.'; this.erroForm.set(m); this.toast.erro(m); },
    });
  }

  confirmarDesativacao() {
    const i = this.desativando(); if (!i) return;
    this.ocupado.set(i.id);
    this.api.desativar(i.id).subscribe({
      next: r => { this.ocupado.set(null); this.desativando.set(null); this.toast.sucesso(`${r.nome} desativado.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível desativar.'); },
    });
  }
  reativar(i: Ingrediente) {
    this.ocupado.set(i.id);
    this.api.reativar(i.id).subscribe({
      next: r => { this.ocupado.set(null); this.toast.sucesso(`${r.nome} reativado.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível reativar.'); },
    });
  }
}
