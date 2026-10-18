import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';
import { CardapioComponent } from './pages/cardapio/cardapio.component';
import { AuthComponent } from './pages/auth/auth.component';
import { CheckoutComponent } from './pages/checkout/checkout.component';
import { AcompanharComponent } from './pages/acompanhar/acompanhar.component';
import { PedidosComponent } from './pages/pedidos/pedidos.component';
import { AdminLayoutComponent } from './pages/admin/admin-layout.component';
import { PedidosAdminComponent } from './pages/admin/pedidos-admin.component';
import { UsuariosAdminComponent } from './pages/admin/usuarios-admin.component';
import { PratosAdminComponent } from './pages/admin/pratos-admin.component';
import { PratoFormComponent } from './pages/admin/prato-form.component';
import { CategoriasAdminComponent } from './pages/admin/categorias-admin.component';
import { IngredientesAdminComponent } from './pages/admin/ingredientes-admin.component';
import { EstoqueAdminComponent } from './pages/admin/estoque-admin.component';
import { MovimentacoesAdminComponent } from './pages/admin/movimentacoes-admin.component';

export const routes: Routes = [
  { path: '', component: CardapioComponent },
  { path: 'login', component: AuthComponent, data: { modo: 'login' } },
  { path: 'cadastro', component: AuthComponent, data: { modo: 'cadastro' } },
  // Telas do cliente: precisa estar logado E ser CLIENTE
  { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard, roleGuard('CLIENTE')] },
  { path: 'pedido/:id', component: AcompanharComponent, canActivate: [authGuard, roleGuard('CLIENTE')] },
  { path: 'pedidos', component: PedidosComponent, canActivate: [authGuard, roleGuard('CLIENTE')] },
  // Painel: a moldura (menu) é o AdminLayoutComponent e cada tela entra como rota filha.
  // As próximas telas (cardápio, estoque, fornecedores...) entram em "children", cada uma com o seu roleGuard.
  {
    path: 'admin', component: AdminLayoutComponent,
    canActivate: [authGuard, roleGuard('ADMIN', 'GERENTE', 'COZINHEIRO')],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'pedidos' },
      { path: 'pedidos', component: PedidosAdminComponent },
      // RF-041: só o ADMIN gerencia usuários (o roleGuard da rota filha restringe mais que o da moldura)
      { path: 'usuarios', component: UsuariosAdminComponent, canActivate: [roleGuard('ADMIN')] },
      // Blocos 3 e 4: cardápio, ficha técnica e ingredientes (ADMIN e GERENTE; o cozinheiro não entra)
      { path: 'pratos', component: PratosAdminComponent, canActivate: [roleGuard('ADMIN', 'GERENTE')] },
      { path: 'pratos/novo', component: PratoFormComponent, canActivate: [roleGuard('ADMIN', 'GERENTE')] },
      { path: 'pratos/:id', component: PratoFormComponent, canActivate: [roleGuard('ADMIN', 'GERENTE')] },
      { path: 'categorias', component: CategoriasAdminComponent, canActivate: [roleGuard('ADMIN', 'GERENTE')] },
      { path: 'ingredientes', component: IngredientesAdminComponent, canActivate: [roleGuard('ADMIN', 'GERENTE')] },
      // Blocos 6 e 8: estoque (ADMIN e GERENTE)
      { path: 'estoque', component: EstoqueAdminComponent, canActivate: [roleGuard('ADMIN', 'GERENTE')] },
      { path: 'estoque/movimentacoes', component: MovimentacoesAdminComponent, canActivate: [roleGuard('ADMIN', 'GERENTE')] },
    ],
  },
  { path: '**', redirectTo: '' },
];
