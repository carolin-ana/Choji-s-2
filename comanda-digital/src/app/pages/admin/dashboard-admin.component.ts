import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, forkJoin, of, startWith, switchMap } from 'rxjs';
import type { ChartConfiguration, TooltipItem } from 'chart.js';
import { DashboardService } from '../../core/dashboard.service';
import { EstoqueService } from '../../core/estoque.service';
import { EstoqueSaldo, ROTULOS_FAIXA, ROTULOS_UNIDADE, Resumo, TopPrato, Vendas } from '../../core/models';
import { GraficoChartComponent } from './grafico-chart.component';

// Cor da série (paleta validada para daltonismo; uma série só, então o título nomeia o gráfico e não há legenda)
const AZUL = '#2a78d6';
const GRADE = '#ececec';
const TEXTO = '#555';

const moeda = (v: number, casas = 2) =>
  v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: casas, maximumFractionDigits: casas });
// AAAA-MM-DD no fuso do navegador (o mesmo do restaurante)
const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const diasAtras = (n: number) => { const d = new Date(); d.setDate(d.getDate() - n); return iso(d); };

type Atalho = '7' | '30' | 'mes' | '';

// Bloco 9: RF-034 (KPIs do dia), RF-035 (top 5 em barras), RF-036 (alertas de estoque) e RF-037 (faturamento diário em linha + tabela)
@Component({
  selector: 'app-dashboard-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe, GraficoChartComponent],
  templateUrl: './dashboard-admin.component.html',
})
export class DashboardAdminComponent {
  private api = inject(DashboardService);
  private fb = inject(FormBuilder);
  estoque = inject(EstoqueService);
  rotulosFaixa = ROTULOS_FAIXA;
  rotulosUnidade = ROTULOS_UNIDADE;

  // ---- RF-034: hoje ----
  resumo = signal<Resumo | null>(null);
  erroResumo = signal(false);
  variacao = computed(() => {
    const r = this.resumo();
    if (!r || r.faturamentoDiaAnterior === 0) return null;
    return (r.faturamento - r.faturamentoDiaAnterior) / r.faturamentoDiaAnterior * 100;
  });

  // ---- RF-036 ----
  alertas = signal<EstoqueSaldo[]>([]);
  totalAlertas = signal(0);

  // ---- RF-035 e RF-037: período ----
  atalho = signal<Atalho>('30');
  periodo = this.fb.nonNullable.group({ dataInicio: diasAtras(29), dataFim: iso(new Date()) });
  top = signal<TopPrato[]>([]);
  vendas = signal<Vendas | null>(null);
  carregandoPeriodo = signal(true);
  erroPeriodo = signal('');
  verDias = signal(false);
  private recarregar$ = new Subject<void>();

  constructor() {
    this.carregarHoje();
    this.recarregar$.pipe(
      startWith(undefined),
      switchMap(() => {
        const { dataInicio, dataFim } = this.periodo.getRawValue();
        if (!dataInicio || !dataFim) return of(null);
        if (dataInicio > dataFim) { this.erroPeriodo.set('A data inicial não pode ser depois da data final.'); return of(null); }
        this.carregandoPeriodo.set(true); this.erroPeriodo.set('');
        return forkJoin({ top: this.api.topPratos(dataInicio, dataFim), vendas: this.api.vendas(dataInicio, dataFim) }).pipe(
          catchError(e => { this.erroPeriodo.set(e.error?.message ?? 'Não foi possível carregar o período. Confira se o servidor está no ar.'); return of(null); }));
      }),
      takeUntilDestroyed(),
    ).subscribe(r => {
      this.carregandoPeriodo.set(false);
      if (r) { this.top.set(r.top); this.vendas.set(r.vendas); }
    });
  }

  carregarHoje() {
    this.erroResumo.set(false);
    this.api.resumo().subscribe({ next: r => this.resumo.set(r), error: () => this.erroResumo.set(true) });
    this.estoque.listarAlertas(0, 5).subscribe({
      next: p => { this.alertas.set(p.content); this.totalAlertas.set(p.totalElements); this.estoque.alertas.set(p.totalElements); },
      error: () => {},
    });
  }

  usarAtalho(a: Atalho) {
    const hoje = new Date();
    const inicio = a === '7' ? diasAtras(6) : a === '30' ? diasAtras(29) : iso(new Date(hoje.getFullYear(), hoje.getMonth(), 1));
    this.atalho.set(a);
    this.periodo.setValue({ dataInicio: inicio, dataFim: iso(hoje) });
    this.recarregar$.next();
  }
  aplicarDatas() { this.atalho.set(''); this.recarregar$.next(); }
  atualizarTudo() { this.carregarHoje(); this.recarregar$.next(); }

  // ---- Configuração dos gráficos (Chart.js) ----

  // RF-035: barras horizontais, do mais vendido para o menos vendido
  graficoTop = computed<ChartConfiguration>(() => {
    const t = this.top();
    return {
      type: 'bar',
      data: {
        labels: t.map(p => p.prato),
        datasets: [{ label: 'Unidades vendidas', data: t.map(p => p.quantidade), backgroundColor: AZUL,
                     borderRadius: 4, borderSkipped: false, maxBarThickness: 26 }],
      },
      options: {
        indexAxis: 'y', responsive: true, maintainAspectRatio: false, animation: false,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: (c: TooltipItem<'bar'>) => {
            const p = t[c.dataIndex];
            return `${p.quantidade} vendidos · ${moeda(p.faturamento)} (${p.percentualQuantidade.toLocaleString('pt-BR')}% das unidades)`;
          } } },
        },
        scales: {
          x: { beginAtZero: true, ticks: { precision: 0, color: TEXTO, callback: (v: string | number) => Number(v).toLocaleString('pt-BR') }, grid: { color: GRADE } },
          y: { ticks: { color: '#222' }, grid: { display: false } },
        },
      },
    } as ChartConfiguration;
  });

  // RF-037: linha com o faturamento de cada dia
  graficoVendas = computed<ChartConfiguration>(() => {
    const dias = this.vendas()?.dias ?? [];
    return {
      type: 'line',
      data: {
        labels: dias.map(d => d.data.slice(8, 10) + '/' + d.data.slice(5, 7)),
        datasets: [{ label: 'Faturamento', data: dias.map(d => d.faturamento), borderColor: AZUL, backgroundColor: AZUL,
                     borderWidth: 2, pointRadius: dias.length > 45 ? 0 : 4, pointHoverRadius: 6, tension: 0, fill: false }],
      },
      options: {
        responsive: true, maintainAspectRatio: false, animation: false,
        interaction: { mode: 'index', intersect: false }, // passa o mouse em qualquer altura e vê o dia inteiro
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: (c: TooltipItem<'line'>) => {
            const d = dias[c.dataIndex];
            return `${moeda(d.faturamento)} · ${d.pedidos} ${d.pedidos === 1 ? 'pedido' : 'pedidos'}`;
          } } },
        },
        scales: {
          y: { beginAtZero: true, ticks: { color: TEXTO, callback: (v: string | number) => moeda(Number(v), 0) }, grid: { color: GRADE } },
          x: { ticks: { color: TEXTO, maxRotation: 0, autoSkip: true, maxTicksLimit: 10 }, grid: { display: false } },
        },
      },
    } as ChartConfiguration;
  });

  diasDecrescentes() { return [...(this.vendas()?.dias ?? [])].reverse(); }
  melhorDia() {
    const d = this.vendas()?.dias ?? [];
    return d.length ? d.reduce((a, b) => (b.faturamento > a.faturamento ? b : a)) : null;
  }
}
