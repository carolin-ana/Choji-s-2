import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, merge, of, startWith, switchMap, tap } from 'rxjs';
import { AdminCardapioService } from '../../core/admin-cardapio.service';
import { ToastService } from '../../core/toast.service';
import { Categoria, Pagina, ROTULOS_STATUS_REGISTRO, StatusRegistro } from '../../core/models';

// RF-009: criar, editar, listar e desativar categorias
@Component({
  selector: 'app-categorias-admin',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './categorias-admin.component.html',
})
export class CategoriasAdminComponent {
  private api = inject(AdminCardapioService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  rotulosStatus = ROTULOS_STATUS_REGISTRO;

  filtros = this.fb.nonNullable.group({ busca: '', status: '' as StatusRegistro | '' });
  pagina = signal<Pagina<Categoria> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);

  formAberto = signal(false);
  editando = signal<Categoria | null>(null);
  salvando = signal(false);
  erroForm = signal('');
  form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(80)]],
    descricao: ['', Validators.maxLength(255)],
    ordem: [0, [Validators.required, Validators.min(0), Validators.max(999)]],
  });
  desativando = signal<Categoria | null>(null);
  ocupado = signal<number | null>(null);
  private recarregar$ = new Subject<void>();

  constructor() {
    merge(
      this.recarregar$.pipe(startWith(undefined)),
      this.filtros.valueChanges.pipe(debounceTime(300), tap(() => this.numeroPagina.set(0))),
    ).pipe(
      switchMap(() => this.api.listarCategorias(this.filtros.getRawValue(), this.numeroPagina()).pipe(catchError(() => of(null)))),
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
  invalido(c: 'nome' | 'descricao' | 'ordem') { const x = this.form.controls[c]; return x.invalid && (x.touched || x.dirty); }

  abrirNova() {
    this.editando.set(null);
    // sugere a próxima posição depois da última categoria
    const maior = Math.max(0, ...(this.pagina()?.content.map(c => c.ordem) ?? [0]));
    this.form.reset({ nome: '', descricao: '', ordem: maior + 1 });
    this.erroForm.set(''); this.formAberto.set(true);
  }
  abrirEdicao(c: Categoria) {
    this.editando.set(c);
    this.form.reset({ nome: c.nome, descricao: c.descricao ?? '', ordem: c.ordem });
    this.erroForm.set(''); this.formAberto.set(true);
  }

  salvar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.salvando.set(true); this.erroForm.set('');
    const atual = this.editando();
    const dados = this.form.getRawValue();
    const req = atual ? this.api.atualizarCategoria(atual.id, dados) : this.api.criarCategoria(dados);
    req.subscribe({
      next: c => {
        this.salvando.set(false); this.formAberto.set(false);
        this.toast.sucesso(atual ? `Categoria ${c.nome} atualizada.` : `Categoria ${c.nome} criada.`);
        this.recarregar$.next();
      },
      error: e => { this.salvando.set(false); const m = e.error?.message ?? 'Não foi possível salvar.'; this.erroForm.set(m); this.toast.erro(m); },
    });
  }

  confirmarDesativacao() {
    const c = this.desativando(); if (!c) return;
    this.ocupado.set(c.id);
    this.api.desativarCategoria(c.id).subscribe({
      next: r => { this.ocupado.set(null); this.desativando.set(null); this.toast.sucesso(`Categoria ${r.nome} desativada.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível desativar.'); },
    });
  }
  reativar(c: Categoria) {
    this.ocupado.set(c.id);
    this.api.reativarCategoria(c.id).subscribe({
      next: r => { this.ocupado.set(null); this.toast.sucesso(`Categoria ${r.nome} reativada.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível reativar.'); },
    });
  }
}
