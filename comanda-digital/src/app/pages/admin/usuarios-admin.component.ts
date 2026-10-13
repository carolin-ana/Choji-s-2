import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, merge, of, startWith, switchMap, tap } from 'rxjs';
import { AdminUsuarioService } from '../../core/admin-usuario.service';
import { ToastService } from '../../core/toast.service';
import { Pagina, Perfil, ROTULOS_PERFIL, ROTULOS_STATUS_REGISTRO, StatusRegistro, UsuarioInterno } from '../../core/models';

// RF-041: o ADMIN cria, edita, desativa e reativa gerentes e cozinheiros.
@Component({
  selector: 'app-usuarios-admin',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './usuarios-admin.component.html',
})
export class UsuariosAdminComponent {
  private api = inject(AdminUsuarioService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  rotulosPerfil = ROTULOS_PERFIL;
  rotulosStatus = ROTULOS_STATUS_REGISTRO;
  perfisFiltro: Perfil[] = ['ADMIN', 'GERENTE', 'COZINHEIRO'];
  perfisEditaveis: Perfil[] = ['GERENTE', 'COZINHEIRO']; // contas ADMIN não são criadas nem alteradas por aqui

  filtros = this.fb.nonNullable.group({ busca: '', perfil: '' as Perfil | '', status: '' as StatusRegistro | '' });
  pagina = signal<Pagina<UsuarioInterno> | null>(null);
  numeroPagina = signal(0);
  carregando = signal(true);
  erroLista = signal(false);

  // Janela de criar/editar. editando = null → criando um novo.
  formAberto = signal(false);
  editando = signal<UsuarioInterno | null>(null);
  salvando = signal(false);
  erroForm = signal('');
  form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    telefone: ['', Validators.maxLength(30)],
    perfil: ['COZINHEIRO' as Perfil, Validators.required],
    senha: [''],
  });

  // Janela de confirmação para desativar
  desativando = signal<UsuarioInterno | null>(null);
  ocupado = signal<number | null>(null);

  private recarregar$ = new Subject<void>();

  constructor() {
    merge(
      this.recarregar$.pipe(startWith(undefined)),
      // espera a pessoa parar de digitar na busca antes de consultar o back
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
  }

  irPara(n: number) { this.numeroPagina.set(n); this.recarregar$.next(); }
  limpar() { this.filtros.reset(); }
  temFiltro() { const f = this.filtros.getRawValue(); return !!(f.busca || f.perfil || f.status); }
  gerenciavel(u: UsuarioInterno) { return u.perfil !== 'ADMIN'; }

  abrirNovo() {
    this.editando.set(null);
    this.form.reset({ nome: '', email: '', telefone: '', perfil: 'COZINHEIRO', senha: '' });
    // na criação a senha é obrigatória
    this.form.controls.senha.setValidators([Validators.required, Validators.minLength(6), Validators.maxLength(72)]);
    this.form.controls.senha.updateValueAndValidity();
    this.erroForm.set('');
    this.formAberto.set(true);
  }

  abrirEdicao(u: UsuarioInterno) {
    this.editando.set(u);
    this.form.reset({ nome: u.nome, email: u.email, telefone: u.telefone ?? '', perfil: u.perfil, senha: '' });
    // na edição a senha é opcional: vazia = mantém a atual
    this.form.controls.senha.setValidators([Validators.minLength(6), Validators.maxLength(72)]);
    this.form.controls.senha.updateValueAndValidity();
    this.erroForm.set('');
    this.formAberto.set(true);
  }

  fecharForm() { this.formAberto.set(false); }

  invalido(campo: keyof typeof this.form.controls) {
    const c = this.form.controls[campo];
    return c.invalid && (c.touched || c.dirty);
  }

  salvar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.salvando.set(true); this.erroForm.set('');
    const dados = this.form.getRawValue();
    const atual = this.editando();
    const req = atual ? this.api.atualizar(atual.id, dados) : this.api.criar(dados);
    req.subscribe({
      next: u => {
        this.salvando.set(false); this.formAberto.set(false);
        this.toast.sucesso(atual ? `${u.nome} foi atualizado.` : `${u.nome} foi cadastrado.`);
        this.recarregar$.next();
      },
      error: e => {
        this.salvando.set(false);
        // mostra o motivo dentro da janela (ex.: e-mail repetido) e também num aviso
        const msg = e.status === 409 ? 'Este e-mail já está cadastrado.' : (e.error?.message ?? 'Não foi possível salvar.');
        this.erroForm.set(msg);
        this.toast.erro(msg);
      },
    });
  }

  confirmarDesativacao() {
    const u = this.desativando();
    if (!u) return;
    this.ocupado.set(u.id);
    this.api.desativar(u.id).subscribe({
      next: r => { this.ocupado.set(null); this.desativando.set(null); this.toast.sucesso(`${r.nome} foi desativado.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível desativar.'); },
    });
  }

  reativar(u: UsuarioInterno) {
    this.ocupado.set(u.id);
    this.api.reativar(u.id).subscribe({
      next: r => { this.ocupado.set(null); this.toast.sucesso(`${r.nome} foi reativado.`); this.recarregar$.next(); },
      error: e => { this.ocupado.set(null); this.toast.erro(e.error?.message ?? 'Não foi possível reativar.'); },
    });
  }
}
