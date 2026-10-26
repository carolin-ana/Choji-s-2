import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, merge, of, startWith, switchMap, tap } from 'rxjs';
import { AdminFornecedorService } from '../../core/admin-fornecedor.service';
import { AdminIngredienteService } from '../../core/admin-ingrediente.service';
import { ToastService } from '../../core/toast.service';
import { cnpjValidator, mascararCnpj } from '../../core/cnpj';
import { CatalogoItem, Fornecedor, Ingrediente, Pagina, ROTULOS_UNIDADE, StatusRegistro } from '../../core/models';

// RF-021 (cadastro de fornecedores, com CNPJ validado, RN07) e RF-022 (catálogo de cada fornecedor)
@Component({
  selector: 'app-fornecedores-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './fornecedores-admin.component.html',
})
export class FornecedoresAdminComponent {
  private api = inject(AdminFornecedorService);
  private ingApi = inject(AdminIngredienteService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  rotulosUnidade = ROTULOS_UNIDADE;

  filtros = this.fb.nonNullable.group({ busca: '', status: '' as StatusRegistro | '' });
  pagina = signal<Pagina<Fornecedor> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);

  // ---- Janela de criar/editar fornecedor ----
  formAberto = signal(false);
  editando = signal<Fornecedor | null>(null);
  salvando = signal(false);
  erroForm = signal('');
  form = this.fb.nonNullable.group({
    razaoSocial: ['', [Validators.required, Validators.maxLength(150)]],
    cnpj: ['', [Validators.required, cnpjValidator]],
    telefone: ['', Validators.maxLength(30)],
    email: ['', [Validators.email, Validators.maxLength(160)]],
    categorias: ['', Validators.maxLength(255)],
  });

  desativando = signal<Fornecedor | null>(null);
  ocupado = signal<number | null>(null);

  // ---- Janela do catálogo (RF-022) ----
  catalogoDe = signal<Fornecedor | null>(null);
  itens = signal<CatalogoItem[]>([]);
  carregandoCatalogo = signal(false);
  ingredientes = signal<Ingrediente[]>([]);
  editandoItem = signal<CatalogoItem | null>(null); // null = adicionando um novo
  salvandoItem = signal(false);
  erroItem = signal('');
  itemForm = this.fb.group({
    ingredienteId: this.fb.control<number | null>(null, Validators.required),
    preco: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.0001)]),
    unidadeVenda: this.fb.nonNullable.control('', Validators.maxLength(60)),
  });

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
    // máscara do CNPJ enquanto digita
    this.form.controls.cnpj.valueChanges.pipe(takeUntilDestroyed()).subscribe(v => {
      const m = mascararCnpj(v);
      if (m !== v) this.form.controls.cnpj.setValue(m, { emitEvent: false });
    });
  }

  irPara(n: number) { this.numeroPagina.set(n); this.recarregar$.next(); }
  limpar() { this.filtros.reset(); }
  temFiltro() { const f = this.filtros.getRawValue(); return !!(f.busca || f.status); }
  invalido(c: keyof typeof this.form.controls) { const x = this.form.controls[c]; return x.invalid && (x.touched || x.dirty); }

  // ================= Fornecedor =================

  abrirNovo() {
    this.editando.set(null);
    this.form.reset({ razaoSocial: '', cnpj: '', telefone: '', email: '', categorias: '' });
    this.erroForm.set(''); this.formAberto.set(true);
  }
  abrirEdicao(f: Fornecedor) {
    this.editando.set(f);
    this.form.reset({ razaoSocial: f.razaoSocial, cnpj: f.cnpjFormatado, telefone: f.telefone ?? '', email: f.email ?? '', categorias: f.categorias ?? '' });
    this.erroForm.set(''); this.formAberto.set(true);
  }

  salvar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.salvando.set(true); this.erroForm.set('');
    const atual = this.editando();
    const dados = this.form.getRawValue();
    const req = atual ? this.api.atualizar(atual.id, dados) : this.api.criar(dados);
    req.subscribe({
      next: f => {
        this.salvando.set(false); this.formAberto.set(false);
        this.toast.sucesso(atual ? `${f.razaoSocial} atualizado.` : `${f.razaoSocial} cadastrado. Agora monte o catálogo dele.`);
        this.recarregar$.next();
        if (!atual) this.abrirCatalogo(f);
      },
      error: e => { this.salvando.set(false); const m = e.error?.message ?? 'Não foi possível salvar.'; this.erroForm.set(m); this.toast.erro(m); },
    });
  }

  confirmarDesativacao() {
    const f = this.desativando(); if (!f) return;
    this.ocupado.set(f.id);
    this.api.desativar(f.id).subscribe({
      next: r => { this.ocupado.set(null); this.desativando.set(null); this.toast.sucesso(`${r.razaoSocial} desativado.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível desativar.'); },
    });
  }
  reativar(f: Fornecedor) {
    this.ocupado.set(f.id);
    this.api.reativar(f.id).subscribe({
      next: r => { this.ocupado.set(null); this.toast.sucesso(`${r.razaoSocial} reativado.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível reativar.'); },
    });
  }

  // ================= Catálogo =================

  abrirCatalogo(f: Fornecedor) {
    this.catalogoDe.set(f);
    this.itens.set([]);
    this.novoItem();
    this.carregarCatalogo();
    if (!this.ingredientes().length) this.ingApi.todos().subscribe({ next: l => this.ingredientes.set(l), error: () => {} });
  }
  fecharCatalogo() { this.catalogoDe.set(null); this.recarregar$.next(); } // atualiza a contagem de itens na lista

  private carregarCatalogo() {
    const f = this.catalogoDe(); if (!f) return;
    this.carregandoCatalogo.set(true);
    this.api.catalogo(f.id).subscribe({
      next: l => { this.carregandoCatalogo.set(false); this.itens.set(l); },
      error: () => { this.carregandoCatalogo.set(false); this.toast.erro('Não foi possível carregar o catálogo.'); },
    });
  }

  // Ingredientes que dá para adicionar: ativos e que ainda não estão no catálogo
  disponiveis(): Ingrediente[] {
    const usados = new Set(this.itens().map(i => i.ingredienteId));
    return this.ingredientes().filter(i => i.status === 'ATIVO' && !usados.has(i.id));
  }
  unidadeEscolhida(): string {
    const it = this.editandoItem();
    if (it) return this.rotulosUnidade[it.unidade];
    const ing = this.ingredientes().find(i => i.id === this.itemForm.controls.ingredienteId.value);
    return ing ? this.rotulosUnidade[ing.unidadePadrao] : 'unidade';
  }

  novoItem() {
    this.editandoItem.set(null);
    this.itemForm.reset({ ingredienteId: null, preco: null, unidadeVenda: '' });
    this.itemForm.controls.ingredienteId.enable();
    this.erroItem.set('');
  }
  editarItem(i: CatalogoItem) {
    this.editandoItem.set(i);
    this.itemForm.reset({ ingredienteId: i.ingredienteId, preco: i.preco, unidadeVenda: i.unidadeVenda ?? '' });
    this.itemForm.controls.ingredienteId.disable(); // na edição só muda preço e embalagem
    this.erroItem.set('');
  }

  salvarItem() {
    const f = this.catalogoDe(); if (!f) return;
    if (this.itemForm.invalid) { this.itemForm.markAllAsTouched(); return; }
    this.salvandoItem.set(true); this.erroItem.set('');
    const it = this.editandoItem();
    const dados = this.itemForm.getRawValue();
    const req = it ? this.api.atualizarItem(f.id, it.id, dados) : this.api.adicionarItem(f.id, dados);
    req.subscribe({
      next: r => {
        this.salvandoItem.set(false);
        this.toast.sucesso(it ? `Preço de ${r.ingrediente} atualizado.` : `${r.ingrediente} entrou no catálogo.`);
        this.novoItem();
        this.carregarCatalogo();
      },
      error: e => { this.salvandoItem.set(false); const m = e.error?.message ?? 'Não foi possível salvar o item.'; this.erroItem.set(m); this.toast.erro(m); },
    });
  }

  removerItem(i: CatalogoItem) {
    const f = this.catalogoDe(); if (!f) return;
    this.api.removerItem(f.id, i.id).subscribe({
      next: () => { this.toast.sucesso(`${i.ingrediente} saiu do catálogo.`); if (this.editandoItem()?.id === i.id) this.novoItem(); this.carregarCatalogo(); },
      error: e => this.toast.erro(e.error?.message ?? 'Não foi possível remover.'),
    });
  }

  // Diferença do preço do fornecedor para o custo atual do ingrediente (negativo = mais barato)
  diferenca(i: CatalogoItem): number | null {
    return i.custoAtual > 0 ? ((i.preco - i.custoAtual) / i.custoAtual) * 100 : null;
  }
}
