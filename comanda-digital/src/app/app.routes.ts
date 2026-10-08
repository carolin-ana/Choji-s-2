import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';
import { CardapioComponent } from './pages/cardapio/cardapio.component';
import { AuthComponent } from './pages/auth/auth.component';
import { CheckoutComponent } from './pages/checkout/checkout.component';
import { AcompanharComponent } from './pages/acompanhar/acompanhar.component';
import { PedidosComponent } from './pages/pedidos/pedidos.component';
import { AdminLayoutComponent } from './pages/admin/admin-layout.component';
import { PedidosAdminComponent } from './pages/admin/pedidos-admin.component';

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
    ],
  },
  { path: '**', redirectTo: '' },
];
