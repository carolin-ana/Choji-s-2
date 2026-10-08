-- Bloco 5 (Pedidos e cozinha)
-- RF-019 pede filtro por "canal". Todo pedido feito pelo site entra como SITE.
ALTER TABLE pedido ADD COLUMN canal VARCHAR(20) NOT NULL DEFAULT 'SITE';

-- Usuários internos de exemplo, para testar os perfis (senha de todos: senha123, mesmo hash do admin)
INSERT IGNORE INTO usuario (nome, email, senha_hash, perfil) VALUES
('Gerente Exemplo', 'gerente@email.com', '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'GERENTE'),
('Cozinheiro Exemplo', 'cozinheiro@email.com', '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'COZINHEIRO');
