import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, merge, of, switchMap, tap, timer } from 'rxjs';
import { AdminPedidoService } from '../../core/admin-pedido.service';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';
import { CANAIS, CICLO, CanalPedido, HistoricoStatus, Pagina, PedidoAdmin, ROTULOS, ROTULOS_CANAL, StatusPedido, TODOS_STATUS } from '../../core/models';

interface Acao { status: StatusPedido; rotulo: string; soGerencia: boolean; }

// RF-016: o próximo passo de cada status. soGerencia = o cozinheiro não vê o botão
// (SRS 1.3: cozinheiro só faz CONFIRMADO > EM_PREPARO > PRONTO). O back confere de novo.
const PROXIMA: Partial<Record<StatusPedido, Acao>> = {
  RECEBIDO: { status: 'CONFIRMADO', rotulo: 'Confirmar pedido', soGerencia: true },
  CONFIRMADO: { status: 'EM_PREPARO', rotulo: 'Iniciar preparo', soGerencia: false },
  EM_PREPARO: { status: 'PRONTO', rotulo: 'Marcar como pronto', soGerencia: false },
  PRONTO: { status: 'SAIU_ENTREGA', rotulo: 'Saiu para entrega', soGerencia: true },
  SAIU_ENTREGA: { status: 'FINALIZADO', rotulo: 'Finalizar', soGerencia: true },
};
const INTERVALO_MS = 10_000;

@Component({
  selector: 'app-pedidos-admin',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  templateUrl: './pedidos-admin.component.html',
})
export class PedidosAdminComponent {
  private api = inject(AdminPedidoService);
  private auth = inject(AuthService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  rotulos = ROTULOS;
  rotulosCanal = ROTULOS_CANAL;
  todosStatus = TODOS_STATUS;
  canais = CANAIS;
  ciclo = CICLO;

  // RF-019: filtros (Reactive Forms). Vazio = "todos".
  filtros = this.fb.nonNullable.group({
    status: '' as StatusPedido | '', canal: '' as CanalPedido | '', dataInicio: '', dataFim: '',
  });
  pagina = signal<Pagina<PedidoAdmin> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);
  ocupado = signal<number | null>(null);          // id do pedido com uma ação em andamento
  detalhe = signal<PedidoAdmin | null>(null);     // RF-020
  cancelando = signal<PedidoAdmin | null>(null);  // pedido com a janela de cancelamento aberta
  motivo = this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]);
  gerencia = computed(() => this.auth.temPerfil(['ADMIN', 'GERENTE']));

  private recarregar$ = new Subject<void>();

  constructor() {
    // Busca a lista: ao abrir a tela, a cada 10 s (RF-015: pedido novo aparece sozinho),
    // quando um filtro muda e quando alguém pede (recarregar$).
    merge(
      timer(0, INTERVALO_MS),
      this.recarregar$,
      this.filtros.valueChanges.pipe(tap(() => this.numeroPagina.set(0))),
    ).pipe(
      switchMap(() => this.api.listar(this.filtros.getRawValue(), this.numeroPagina()).pipe(
        catchError(() => of(null)), // erro não pode parar a atualização automática
      )),
      takeUntilDestroyed(),
    ).subscribe(pg => {
      this.carregando.set(false);
      this.erroLista.set(!pg);
      if (!pg) return;
      // se a página atual deixou de existir (ex.: pedidos saíram do filtro), volta para a última
      if (pg.content.length === 0 && pg.page > 0) { this.irPara(Math.max(pg.totalPages - 1, 0)); return; }
      this.pagina.set(pg);
    });
  }

  irPara(n: number) { this.numeroPagina.set(n); this.recarregar$.next(); }
  limpar() { this.filtros.reset(); }
  temFiltro() { const f = this.filtros.getRawValue(); return !!(f.status || f.canal || f.dataInicio || f.dataFim); }

  proxima(p: PedidoAdmin): Acao | null {
    const a = PROXIMA[p.status];
    return a && (!a.soGerencia || this.gerencia()) ? a : null;
  }
  // Só gerente/admin cancela, e só enquanto o pedido não foi encerrado. O back confere de novo.
  podeCancelar(p: PedidoAdmin): boolean {
    return this.gerencia() && p.status !== 'FINALIZADO' && p.status !== 'CANCELADO';
  }
  indice(s: StatusPedido) { return CICLO.indexOf(s); }
  // RF-020: quando (e por quem) o pedido entrou em cada etapa. Vazio se a etapa ainda não aconteceu.
  registro(p: PedidoAdmin, s: StatusPedido): HistoricoStatus | undefined { return p.historico.find(h => h.status === s); }

  avancar(p: PedidoAdmin, a: Acao) {
    this.ocupado.set(p.id);
    this.api.mudarStatus(p.id, a.status).subscribe({
      next: novo => { this.aplicar(novo); this.toast.sucesso(`Pedido #${novo.id}: ${ROTULOS[novo.status]}.`); },
      error: e => this.falhou(e, 'Não foi possível mudar o status.'),
    });
  }

  abrirCancelamento(p: PedidoAdmin) { this.motivo.reset(); this.cancelando.set(p); }
  confirmarCancelamento() {
    const p = this.cancelando();
    if (!p) return;
    if (this.motivo.invalid || !this.motivo.value.trim()) { this.motivo.markAsTouched(); return; }
    this.ocupado.set(p.id);
    this.api.cancelar(p.id, this.motivo.value.trim()).subscribe({
      next: novo => { this.cancelando.set(null); this.aplicar(novo); this.toast.sucesso(`Pedido #${novo.id} cancelado.`); },
      error: e => this.falhou(e, 'Não foi possível cancelar o pedido.'),
    });
  }

  // RF-020: abre na hora com o que já está na lista e confirma com o back (GET /api/admin/pedidos/{id})
  abrirDetalhe(p: PedidoAdmin) {
    this.detalhe.set(p);
    this.api.buscar(p.id).subscribe({
      next: d => { if (this.detalhe()?.id === d.id) this.detalhe.set(d); },
      error: () => this.toast.erro('Não foi possível atualizar os detalhes do pedido.'),
    });
  }

  // Troca o pedido na lista pelo que o back devolveu e pede uma atualização (o filtro pode ter mudado o resultado)
  private aplicar(novo: PedidoAdmin) {
    this.ocupado.set(null);
    this.pagina.update(pg => pg && { ...pg, content: pg.content.map(x => (x.id === novo.id ? novo : x)) });
    if (this.detalhe()?.id === novo.id) this.detalhe.set(novo);
    this.recarregar$.next();
  }
  private falhou(e: { error?: { message?: string } }, padrao: string) {
    this.ocupado.set(null);
    this.toast.erro(e.error?.message ?? padrao); // a mensagem vem do @ControllerAdvice do back
    this.recarregar$.next();
  }
}
