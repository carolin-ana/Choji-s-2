import { Injectable, signal } from '@angular/core';

export interface Toast { id: number; tipo: 'sucesso' | 'erro' | 'aviso'; texto: string; }

// RNF10: avisos rápidos de sucesso/erro que somem sozinhos. Qualquer tela pode chamar:
//   private toast = inject(ToastService);   this.toast.sucesso('Pedido confirmado');
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly lista = signal<Toast[]>([]);
  private proximoId = 1;

  sucesso(texto: string) { this.abrir('sucesso', texto); }
  erro(texto: string) { this.abrir('erro', texto); }
  aviso(texto: string) { this.abrir('aviso', texto); } // alerta que não é erro (ex.: food cost alto)
  fechar(id: number) { this.lista.update(l => l.filter(t => t.id !== id)); }

  private abrir(tipo: Toast['tipo'], texto: string) {
    const id = this.proximoId++;
    this.lista.update(l => [...l, { id, tipo, texto }]);
    setTimeout(() => this.fechar(id), tipo === 'sucesso' ? 3500 : 7000);
  }
}
