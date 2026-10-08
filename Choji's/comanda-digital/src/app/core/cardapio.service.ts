import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { Prato } from './models';

const p = (id: number, nome: string, categoria: string, precoVenda: number, foto: string, tempoPreparoMin: number, descricao: string): Prato =>
  ({ id, nome, categoria, precoVenda, fotoUrl: `imagens/${foto}`, tempoPreparoMin, descricao, status: 'ATIVO' });

// Mesmo formato que GET /api/cardapio vai devolver (SRS seção 7)
const MOCK: Prato[] = [
  p(1, 'Tonkotsu Densetsu', 'Lamens', 72, 'tonkotsu.png', 20, 'Caldo de ossos suínos cozido por 18h, chashu braseado e ovo ajitsuke.'),
  p(3, 'Missô Akai Especial', 'Lamens', 70, 'missoAkai.png', 18, 'Caldo intenso de missô vermelho, porco agridoce e milho amanteigado.'),
  p(5, 'Gyudon Premium', 'Donburi', 89, 'gyudon.png', 15, 'Wagyu marinado em dashi e mirin sobre arroz japonês com gema curada.'),
  p(6, 'Katsudon Crocante', 'Donburi', 64, 'katsudon.png', 15, 'Lombinho empanado no panko, cebola caramelizada e ovo suave.'),
  p(7, 'Oyakodon da Casa', 'Donburi', 58, 'oyakodon.png', 12, 'Frango, ovo e cebolinha cozidos em caldo suave de tsuyu.'),
  p(9, 'Takoyaki Especial', 'Entradas', 28, 'takoyaki.png', 10, 'Bolinhos de polvo com maionese japonesa e katsuobushi.'),
  p(10, 'Edamame com Flor de Sal', 'Entradas', 18, 'edamame.png', 5, 'Vagens de soja no vapor com flor de sal e limão siciliano.'),
  p(11, 'Matcha Latte Gelado', 'Bebidas', 19, 'matchaLatte.png', 5, 'Matcha cerimonial com leite integral e mel, servido com gelo.'),
  p(13, 'Mochi de Morango', 'Sobremesas', 22, 'mochi.png', 5, 'Arroz glutinoso recheado com sorvete de morango.'),
  p(14, 'Dorayaki com Azuki', 'Sobremesas', 20, 'dorayaki.png', 8, 'Pancakes fofos recheados com pasta de feijão azuki.'),
];

@Injectable({ providedIn: 'root' })
export class CardapioService {
  private http = inject(HttpClient);
  listar(categoria?: string): Observable<Prato[]> {
    if (environment.useMock) return of(categoria ? MOCK.filter(x => x.categoria === categoria) : MOCK);
    const params: Record<string, string> = categoria ? { categoria } : {};
    return this.http.get<Prato[]>(`${environment.apiUrl}/api/cardapio`, { params });
  }
}