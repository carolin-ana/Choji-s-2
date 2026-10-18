-- Blocos 3 e 4: ingredientes, ficha técnica e itens da ficha (SRS seção 6)
CREATE TABLE ingrediente (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(120) NOT NULL, sku VARCHAR(40) NOT NULL UNIQUE,
  unidade_padrao VARCHAR(5) NOT NULL, estoque_minimo DECIMAL(12,3) NOT NULL DEFAULT 0,
  custo_unitario DECIMAL(10,4) NOT NULL DEFAULT 0, status VARCHAR(10) NOT NULL DEFAULT 'ATIVO',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE ficha_tecnica (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, prato_id BIGINT NOT NULL UNIQUE, rendimento INT NOT NULL DEFAULT 1,
  modo_preparo TEXT, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ficha_prato FOREIGN KEY (prato_id) REFERENCES prato(id));
CREATE TABLE ficha_tecnica_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, ficha_tecnica_id BIGINT NOT NULL, ingrediente_id BIGINT NOT NULL,
  quantidade DECIMAL(12,3) NOT NULL, unidade VARCHAR(5) NOT NULL, fator_correcao DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  CONSTRAINT fk_fti_ficha FOREIGN KEY (ficha_tecnica_id) REFERENCES ficha_tecnica(id),
  CONSTRAINT fk_fti_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id));

-- Seed: ingredientes (custo por unidade padrão: por grama, por ml ou por unidade)
INSERT INTO ingrediente (sku, nome, unidade_padrao, estoque_minimo, custo_unitario) VALUES
('ING-001', 'Macarrão para lámen', 'G', 3000, 0.0300),
('ING-002', 'Caldo tonkotsu', 'ML', 5000, 0.0200),
('ING-003', 'Chashu (barriga suína)', 'G', 2000, 0.0800),
('ING-004', 'Ovo', 'UN', 60, 0.9000),
('ING-005', 'Cebolinha', 'G', 500, 0.0300),
('ING-006', 'Missô vermelho', 'G', 1000, 0.0600),
('ING-007', 'Milho', 'G', 1000, 0.0200),
('ING-008', 'Arroz japonês', 'G', 5000, 0.0150),
('ING-009', 'Carne wagyu', 'G', 1500, 0.2500),
('ING-010', 'Cebola', 'G', 2000, 0.0080),
('ING-011', 'Lombo suíno', 'G', 2000, 0.0450),
('ING-012', 'Farinha panko', 'G', 1000, 0.0300),
('ING-013', 'Peito de frango', 'G', 3000, 0.0280),
('ING-014', 'Polvo', 'G', 1000, 0.1200),
('ING-015', 'Farinha de trigo', 'G', 3000, 0.0060),
('ING-016', 'Maionese japonesa', 'G', 500, 0.0500),
('ING-017', 'Katsuobushi', 'G', 100, 0.4000),
('ING-018', 'Edamame', 'G', 2000, 0.0400),
('ING-019', 'Flor de sal', 'G', 200, 0.1500),
('ING-020', 'Matcha cerimonial', 'G', 100, 0.9000),
('ING-021', 'Leite integral', 'ML', 5000, 0.0060),
('ING-022', 'Mel', 'G', 500, 0.0500),
('ING-023', 'Massa de mochi', 'G', 1000, 0.0250),
('ING-024', 'Sorvete de morango', 'G', 1500, 0.0400),
('ING-025', 'Pasta de feijão azuki', 'G', 1000, 0.0350),
('ING-026', 'Shoyu', 'ML', 1000, 0.0300),
('ING-027', 'Dashi', 'ML', 3000, 0.0150),
('ING-028', 'Óleo de soja', 'ML', 3000, 0.0100);

-- Seed: ficha técnica dos 10 pratos do cardápio (o SRS pede pelo menos 5)
INSERT INTO ficha_tecnica (prato_id, rendimento, modo_preparo) VALUES
((SELECT id FROM prato WHERE nome = 'Tonkotsu Densetsu'), 1, '1. Aqueça o caldo tonkotsu até ferver.
2. Cozinhe o macarrão por 2 minutos e escorra.
3. Monte a tigela com caldo, macarrão, chashu fatiado e meio ovo ajitsuke.
4. Finalize com cebolinha.'),
((SELECT id FROM prato WHERE nome = 'Missô Akai Especial'), 1, '1. Dissolva o missô no dashi quente, sem ferver.
2. Cozinhe o macarrão por 2 minutos.
3. Monte com chashu, milho na manteiga e cebolinha.'),
((SELECT id FROM prato WHERE nome = 'Gyudon Premium'), 1, '1. Refogue a cebola em fatias no shoyu.
2. Junte o wagyu fatiado fino e cozinhe por 1 minuto.
3. Sirva sobre o arroz com a gema curada por cima.'),
((SELECT id FROM prato WHERE nome = 'Katsudon Crocante'), 1, '1. Empane o lombo no ovo e no panko.
2. Frite em óleo a 170 °C até dourar.
3. Cozinhe a cebola no shoyu, junte o katsu e o ovo batido.
4. Sirva sobre o arroz.'),
((SELECT id FROM prato WHERE nome = 'Oyakodon da Casa'), 1, '1. Cozinhe o frango em cubos no dashi com shoyu e cebola.
2. Adicione o ovo batido e tampe por 1 minuto.
3. Sirva sobre o arroz.'),
((SELECT id FROM prato WHERE nome = 'Takoyaki Especial'), 1, '1. Misture farinha, ovo e água até formar a massa.
2. Unte a forma, coloque a massa e um pedaço de polvo em cada cavidade.
3. Vire as bolinhas até dourar.
4. Finalize com maionese e katsuobushi.'),
((SELECT id FROM prato WHERE nome = 'Edamame com Flor de Sal'), 4, '1. Cozinhe as vagens no vapor por 5 minutos.
2. Tempere com flor de sal e limão.
3. Divida em 4 porções.'),
((SELECT id FROM prato WHERE nome = 'Matcha Latte Gelado'), 1, '1. Bata o matcha com 50 ml de água morna até espumar.
2. Coloque gelo no copo, o leite e o mel.
3. Despeje o matcha por cima.'),
((SELECT id FROM prato WHERE nome = 'Mochi de Morango'), 1, '1. Abra a massa de mochi em discos.
2. Recheie com uma bola de sorvete e feche.
3. Leve ao congelador por 30 minutos.'),
((SELECT id FROM prato WHERE nome = 'Dorayaki com Azuki'), 1, '1. Misture farinha, ovo e mel até ficar liso.
2. Doure pequenas panquecas em frigideira untada.
3. Recheie duas a duas com a pasta de azuki.');

-- Seed: itens das fichas (a unidade é a unidade padrão do ingrediente)
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 150, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-001' WHERE p.nome = 'Tonkotsu Densetsu';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 400, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-002' WHERE p.nome = 'Tonkotsu Densetsu';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 120, i.unidade_padrao, 1.10 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-003' WHERE p.nome = 'Tonkotsu Densetsu';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 1, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-004' WHERE p.nome = 'Tonkotsu Densetsu';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 10, i.unidade_padrao, 1.20 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-005' WHERE p.nome = 'Tonkotsu Densetsu';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 150, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-001' WHERE p.nome = 'Missô Akai Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 350, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-027' WHERE p.nome = 'Missô Akai Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 50, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-006' WHERE p.nome = 'Missô Akai Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 80, i.unidade_padrao, 1.10 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-003' WHERE p.nome = 'Missô Akai Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 40, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-007' WHERE p.nome = 'Missô Akai Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 10, i.unidade_padrao, 1.20 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-005' WHERE p.nome = 'Missô Akai Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 200, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-008' WHERE p.nome = 'Gyudon Premium';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 120, i.unidade_padrao, 1.15 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-009' WHERE p.nome = 'Gyudon Premium';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 60, i.unidade_padrao, 1.10 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-010' WHERE p.nome = 'Gyudon Premium';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 1, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-004' WHERE p.nome = 'Gyudon Premium';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 20, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-026' WHERE p.nome = 'Gyudon Premium';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 200, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-008' WHERE p.nome = 'Katsudon Crocante';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 150, i.unidade_padrao, 1.10 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-011' WHERE p.nome = 'Katsudon Crocante';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 30, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-012' WHERE p.nome = 'Katsudon Crocante';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 2, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-004' WHERE p.nome = 'Katsudon Crocante';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 50, i.unidade_padrao, 1.10 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-010' WHERE p.nome = 'Katsudon Crocante';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 100, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-028' WHERE p.nome = 'Katsudon Crocante';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 15, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-026' WHERE p.nome = 'Katsudon Crocante';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 200, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-008' WHERE p.nome = 'Oyakodon da Casa';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 150, i.unidade_padrao, 1.10 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-013' WHERE p.nome = 'Oyakodon da Casa';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 2, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-004' WHERE p.nome = 'Oyakodon da Casa';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 50, i.unidade_padrao, 1.10 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-010' WHERE p.nome = 'Oyakodon da Casa';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 100, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-027' WHERE p.nome = 'Oyakodon da Casa';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 15, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-026' WHERE p.nome = 'Oyakodon da Casa';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 40, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-014' WHERE p.nome = 'Takoyaki Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 60, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-015' WHERE p.nome = 'Takoyaki Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 15, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-016' WHERE p.nome = 'Takoyaki Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 3, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-017' WHERE p.nome = 'Takoyaki Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 1, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-004' WHERE p.nome = 'Takoyaki Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 30, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-028' WHERE p.nome = 'Takoyaki Especial';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 600, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-018' WHERE p.nome = 'Edamame com Flor de Sal';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 8, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-019' WHERE p.nome = 'Edamame com Flor de Sal';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 4, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-020' WHERE p.nome = 'Matcha Latte Gelado';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 250, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-021' WHERE p.nome = 'Matcha Latte Gelado';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 15, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-022' WHERE p.nome = 'Matcha Latte Gelado';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 80, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-023' WHERE p.nome = 'Mochi de Morango';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 60, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-024' WHERE p.nome = 'Mochi de Morango';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 60, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-015' WHERE p.nome = 'Dorayaki com Azuki';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 1, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-004' WHERE p.nome = 'Dorayaki com Azuki';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 10, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-022' WHERE p.nome = 'Dorayaki com Azuki';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 60, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-025' WHERE p.nome = 'Dorayaki com Azuki';
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) SELECT f.id, i.id, 10, i.unidade_padrao, 1.00 FROM ficha_tecnica f JOIN prato p ON p.id = f.prato_id JOIN ingrediente i ON i.sku = 'ING-028' WHERE p.nome = 'Dorayaki com Azuki';
