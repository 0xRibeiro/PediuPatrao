# PediuPatrão 🍕

Projeto da atividade prática M1 de Engenharia de Software: evolução de um sistema de pizzaria para controlar usuários, pedidos e auditoria.

## Tecnologias

- Java 21
- Spring Boot e Spring Security
- MongoDB
- Thymeleaf
- Maven

## Como executar

1. Tenha Java 21 instalado e acesso a um banco MongoDB (local ou Atlas).
2. Na pasta do projeto, crie um arquivo `.env` a partir do `.env.example` e preencha as variáveis:

   ```properties
   MONGODB_URL=mongodb+srv://... 
   MONGODB_BANCO=pediupatrao
   SPRING_SECURITY_USER=usuario
   SPRING_SECURITY_PASSWORD=senha
   ```

   Use uma URI real no lugar do exemplo e **não publique o `.env`**. As variáveis `SPRING_SECURITY_*` são configurações de ambiente; os perfis da aplicação são persistidos no MongoDB.
3. Execute:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Acesse `http://localhost:8080/login`.

## Perfis

- **ADMIN:** administra usuários, atribui perfis, consulta pedidos e auditorias.
- **GERENTE:** gerencia pedidos, aplica descontos, cancela pedidos, consulta clientes e auditorias.
- **ATENDENTE:** cadastra e atualiza clientes, registra pedidos e acompanha seus estados.

Descontos e cancelamentos são exclusivos do GERENTE, inclusive em comparação ao ADMIN.

## Funcionalidades implementadas

- Autenticação e autorização pelo Spring Security.
- Cadastro de pedidos com itens, quantidades e preços consultados no MongoDB; total calculado no servidor.
- Transições obrigatórias: `RECEBIDO` → `EM_PREPARACAO` → `PRONTO` → `SAIU_PARA_ENTREGA` ou `RETIRADO` → `FINALIZADO`.
- Registro de usuário/data de entrada e de saída dos pedidos.
- Desconto percentual de até 20%, aplicado somente pelo GERENTE antes da saída.
- Cancelamento pelo GERENTE, com justificativa e impedimento de alterações posteriores.
- Auditoria persistida de criação e alterações de pedidos e clientes, com dados anteriores/novos, usuário e horário.
- Consulta às auditorias restrita a ADMIN e GERENTE.
- Proteção CSRF em requisições que alteram dados.

## Rotas principais

| Método | Rota | Uso |
|---|---|---|
| GET | `/api/clientes` | Consultar clientes |
| POST | `/api/clientes` | Cadastrar cliente (ATENDENTE) |
| PUT | `/api/clientes/{id}` | Atualizar cliente (ATENDENTE) |
| GET | `/api/produtos` | Consultar produtos |
| GET | `/api/pedidos` | Consultar pedidos |
| POST | `/api/pedidos` | Criar pedido (GERENTE/ATENDENTE) |
| PUT | `/api/pedidos/{id}/status?status=PRONTO` | Avançar status (conforme regras) |
| PUT | `/api/pedidos/{id}/desconto?percentual=10` | Desconto (GERENTE) |
| PUT | `/api/pedidos/{id}/cancelar?justificativa=Motivo` | Cancelar (GERENTE) |
| GET | `/api/auditorias` | Consultar auditoria (ADMIN/GERENTE) |

Para requisições POST/PUT/DELETE feitas fora dos formulários Thymeleaf, envie o token CSRF obtido na sessão autenticada. Requisições sem token válido podem receber HTTP 403.

## Verificações realizadas

Foram testados os perfis ADMIN, GERENTE e ATENDENTE; bloqueios HTTP 403; criação de pedidos, cálculo do valor, transições de status, descontos válidos e inválidos, cancelamento, auditoria de pedidos e alteração de clientes, além da exigência do token CSRF nas APIs.
