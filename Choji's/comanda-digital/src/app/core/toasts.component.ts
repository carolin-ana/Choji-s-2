import { Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

// Fica uma vez só no app.html e mostra os avisos do ToastService no canto da tela
@Component({
  selector: 'app-toasts',
  standalone: true,
  template: `
  <div class="toasts" aria-live="polite">
    @for (t of toast.lista(); track t.id) {
      <div class="toast" [class.toast-erro]="t.tipo === 'erro'">
        <span>{{ t.texto }}</span>
        <button type="button" (click)="toast.fechar(t.id)" aria-label="Fechar aviso">×</button>
      </div>
    }
  </div>`,
})
export class ToastsComponent {
  toast = inject(ToastService);
}
