import { Component, ElementRef, OnDestroy, afterRenderEffect, input, viewChild } from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

// Chart.js registra os tipos de gráfico, escalas e plugins uma vez só
Chart.register(...registerables);

// Componente pequeno que desenha um gráfico do Chart.js num <canvas>.
// Uso: <app-grafico-chart [config]="configuracao" [altura]="280" descricao="..." />
// Sempre que a configuração muda (outro período, por exemplo), o gráfico é recriado.
@Component({
  selector: 'app-grafico-chart',
  standalone: true,
  template: `<div class="grafico-canvas" [style.height.px]="altura()"><canvas #canvas role="img" [attr.aria-label]="descricao()"></canvas></div>`,
})
export class GraficoChartComponent implements OnDestroy {
  config = input.required<ChartConfiguration>();
  altura = input(280);
  descricao = input('');
  private canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private grafico?: Chart;

  constructor() {
    afterRenderEffect(() => {
      const cfg = this.config();
      this.grafico?.destroy();
      this.grafico = new Chart(this.canvas().nativeElement, cfg);
    });
  }

  ngOnDestroy() { this.grafico?.destroy(); }
}
