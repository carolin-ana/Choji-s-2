# Comanda Digital · Choji's Kitchen

Sistema de pedidos para uma Dark Kitchen (cozinha que só vende por delivery), desenvolvido como Trabalho Final da disciplina de **Desenvolvimento Full-Stack** (UNASP SP, prof. Thiago Silva, 2026/2), seguindo o SRS *Comanda Digital v3.2*.

> Autor(a): **Ana Carolina Souza Vieira** · Turma **GTADSI43A**
> Autor(a): **Dennys de Oliveira** · Turma **GTADSI43A**

## Sistema no ar

| O quê | Endereço |
|---|---|
| **Site (front-end)** | https://comanda-digital-silk.vercel.app |
| **API (back-end)** | https://comanda-api-ktex.onrender.com |
| **Swagger (documentação da API)** | https://comanda-api-ktex.onrender.com/swagger-ui.html |
| **Repositório** | https://github.com/carolin-ana/Choji-s-2 |

> **Primeiro acesso pode demorar:** o back-end está no plano gratuito do Render, que desliga o servidor após 15 minutos sem uso. A primeira chamada depois disso leva de 1 a 3 minutos para "acordar" o sistema. Se o cardápio aparecer vazio, aguarde um pouco e atualize a página.

### Usuários para teste

| Perfil | E-mail | Senha | O que acessa |
|---|---|---|---|
| ADMIN | `admin@email.com` | `senha123` | Painel completo |
| GERENTE | `gerente@email.com` | `senha123` | Painel: pedidos, cardápio, estoque, fornecedores, cotação e compras |
| COZINHEIRO | `cozinheiro@email.com` | `senha123` | Painel: só iniciar preparo e marcar como pronto |
| CLIENTE | crie em **Entrar → Criar conta** | | Cardápio, carrinho, checkout, acompanhamento e histórico |

A equipe (admin, gerente e cozinheiro) entra pela mesma tela de login do site e é levada direto para o painel em `/admin`.

## Tecnologias

| Camada | Tecnologia | Onde está publicado |
|---|---|---|
| Front-end | Angular 22 (componentes standalone, signals, Reactive Forms, HttpClient com interceptor JWT, guards de rota) | Vercel |
| Back-end | Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security + JWT, Bean Validation, SpringDoc (Swagger), Flyway | Render (Docker) |
| Banco de dados | MySQL 8 | Aiven (plano gratuito) |

## Funcionalidades (blocos do SRS)

| # | Bloco | Situação |
|---|---|---|
| 1 | Cardápio público e carrinho (RF-001 a 003) | ✅ Pronto |
| 2 | Jornada do pedido do cliente (RF-004 a 008) | ✅ Pronto |
| 3 | CRUD de cardápio (RF-009, 010, 014) | ✅ Pronto |
| 4 | Ficha técnica, custo e food cost (RF-011 a 013) | ✅ Pronto |
| 5 | Pedidos e cozinha (RF-015, 016, 019, 020) | ✅ Pronto |
| 6 | Integração pedido × estoque (RF-017, 018) | ✅ Pronto |
| 7 | Fornecedores e compras (RF-021 a 026) | ✅ Pronto |
| 8 | Controle de estoque (RF-027 a 033) | ✅ Pronto |
| 9 | Dashboard (RF-034 a 037) | 🚧 Em desenvolvimento |
| 10 | Autenticação e RBAC (RF-038 a 043) | ✅ Pronto |

### Regras já implementadas

- Senhas com BCrypt, nunca devolvidas pela API; JWT com validade de 8 horas e o perfil no token.
- Rotas protegidas com `@PreAuthorize` no back e `authGuard`/`roleGuard` no front (cliente não acessa o painel; equipe não acessa o checkout).
- E-mail único no cadastro (409 se repetido).
- O preço do pedido é sempre lido do banco, nunca do front.
- O status do pedido só avança uma etapa por vez: RECEBIDO → CONFIRMADO → EM_PREPARO → PRONTO → SAIU_ENTREGA → FINALIZADO.
- Cancelamento só por GERENTE ou ADMIN, com motivo obrigatório; cada mudança de status fica registrada com data, hora e responsável.
- Prato só fica ATIVO com ficha técnica (RN01); food cost acima de 35% gera alerta sem bloquear (RN02); fator de correção mínimo 1,0 (RN08). O custo é calculado só no back: `SUM(quantidade × fator de correção × custo unitário) / rendimento`.
- Estoque por movimentações (entrada, saída e estorno): o saldo é sempre calculado. Ao confirmar um pedido, os ingredientes são baixados pela ficha técnica na mesma transação (`@Transactional`); sem estoque, a API responde 422 dizendo o que falta (RN03). Cancelar estorna o que foi baixado.
- Fornecedor com CNPJ validado pelo algoritmo dos dígitos verificadores, inclusive o CNPJ alfanumérico (RN07). Cada fornecedor tem um catálogo com preço por unidade do ingrediente; a cotação compara os fornecedores do mais barato para o mais caro. Pedido de compra RASCUNHO → ENVIADO → RECEBIDO: o recebimento dá entrada no estoque e atualiza o custo unitário do ingrediente (RN05), o que recalcula o food cost dos pratos. Todo preço fica no histórico, com gráfico de evolução.
- Listagens do painel com paginação (`Pageable`) e filtros por status, canal e período.
- Erros tratados em um `@RestControllerAdvice` (respostas padronizadas, sem stack trace para o cliente).
- Segredos (senha do banco, chave JWT) apenas em variáveis de ambiente.

## Estrutura do repositório

```
comanda-api/        Back-end Spring Boot
  src/main/java/com/comanda/
    controller/     Endpoints REST (só recebem e devolvem)
    service/        Regras de negócio
    repository/     Interfaces Spring Data JPA
    model/          Entidades (@Entity)
    dto/            Objetos de entrada e saída (records) — nenhuma entidade é exposta
    config/         Segurança, CORS e Swagger
    security/       Geração e leitura do JWT
    exception/      Tratamento global de erros
  src/main/resources/db/migration/   Scripts Flyway (V1, V2...), incluindo o seed
  Dockerfile        Usado pelo Render para montar e rodar o back
comanda-digital/    Front-end Angular
  src/app/core/     Serviços, modelos, interceptor e guards
  src/app/pages/    Telas (cardápio, login, checkout, pedidos, painel admin)
  src/environments/ URL da API em desenvolvimento e em produção
  vercel.json       Configuração da Vercel
```

## Banco de dados

As tabelas são criadas pelo **Flyway** ao iniciar o back (`comanda-api/src/main/resources/db/migration`). O seed cria o usuário admin, os usuários de exemplo, as categorias e os pratos do cardápio. O Hibernate não altera o banco (`ddl-auto=none`).

O diagrama entidade-relacionamento (DER) está em `docs/der.png` *(a adicionar)*.

## Como rodar localmente

**Pré-requisitos:** Java 17 (JDK), Node.js 22+, Angular CLI (`npm install -g @angular/cli`) e MySQL 8 rodando na porta 3306.

### Back-end

No PowerShell, na pasta `comanda-api`:

```powershell
$env:DB_PASSWORD="senha-do-seu-mysql"
$env:JWT_SECRET="uma-chave-qualquer-com-pelo-menos-32-caracteres"
.\mvnw spring-boot:run
```

O banco `comanda` é criado automaticamente. A API fica em `http://localhost:8080` e o Swagger em `http://localhost:8080/swagger-ui.html`.

### Front-end

Na pasta `comanda-digital`:

```powershell
npm install
ng serve
```

O site fica em `http://localhost:4200` e usa a API local (`src/environments/environment.ts`).

### Variáveis de ambiente do back-end

| Variável | Para que serve | Valor padrão (local) |
|---|---|---|
| `DB_URL` | Endereço JDBC do MySQL | `jdbc:mysql://localhost:3306/comanda?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false` |
| `DB_USER` | Usuário do banco | `root` |
| `DB_PASSWORD` | Senha do banco | *(obrigatória)* |
| `JWT_SECRET` | Chave de assinatura do JWT (mínimo 32 caracteres) | *(obrigatória)* |
| `CORS_ORIGINS` | Endereços do front autorizados, separados por vírgula | `http://localhost:4200` |
| `PORT` | Porta HTTP | `8080` |

## Como foi feito o deploy

1. **Banco:** serviço MySQL gratuito na **Aiven**, com conexão TLS (`sslMode=REQUIRED`).
2. **Back-end:** Web Service gratuito no **Render**, construído a partir do `comanda-api/Dockerfile` (build com Maven e execução com Java 17, memória limitada para caber no plano gratuito e fuso horário de São Paulo). As variáveis `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` e `CORS_ORIGINS` ficam configuradas no painel do Render, fora do código.
3. **Front-end:** projeto na **Vercel** com a pasta `comanda-digital` como raiz. O build de produção troca o `environment.ts` pelo `environment.prod.ts`, que aponta para a API do Render.

A cada `git push` na branch `main`, o Render e a Vercel publicam a nova versão automaticamente.
