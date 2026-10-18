import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, forkJoin, of, switchMap } from 'rxjs';
import { AdminCardapioService } from '../../core/admin-cardapio.service';
import { AdminIngredienteService } from '../../core/admin-ingrediente.service';
import { ToastService } from '../../core/toast.service';
import {
  Categoria, Ficha, FichaForm, Ingrediente, PratoAdmin, ROTULOS_FAIXA, ROTULOS_STATUS_REGISTRO, ROTULOS_UNIDADE, StatusRegistro,
} from '../../core/models';

type LinhaFicha = FormGroup<{
  ingredienteId: FormControl<number | null>;
  quantidade: FormControl<number | null>;
  fatorCorrecao: FormControl<number | null>;
}>;

// RF-010 (dados do prato), RF-011 (ficha técnica com FormArray), RF-012/013 (custo e food cost) e RF-014 (modo de preparo).
// Todo o cálculo de custo é feito pelo back: a tela só manda a ficha e mostra o resultado.
@Component({
  selector: 'app-prato-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DecimalPipe],
  templateUrl: './prato-form.component.html',
})
export class PratoFormComponent {
  private api = inject(AdminCardapioService);
  private ingApi = inject(AdminIngredienteService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  rotulosUnidade = ROTULOS_UNIDADE;
  rotulosFaixa = ROTULOS_FAIXA;
  rotulosStatus = ROTULOS_STATUS_REGISTRO;
  statusOpcoes: StatusRegistro[] = ['ATIVO', 'PAUSADO', 'INATIVO'];

  pratoId = signal<number | null>(null);
  prato = signal<PratoAdmin | null>(null);
  categorias = signal<Categoria[]>([]);
  ingredientes = signal<Ingrediente[]>([]);
  carregando = signal(true);
  erroCarga = signal('');

  salvandoDados = signal(false);
  erroDados = signal('');
  salvandoFicha = signal(false);
  erroFicha = signal('');
  temFichaSalva = signal(false);
  // Resultado do último cálculo feito pelo back (ficha salva ou simulação da ficha em edição)
  calculo = signal<Ficha | null>(null);
  calculando = signal(false);
  erroCalculo = signal('');

  dados = this.fb.group({
    nome: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    categoriaId: this.fb.control<number | null>(null, Validators.required),
    precoVenda: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    tempoPreparoMin: this.fb.control<number | null>(15, [Validators.required, Validators.min(1), Validators.max(600)]),
    fotoUrl: this.fb.nonNullable.control('', Validators.maxLength(255)),
    descricao: this.fb.nonNullable.control('', Validators.maxLength(500)),
    status: this.fb.nonNullable.control<StatusRegistro>('INATIVO'),
  });

  ficha = this.fb.group({
    rendimento: this.fb.control<number | null>(1, [Validators.required, Validators.min(1), Validators.max(1000)]),
    modoPreparo: this.fb.nonNullable.control('', Validators.maxLength(5000)),
    itens: this.fb.array<LinhaFicha>([]),
  });
  get itens(): FormArray<LinhaFicha> { return this.ficha.controls.itens; }

  constructor() {
    const param = inject(ActivatedRoute).snapshot.paramMap.get('id');
    const id = param ? Number(param) : null;
    this.pratoId.set(id);

    forkJoin({
      categorias: this.api.todasCategorias(),
      ingredientes: this.ingApi.todos(),
      prato: id ? this.api.buscarPrato(id) : of(null),
      ficha: id ? this.api.buscarFicha(id) : of(null),
    }).subscribe({
      next: r => {
        this.categorias.set(r.categorias);
        this.ingredientes.set(r.ingredientes);
        if (r.prato) this.carregarPrato(r.prato);
        if (r.ficha) this.carregarFicha(r.ficha);
        else this.itens.push(this.novaLinha()); // prato sem ficha: já começa com uma linha em branco
        this.carregando.set(false);
      },
      error: e => {
        this.carregando.set(false);
        this.erroCarga.set(e.status === 404 ? 'Prato não encontrado.' : 'Não foi possível carregar os dados. Confira se o servidor está no ar.');
      },
    });

    // RF-012 "em tempo real": a cada mudança na ficha (com uma pausa de meio segundo), o back recalcula sem salvar
    this.ficha.valueChanges.pipe(
      debounceTime(500),
      switchMap(() => {
        const pid = this.pratoId();
        if (pid === null || this.ficha.invalid) return of(null);
        this.calculando.set(true);
        return this.api.simularFicha(pid, this.fichaRequest()).pipe(catchError(e => {
          this.erroCalculo.set(e.error?.message ?? 'Não foi possível calcular o custo.');
          return of(null);
        }));
      }),
      takeUntilDestroyed(),
    ).subscribe(r => {
      this.calculando.set(false);
      if (r) { this.calculo.set(r); this.erroCalculo.set(''); }
    });
  }

  private carregarPrato(p: PratoAdmin) {
    this.prato.set(p);
    this.temFichaSalva.set(p.temFicha);
    this.dados.reset({
      nome: p.nome, categoriaId: p.categoriaId, precoVenda: p.precoVenda, tempoPreparoMin: p.tempoPreparoMin,
      fotoUrl: p.fotoUrl ?? '', descricao: p.descricao ?? '', status: p.status,
    });
  }

  private carregarFicha(f: Ficha) {
    this.itens.clear({ emitEvent: false });
    f.itens.forEach(i => this.itens.push(this.novaLinha(i.ingredienteId, i.quantidade, i.fatorCorrecao), { emitEvent: false }));
    this.ficha.patchValue({ rendimento: f.rendimento, modoPreparo: f.modoPreparo ?? '' }, { emitEvent: false });
    this.calculo.set(f);
    this.temFichaSalva.set(true);
  }

  novaLinha(ingredienteId: number | null = null, quantidade: number | null = null, fatorCorrecao: number | null = 1): LinhaFicha {
    return this.fb.group({
      ingredienteId: this.fb.control<number | null>(ingredienteId, Validators.required),
      quantidade: this.fb.control<number | null>(quantidade, [Validators.required, Validators.min(0.001)]),
      fatorCorrecao: this.fb.control<number | null>(fatorCorrecao, [Validators.required, Validators.min(1)]), // RN08
    });
  }
  adicionarLinha() { this.itens.push(this.novaLinha()); }
  removerLinha(i: number) { if (this.itens.length > 1) this.itens.removeAt(i); }

  // Ingredientes que aparecem no select de uma linha: os ativos + o que já está escolhido nela (mesmo inativo)
  opcoes(linha: LinhaFicha): Ingrediente[] {
    const atual = linha.controls.ingredienteId.value;
    return this.ingredientes().filter(i => i.status === 'ATIVO' || i.id === atual);
  }
  usadoEmOutraLinha(id: number, linha: LinhaFicha) {
    return this.itens.controls.some(l => l !== linha && l.controls.ingredienteId.value === id);
  }
  ingredienteDa(linha: LinhaFicha): Ingrediente | undefined {
    return this.ingredientes().find(i => i.id === linha.controls.ingredienteId.value);
  }
  // Custo da linha calculado pelo back (só aparece quando o último cálculo corresponde às linhas da tela)
  custoDaLinha(i: number): number | null {
    const c = this.calculo();
    const linha = this.itens.at(i);
    if (!c || c.itens.length !== this.itens.length || c.itens[i]?.ingredienteId !== linha.controls.ingredienteId.value) return null;
    return c.itens[i].custo;
  }
  linhaInvalida(linha: LinhaFicha, campo: 'ingredienteId' | 'quantidade' | 'fatorCorrecao') {
    const c = linha.controls[campo]; return c.invalid && (c.touched || c.dirty);
  }
  dadoInvalido(campo: keyof typeof this.dados.controls) {
    const c = this.dados.controls[campo]; return c.invalid && (c.touched || c.dirty);
  }

  private fichaRequest(): FichaForm {
    const v = this.ficha.getRawValue();
    return { rendimento: v.rendimento, modoPreparo: v.modoPreparo, itens: v.itens };
  }

  salvarDados() {
    if (this.dados.invalid) { this.dados.markAllAsTouched(); return; }
    this.salvandoDados.set(true); this.erroDados.set('');
    const id = this.pratoId();
    const valores = this.dados.getRawValue();
    const req = id ? this.api.atualizarPrato(id, valores) : this.api.criarPrato(valores);
    req.subscribe({
      next: p => {
        this.salvandoDados.set(false);
        if (!id) {
          this.toast.sucesso(`${p.nome} criado. Agora monte a ficha técnica para poder ativá-lo.`);
          this.router.navigate(['/admin/pratos', p.id]);
          return;
        }
        this.carregarPrato(p);
        this.toast.sucesso('Dados do prato salvos.');
        this.ficha.updateValueAndValidity(); // o preço pode ter mudado: recalcula o food cost
      },
      error: e => {
        this.salvandoDados.set(false);
        const m = e.error?.message ?? 'Não foi possível salvar o prato.';
        this.erroDados.set(m); this.toast.erro(m);
      },
    });
  }

  salvarFicha() {
    const id = this.pratoId();
    if (id === null) return;
    if (this.ficha.invalid) { this.ficha.markAllAsTouched(); this.erroFicha.set('Confira os campos destacados.'); return; }
    this.salvandoFicha.set(true); this.erroFicha.set('');
    const req = this.temFichaSalva() ? this.api.atualizarFicha(id, this.fichaRequest()) : this.api.criarFicha(id, this.fichaRequest());
    req.subscribe({
      next: f => {
        this.salvandoFicha.set(false);
        this.calculo.set(f);
        this.temFichaSalva.set(true);
        this.prato.update(p => p && { ...p, temFicha: true, custo: f.custo });
        this.toast.sucesso('Ficha técnica salva.');
        if (f.custo.alerta) this.toast.aviso(f.custo.alerta); // RN02: avisa, mas salva
      },
      error: e => {
        this.salvandoFicha.set(false);
        const m = e.error?.message ?? 'Não foi possível salvar a ficha técnica.';
        this.erroFicha.set(m); this.toast.erro(m);
      },
    });
  }
}
