import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { EQUIPE, Perfil } from './models';

// RF-043: se não está logado, vai pro login e volta pra página pedida depois (RF-005)
export const authGuard: CanActivateFn = (_rota, estado) =>
  inject(AuthService).logado() ||
  inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: estado.url } });

// RF-043: só entra quem tem um dos perfis. Quem não tem volta para a "casa" do seu perfil:
// equipe vai para o painel, cliente vai para o cardápio.
export const roleGuard = (...perfis: Perfil[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  return auth.temPerfil(perfis) || inject(Router).createUrlTree([auth.temPerfil(EQUIPE) ? '/admin' : '/']);
};
