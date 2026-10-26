import { Component, computed, input, signal } from '@angular/core';
import { PrecoHistorico } from '../../core/models';

interface Ponto { t: number; preco: number; origem: string; }
interface Serie { fornecedorId: number; nome: string; cor: string; pontos: Ponto[]; caminho: string; }

// Paleta categórica validada (cores fixas por fornecedor, na ordem do id: a cor segue o fornecedor, não a posição)
const CORES = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#e87ba4', '#008300', '#6250d6', '#e34948'];
const L = 960, A = 300, M = { esq: 84, dir: 16, cima: 16, baixo: 34 };

// RF-026: evolução do preço de um ingrediente em cada fornecedor.
// Desenhado em SVG puro (sem biblioteca): linha em "degraus", porque o preço vale até a próxima mudança.
@Component({
  selector: 'app-grafico-precos',
  standalone: true,
  template: `
  @if (series().length) {
    <div class="grafico">
      <ul class="legenda" aria-label="Fornecedores">
        @for (s of series(); track s.fornecedorId) {
          <li><span class="chave" [style.background]="s.cor"></span>{{ s.nome }}</li>
        }
      </ul>
      <div class="grafico-rolagem"><div class="grafico-area">
        <svg [attr.viewBox]="'0 0 ' + L + ' ' + A" role="img" [attr.aria-label]="'Evolução do preço por ' + unidade() + ' em cada fornecedor'"
             (pointermove)="mover($event)" (pointerleave)="cursor.set(null)">
          @for (y of eixoY(); track y.v) {
            <line class="grade" [attr.x1]="M.esq" [attr.x2]="L - M.dir" [attr.y1]="y.y" [attr.y2]="y.y" />
            <text class="eixo" [attr.x]="M.esq - 8" [attr.y]="y.y + 4" text-anchor="end">{{ y.rotulo }}</text>
          }
          @for (x of eixoX(); track x.t; let primeiro = $first, ultimo = $last) {
            <text class="eixo" [attr.x]="x.x" [attr.y]="A - 10" [attr.text-anchor]="primeiro ? 'start' : (ultimo ? 'end' : 'middle')">{{ x.rotulo }}</text>
          }
          @for (s of series(); track s.fornecedorId) {
            <path [attr.d]="s.caminho" fill="none" [attr.stroke]="s.cor" stroke-width="2" stroke-linejoin="round" />
            @for (p of s.pontos; track $index) {
              <circle [attr.cx]="px(p.t)" [attr.cy]="py(p.preco)" r="4.5" [attr.fill]="s.cor" stroke="#fff" stroke-width="2" />
            }
          }
          @if (cursor(); as c) {
            <line class="mira" [attr.x1]="c.x" [attr.x2]="c.x" [attr.y1]="M.cima" [attr.y2]="A - M.baixo" />
          }
        </svg>
        @if (cursor(); as c) {
          <div class="dica-grafico" [style.left.%]="c.x / L * 100" [class.a-esquerda]="c.x > L * 0.6">
            <span class="data">{{ c.data }}</span>
            @for (v of c.valores; track v.nome) {
              <span class="linha-dica"><span class="traco" [style.background]="v.cor"></span><strong>{{ v.preco }}</strong> {{ v.nome }}</span>
            }
          </div>
        }
      </div></div>
    </div>
  } @else {
    <p class="vazio">Ainda não há preços registrados para este ingrediente.</p>
  }`,
})
export class GraficoPrecosComponent {
  historico = input.required<PrecoHistorico[]>();
  unidade = input('unidade');
  readonly L = L; readonly A = A; readonly M = M;
  cursor = signal<{ x: number; data: string; valores: { nome: string; cor: string; preco: string }[] } | null>(null);

  private agora = Date.now();
  private faixaT = computed(() => {
    const ts = this.historico().map(h => new Date(h.data).getTime());
    const min = Math.min(...ts), max = Math.max(this.agora, ...ts);
    return { min, max: max === min ? min + 86_400_000 : max };
  });
  private faixaP = computed(() => {
    const ps = this.historico().map(h => h.preco);
    let min = Math.min(...ps), max = Math.max(...ps);
    const folga = (max - min) * 0.15 || max * 0.1 || 1;
    min = Math.max(0, min - folga); max = max + folga;
    return { min, max };
  });

  px(t: number) { const f = this.faixaT(); return M.esq + (t - f.min) / (f.max - f.min) * (L - M.esq - M.dir); }
  py(p: number) { const f = this.faixaP(); return A - M.baixo - (p - f.min) / (f.max - f.min) * (A - M.cima - M.baixo); }

  series = computed<Serie[]>(() => {
    const porFornecedor = new Map<number, { nome: string; pontos: Ponto[] }>();
    for (const h of this.historico()) {
      const s = porFornecedor.get(h.fornecedorId) ?? { nome: h.fornecedor, pontos: [] };
      s.pontos.push({ t: new Date(h.data).getTime(), preco: h.preco, origem: h.origem });
      porFornecedor.set(h.fornecedorId, s);
    }
    return [...porFornecedor.entries()].sort((a, b) => a[0] - b[0]).map(([id, s], i) => {
      const pts = s.pontos.sort((a, b) => a.t - b.t);
      // degrau: horizontal até a próxima data, depois sobe/desce; o último preço segue até hoje
      let d = `M${this.px(pts[0].t)},${this.py(pts[0].preco)}`;
      for (let k = 1; k < pts.length; k++) d += ` H${this.px(pts[k].t)} V${this.py(pts[k].preco)}`;
      d += ` H${this.px(this.faixaT().max)}`;
      return { fornecedorId: id, nome: s.nome, cor: CORES[i % CORES.length], pontos: pts, caminho: d };
    });
  });

  eixoY = computed(() => {
    const f = this.faixaP();
    return [0, 1, 2, 3].map(k => { const v = f.min + (f.max - f.min) * k / 3; return { v, y: this.py(v), rotulo: this.moeda(v) }; });
  });
  eixoX = computed(() => {
    const f = this.faixaT();
    return [0, 1, 2, 3, 4].map(k => { const t = f.min + (f.max - f.min) * k / 4; return { t, x: this.px(t), rotulo: this.dataCurta(t) }; });
  });

  // Mira: acha a data sob o ponteiro e mostra o preço em vigor de cada fornecedor naquele dia
  mover(e: PointerEvent) {
    const svg = e.currentTarget as SVGSVGElement;
    const r = svg.getBoundingClientRect();
    const x = Math.min(L - M.dir, Math.max(M.esq, (e.clientX - r.left) / r.width * L));
    const f = this.faixaT();
    const t = f.min + (x - M.esq) / (L - M.esq - M.dir) * (f.max - f.min);
    const valores = this.series().map(s => {
      const vigente = [...s.pontos].reverse().find(p => p.t <= t);
      return { nome: s.nome, cor: s.cor, preco: vigente ? this.moeda(vigente.preco) : '—' };
    });
    this.cursor.set({ x, data: new Date(t).toLocaleDateString('pt-BR'), valores });
  }

  private moeda(v: number) { return 'R$ ' + v.toFixed(4).replace('.', ','); }
  private dataCurta(t: number) { return new Date(t).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }); }
}
