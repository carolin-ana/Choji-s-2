-- Bloco 7: fornecedores, catálogo, cotação, pedidos de compra e histórico de preços (SRS seções 3.4 e 6)
CREATE TABLE fornecedor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, razao_social VARCHAR(150) NOT NULL, cnpj VARCHAR(14) NOT NULL UNIQUE,
  telefone VARCHAR(30), email VARCHAR(160), categorias VARCHAR(255), status VARCHAR(10) NOT NULL DEFAULT 'ATIVO',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);
-- RF-022: preço por unidade padrão do ingrediente (por g, ml, un...). Um fornecedor só tem uma linha por ingrediente.
CREATE TABLE fornecedor_produto (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, fornecedor_id BIGINT NOT NULL, ingrediente_id BIGINT NOT NULL,
  preco DECIMAL(10,4) NOT NULL, unidade_venda VARCHAR(60), status VARCHAR(10) NOT NULL DEFAULT 'ATIVO',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_fornecedor_ingrediente UNIQUE (fornecedor_id, ingrediente_id),
  CONSTRAINT fk_fp_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor(id),
  CONSTRAINT fk_fp_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id));
CREATE TABLE pedido_compra (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, fornecedor_id BIGINT NOT NULL, status VARCHAR(10) NOT NULL DEFAULT 'RASCUNHO',
  valor_total DECIMAL(12,2) NOT NULL DEFAULT 0, observacoes VARCHAR(500), motivo_cancelamento VARCHAR(255),
  usuario_id BIGINT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, enviado_em DATETIME NULL, recebido_em DATETIME NULL,
  CONSTRAINT fk_pc_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor(id),
  CONSTRAINT fk_pc_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id));
CREATE TABLE pedido_compra_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, pedido_compra_id BIGINT NOT NULL, ingrediente_id BIGINT NOT NULL,
  quantidade DECIMAL(12,3) NOT NULL, preco_unitario DECIMAL(10,4) NOT NULL, subtotal DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_pci_compra FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra(id),
  CONSTRAINT fk_pci_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id));
-- RF-026: cada preço cobrado (origem CATALOGO = cadastro/mudança no catálogo; COMPRA = recebimento de pedido)
CREATE TABLE preco_historico (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, ingrediente_id BIGINT NOT NULL, fornecedor_id BIGINT NOT NULL,
  preco DECIMAL(10,4) NOT NULL, origem VARCHAR(10) NOT NULL, pedido_compra_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ph_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente(id),
  CONSTRAINT fk_ph_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor(id),
  CONSTRAINT fk_ph_compra FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra(id));
-- A coluna já existia desde a V6; agora que a tabela pedido_compra existe, ganha a chave estrangeira
ALTER TABLE estoque_movimentacao ADD CONSTRAINT fk_mov_pedido_compra FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra(id);

-- Seed: 3 fornecedores fictícios com CNPJ válido (o terceiro no formato alfanumérico novo da Receita)
INSERT INTO fornecedor (cnpj, razao_social, telefone, email, categorias) VALUES
('47218336000199', 'Hokkai Distribuidora de Alimentos Orientais Ltda', '(11) 3208-4410', 'vendas@hokkai.example.com', 'Mercearia oriental, massas, temperos'),
('58392017000111', 'Mar Azul Pescados e Carnes Ltda', '(11) 4002-7788', 'pedidos@marazul.example.com', 'Pescados, carnes'),
('4K7RM2Q9000109', 'Sakura Hortifruti e Laticínios Ltda', '(11) 2944-1350', 'contato@sakura.example.com', 'Hortifruti, ovos, laticínios, doces');

-- Seed: catálogo de cada fornecedor (preço por g, ml ou un). Vários ingredientes têm mais de um fornecedor para a cotação.
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0290, 'Fardo de 5 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-001';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0580, 'Pote de 1 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-006';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0145, 'Saco de 5 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-008';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.2600, 'Peça a vácuo (~1 kg)', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-009';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0300, 'Pacote de 1 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-012';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0480, 'Bisnaga de 500 g', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-016';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.3800, 'Pacote de 500 g', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-017';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.8500, 'Lata de 100 g', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-020';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0280, 'Galão de 1,8 L', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-026';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0150, 'Caixa de 1 L', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-027';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.1150, 'Caixa de 2 kg (congelado)', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-014';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.2400, 'Peça a vácuo (~1 kg)', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-009';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0430, 'Peça (~3 kg)', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-011';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0820, 'Peça (~2 kg)', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-003';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0275, 'Caixa de 5 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-013';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.4100, 'Pacote de 250 g', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-017';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.8500, 'Bandeja com 30', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-004';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0280, 'Maço de 200 g', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-005';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0075, 'Saco de 10 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-010';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0200, 'Lata de 2 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-007';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0420, 'Pacote de 1 kg (congelado)', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-018';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0058, 'Caixa com 12 L', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-021';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0480, 'Pote de 1 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-022';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.9200, 'Pacote de 100 g', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-020';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.1250, 'Peça (~1 kg)', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-014';
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda, updated_at) SELECT f.id, i.id, 0.0155, 'Saco de 5 kg', DATE_SUB(NOW(), INTERVAL 5 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-008';

-- Seed: histórico de preços (RF-026). Pontos antigos de alguns ingredientes, para o gráfico de evolução...
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.2200, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 120 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-009';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.2300, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 75 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-009';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.2450, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 40 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-009';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.2500, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 100 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-009';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.2550, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 50 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-009';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.1050, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 110 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-014';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.1100, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 60 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-014';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.1300, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 90 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-014';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.1280, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 45 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-014';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.8000, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 120 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-020';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.8300, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 70 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-020';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.9500, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 80 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '4K7RM2Q9000109' AND i.sku = 'ING-020';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.3600, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 90 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '47218336000199' AND i.sku = 'ING-017';
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT i.id, f.id, 0.4300, 'CATALOGO', DATE_SUB(NOW(), INTERVAL 60 DAY) FROM fornecedor f, ingrediente i WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-017';
-- ...e o preço atual de todo item do catálogo
INSERT INTO preco_historico (ingrediente_id, fornecedor_id, preco, origem, created_at) SELECT ingrediente_id, fornecedor_id, preco, 'CATALOGO', updated_at FROM fornecedor_produto;

-- Seed: um pedido de compra já ENVIADO (3 kg de polvo, que está abaixo do mínimo), pronto para testar o recebimento
INSERT INTO pedido_compra (fornecedor_id, status, valor_total, observacoes, usuario_id, created_at, updated_at, enviado_em)
SELECT f.id, 'ENVIADO', 345.00, 'Reposição do polvo (estoque abaixo do mínimo)', (SELECT u.id FROM usuario u WHERE u.email = 'gerente@email.com'),
       DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM fornecedor f WHERE f.cnpj = '58392017000111';
INSERT INTO pedido_compra_item (pedido_compra_id, ingrediente_id, quantidade, preco_unitario, subtotal)
SELECT c.id, i.id, 3000, 0.1150, 345.00 FROM pedido_compra c JOIN fornecedor f ON f.id = c.fornecedor_id, ingrediente i
WHERE f.cnpj = '58392017000111' AND i.sku = 'ING-014';
