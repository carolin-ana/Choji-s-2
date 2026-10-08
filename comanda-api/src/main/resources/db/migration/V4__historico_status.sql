-- RF-020: guarda a data/hora (e quem fez) de cada mudança de status, para a linha do tempo do pedido
CREATE TABLE pedido_status_historico (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, pedido_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL,
  usuario_id BIGINT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_historico_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id),
  CONSTRAINT fk_historico_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id));

-- Pedidos que já existiam: sabemos quando foram recebidos e quando mudaram pela última vez.
-- As etapas do meio não têm hora registrada (a tabela ainda não existia).
INSERT INTO pedido_status_historico (pedido_id, status, usuario_id, created_at)
  SELECT id, 'RECEBIDO', cliente_id, created_at FROM pedido;
INSERT INTO pedido_status_historico (pedido_id, status, created_at)
  SELECT id, status, updated_at FROM pedido WHERE status <> 'RECEBIDO';
