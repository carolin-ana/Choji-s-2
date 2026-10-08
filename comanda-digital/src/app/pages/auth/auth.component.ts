import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { LoginResponse } from '../../core/models';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
  <main class="pagina">
    <a routerLink="/">← Voltar ao cardápio</a>
    <h1>{{ cadastro ? 'Criar conta' : 'Entrar' }}</h1>
    <form class="form" [formGroup]="form" (ngSubmit)="enviar()">
      @if (cadastro) {
        <label>Nome <input formControlName="nome" autocomplete="name" /></label>
        <label>Telefone <input formControlName="telefone" autocomplete="tel" /></label>
        <label>Endereço de entrega <input formControlName="endereco" autocomplete="street-address" /></label>
      }
      <label>E-mail <input type="email" formControlName="email" autocomplete="email" /></label>
      <label>Senha <input type="password" formControlName="senha" autocomplete="current-password" /></label>
      @if (form.invalid && form.touched) { <p class="erro">Preencha todos os campos. A senha precisa ter ao menos 6 caracteres.</p> }
      @if (erro()) { <p class="erro">{{ erro() }}</p> }
      <button class="btn" [disabled]="carregando()">{{ carregando() ? 'Aguarde...' : (cadastro ? 'Criar conta' : 'Entrar') }}</button>
    </form>
    <p>
      @if (cadastro) { Já tem conta? <a routerLink="/login" queryParamsHandling="preserve">Entrar</a> }
      @else { Primeira vez aqui? <a routerLink="/cadastro" queryParamsHandling="preserve">Criar conta</a> }
    </p>
    @if (!cadastro) { <p class="dica">Equipe da cozinha: use este mesmo login para entrar no painel.</p> }
  </main>`,
})
export class AuthComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  cadastro = this.route.snapshot.data['modo'] === 'cadastro';
  carregando = signal(false);
  erro = signal('');
  form: FormGroup = this.cadastro
    ? this.fb.nonNullable.group({
        nome: ['', Validators.required], telefone: ['', Validators.required], endereco: ['', Validators.required],
        email: ['', [Validators.required, Validators.email]], senha: ['', [Validators.required, Validators.minLength(6)]] })
    : this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]], senha: ['', Validators.required] });

  enviar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.carregando.set(true); this.erro.set('');
    const v = this.form.getRawValue();
    const req = this.cadastro ? this.auth.registrar(v) : this.auth.login(v.email, v.senha);
    req.subscribe({
      next: r => this.router.navigateByUrl(this.destino(r)),
      error: e => {
        this.carregando.set(false);
        this.erro.set(e.status === 409 ? 'Este e-mail já está cadastrado.'
          : e.status === 401 ? 'E-mail ou senha incorretos.' : 'Não foi possível concluir. Tente novamente.');
      },
    });
  }

  // RF-005: o cliente volta para onde estava (ex.: checkout). A equipe vai para o painel.
  private destino(r: LoginResponse): string {
    const volta = this.route.snapshot.queryParamMap.get('returnUrl');
    if (r.perfil === 'CLIENTE') return volta ?? '/';
    return volta?.startsWith('/admin') ? volta : '/admin';
  }
}
