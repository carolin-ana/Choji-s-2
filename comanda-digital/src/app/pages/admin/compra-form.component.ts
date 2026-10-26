import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';
import { AdminCompraService } from '../../core/admin-compra.service';
import { AdminFornecedorService } from '../../core/admin-fornecedor.service';
import { EstoqueService } from '../../core/estoque.service';
import { ToastService } from '../../core/toast.service';
import {
  CatalogoItem, Compra, Fornecedor, ROTULOS_FAIXA, ROTULOS_STATUS_COMPRA, ROTULOS_UNIDADE, Recebimento, UnidadeMedida,
} from '../../core/models';

type LinhaCompra = FormGroup<{
  ingredienteId: FormControl<number | null>;
  quantidade: FormControl<number | null>;
  precoUnitario: FormControl<number | null>;
}>;

// RF-024 (montar o pedido a partir do catálogo do fornecedor, com total) e RF-025 (registrar o recebimento).
// Rascunho = formulário editável. Enviado/recebido/cancelado = só leitura, com as ações possíveis.
@Component({
  selector: 'app-compra-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe],
  templateUrl: './compra-form.component.html',
})
export class CompraFormComponent {
  private api = inject(AdminCompraService);
  private fornApi = inject(AdminFornecedorService);
  private estoque = inject(EstoqueService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  rotulos = ROTULOS_STATUS_COMPRA;
  rotulosUnidade = ROTULOS_UNIDADE;
  rotulosFaixa = ROTULOS_FAIXA;

  compraId = signal<number | null>(null);
  compra = signal<Compra | null>(null);
  fornecedores = signal<Fornecedor[]>([]);
  catalogo = signal<CatalogoItem[]>([]);
  carregando = signal(true);
  erroCarga = signal('');
  salvando = signal(false);
  erroForm = signal('');
  editavel = computed(() => !this.compra() || this.compra()!.status === 'RASCUNHO');

  // janelas de receber e cancelar
  recebendo = signal(false);
  cancelando = signal(false);
  ocupado = signal(false);
  recebimento = signal<Recebimento | null>(null);
  receberForm = this.fb.nonNullable.group({ lote: ['', Validators.maxLength(60)], validade: [''], observacao: ['', Validators.maxLength(255)] });
  motivo = this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]);

  form = this.fb.group({
    fornecedorId: this.fb.control<number | null>(null, Validators.required),
    observacoes: this.fb.nonNullable.control('', Validators.maxLength(500)),
    itens: this.fb.array<LinhaCompra>([]),
  });
  get itens(): FormArray<LinhaCompra> { return this.form.controls.itens; }
  private valores = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  // Total estimado na tela (o valor oficial é o que o back calcula ao salvar)
  total = computed(() => {
    this.valores();
    return this.itens.controls.reduce((s, l) => s + this.subtotal(l), 0);
  });

  private prefill: { ingredienteId: number | null } = { ingredienteId: null };

  constructor() {
    const rota = inject(ActivatedRoute).snapshot;
    const id = rota.paramMap.get('id') ? Number(rota.paramMap.get('id')) : null;
    this.compraId.set(id);

    // Trocou o fornecedor: carrega o catálogo dele e ajusta as linhas
    this.form.controls.fornecedorId.valueChanges.pipe(
      switchMap(fid => fid ? this.fornApi.catalogo(fid).pipe(catchError(() => { this.toast.erro('Não foi possível carregar o catálogo do fornecedor.'); return of([]); })) : of([])),
      takeUntilDestroyed(),
    ).subscribe(cat => this.aplicarCatalogo(cat));

    this.fornApi.todos().subscribe({ next: l => this.fornecedores.set(l), error: () => {} });

    if (id) {
      this.api.buscar(id).subscribe({
        next: c => { this.carregarCompra(c); this.carregando.set(false); },
        error: e => { this.carregando.set(false); this.erroCarga.set(e.status === 404 ? 'Pedido de compra não encontrado.' : 'Não foi possível carregar o pedido.'); },
      });
    } else {
      // Vindo da cotação: /admin/compras/novo?fornecedorId=2&ingredienteId=14
      const q = rota.queryParamMap;
      this.prefill.ingredienteId = Number(q.get('ingredienteId')) || null;
      const fid = Number(q.get('fornecedorId')) || null;
      this.carregando.set(false);
      if (fid) this.form.controls.fornecedorId.setValue(fid);
      else this.itens.push(this.novaLinha());
    }
  }

  private carregarCompra(c: Compra) {
    this.compra.set(c);
    this.itens.clear({ emitEvent: false });
    c.itens.forEach(i => this.itens.push(this.novaLinha(i.ingredienteId, i.quantidade, i.precoUnitario), { emitEvent: false }));
    this.form.controls.observacoes.setValue(c.observacoes ?? '', { emitEvent: false });
    this.form.controls.fornecedorId.setValue(c.fornecedorId); // dispara a carga do catálogo
  }

  private aplicarCatalogo(cat: CatalogoItem[]) {
    this.catalogo.set(cat);
    const ids = new Set(cat.map(c => c.ingredienteId));
    // linhas com ingrediente que este fornecedor não vende ficam em branco
    this.itens.controls.forEach(l => { if (l.controls.ingredienteId.value && !ids.has(l.controls.ingredienteId.value)) l.reset({ ingredienteId: null, quantidade: l.controls.quantidade.value, precoUnitario: null }); });
    if (this.prefill.ingredienteId && ids.has(this.prefill.ingredienteId)) {
      const item = cat.find(c => c.ingredienteId === this.prefill.ingredienteId)!;
      this.itens.push(this.novaLinha(item.ingredienteId, null, item.preco));
      this.prefill.ingredienteId = null;
    }
    if (!this.itens.length) this.itens.push(this.novaLinha());
  }

  novaLinha(ingredienteId: number | null = null, quantidade: number | null = null, precoUnitario: number | null = null): LinhaCompra {
    const linha: LinhaCompra = this.fb.group({
      ingredienteId: this.fb.control<number | null>(ingredienteId, Validators.required),
      quantidade: this.fb.control<number | null>(quantidade, [Validators.required, Validators.min(0.001)]),
      precoUnitario: this.fb.control<number | null>(precoUnitario, [Validators.required, Validators.min(0.0001)]),
    });
    // escolheu o ingrediente: o preço vem do catálogo (dá para negociar e mudar)
    linha.controls.ingredienteId.valueChanges.subscribe(iid => {
      const item = this.catalogo().find(c => c.ingredienteId === iid);
      if (item) linha.controls.precoUnitario.setValue(item.preco);
    });
    return linha;
  }
  adicionarLinha() { this.itens.push(this.novaLinha()); }
  removerLinha(i: number) { if (this.itens.length > 1) this.itens.removeAt(i); }

  itemDa(l: LinhaCompra): CatalogoItem | undefined { return this.catalogo().find(c => c.ingredienteId === l.controls.ingredienteId.value); }
  usadoEmOutraLinha(id: number, l: LinhaCompra) { return this.itens.controls.some(x => x !== l && x.controls.ingredienteId.value === id); }
  subtotal(l: LinhaCompra): number {
    const q = l.controls.quantidade.value ?? 0, p = l.controls.precoUnitario.value ?? 0;
    return Math.round(q * p * 100) / 100;
  }
  invalida(l: LinhaCompra, c: 'ingredienteId' | 'quantidade' | 'precoUnitario') { const x = l.controls[c]; return x.invalid && (x.touched || x.dirty); }
  // 3000 g -> "3 kg", para conferir a quantidade
  conversao(q: number | null, u: UnidadeMedida): string {
    if (!q || q < 1000 || (u !== 'G' && u !== 'ML')) return '';
    return '= ' + (q / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 3 }) + (u === 'G' ? ' kg' : ' L');
  }
  fornecedoresSelecionaveis() {
    const atual = this.form.controls.fornecedorId.value;
    return this.fornecedores().filter(f => f.status === 'ATIVO' || f.id === atual);
  }

  // ================= Ações =================

  salvar(enviar: boolean) {
    if (this.form.invalid) { this.form.markAllAsTouched(); this.erroForm.set('Confira os campos destacados.'); return; }
    this.salvando.set(true); this.erroForm.set('');
    const v = this.form.getRawValue();
    const dados = { fornecedorId: v.fornecedorId, observacoes: v.observacoes, itens: v.itens };
    const id = this.compraId();
    const req = (id ? this.api.atualizar(id, dados) : this.api.criar(dados)).pipe(
      switchMap(c => enviar ? this.api.enviar(c.id) : of(c)),
    );
    req.subscribe({
      next: c => {
        this.salvando.set(false);
        this.toast.sucesso(enviar ? `Pedido de compra #${c.id} enviado para ${c.fornecedor}.` : `Rascunho #${c.id} salvo.`);
        if (!id) { this.router.navigate(['/admin/compras', c.id]); return; }
        this.carregarCompra(c);
      },
      error: e => {
        this.salvando.set(false);
        const m = e.error?.message ?? 'Não foi possível salvar o pedido de compra.';
        this.erroForm.set(m); this.toast.erro(m);
      },
    });
  }

  abrirRecebimento() { this.receberForm.reset({ lote: '', validade: '', observacao: '' }); this.recebendo.set(true); }

  confirmarRecebimento() {
    const c = this.compra(); if (!c) return;
    const v = this.receberForm.getRawValue();
    this.ocupado.set(true);
    this.api.receber(c.id, v.lote, v.validade, v.observacao).subscribe({
      next: r => {
        this.ocupado.set(false); this.recebendo.set(false);
        this.compra.set(r.compra); this.recebimento.set(r);
        this.estoque.atualizarAlertas(); // o badge do menu pode mudar
        this.toast.sucesso(`Recebimento registrado: ${r.compra.itens.length} ${r.compra.itens.length === 1 ? 'item entrou' : 'itens entraram'} no estoque.`);
      },
      error: e => { this.ocupado.set(false); this.toast.erro(e.error?.message ?? 'Não foi possível registrar o recebimento.'); },
    });
  }

  abrirCancelamento() { this.motivo.reset(''); this.cancelando.set(true); }

  confirmarCancelamento() {
    const c = this.compra(); if (!c) return;
    if (this.motivo.invalid) { this.motivo.markAsTouched(); return; }
    this.ocupado.set(true);
    this.api.cancelar(c.id, this.motivo.value).subscribe({
      next: r => { this.ocupado.set(false); this.cancelando.set(false); this.carregarCompra(r); this.toast.sucesso(`Pedido de compra #${r.id} cancelado.`); },
      error: e => { this.ocupado.set(false); this.toast.erro(e.error?.message ?? 'Não foi possível cancelar.'); },
    });
  }
}
