INSERT INTO usuario (nome, email, senha_hash, perfil) VALUES ('Admin', 'admin@email.com', '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'ADMIN');
INSERT INTO categoria (nome, ordem) VALUES ('Lamens',1), ('Donburi',2), ('Entradas',3), ('Bebidas',4), ('Sobremesas',5);
INSERT INTO prato (categoria_id, nome, descricao, foto_url, preco_venda, tempo_preparo_min) VALUES
((SELECT id FROM categoria WHERE nome='Lamens'), 'Tonkotsu Densetsu', 'Caldo de ossos suínos cozido por 18h, chashu braseado e ovo ajitsuke.', 'imagens/tonkotsu.png', 72.00, 20),
((SELECT id FROM categoria WHERE nome='Lamens'), 'Missô Akai Especial', 'Caldo intenso de missô vermelho, porco agridoce e milho amanteigado.', 'imagens/missoAkai.png', 70.00, 18),
((SELECT id FROM categoria WHERE nome='Donburi'), 'Gyudon Premium', 'Wagyu marinado em dashi e mirin sobre arroz japonês com gema curada.', 'imagens/gyudon.png', 89.00, 15),
((SELECT id FROM categoria WHERE nome='Donburi'), 'Katsudon Crocante', 'Lombinho empanado no panko, cebola caramelizada e ovo suave.', 'imagens/katsudon.png', 64.00, 15),
((SELECT id FROM categoria WHERE nome='Donburi'), 'Oyakodon da Casa', 'Frango, ovo e cebolinha cozidos em caldo suave de tsuyu.', 'imagens/oyakodon.png', 58.00, 12),
((SELECT id FROM categoria WHERE nome='Entradas'), 'Takoyaki Especial', 'Bolinhos de polvo com maionese japonesa e katsuobushi.', 'imagens/takoyaki.png', 28.00, 10),
((SELECT id FROM categoria WHERE nome='Entradas'), 'Edamame com Flor de Sal', 'Vagens de soja no vapor com flor de sal e limão siciliano.', 'imagens/edamame.png', 18.00, 5),
((SELECT id FROM categoria WHERE nome='Bebidas'), 'Matcha Latte Gelado', 'Matcha cerimonial com leite integral e mel, servido com gelo.', 'imagens/matchaLatte.png', 19.00, 5),
((SELECT id FROM categoria WHERE nome='Sobremesas'), 'Mochi de Morango', 'Arroz glutinoso recheado com sorvete de morango.', 'imagens/mochi.png', 22.00, 5),
((SELECT id FROM categoria WHERE nome='Sobremesas'), 'Dorayaki com Azuki', 'Pancakes fofos recheados com pasta de feijão azuki.', 'imagens/dorayaki.png', 20.00, 8);
