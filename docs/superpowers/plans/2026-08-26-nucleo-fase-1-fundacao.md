# Núcleo — Fase 1: Fundação — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar um backend Spring Boot que sobe, conecta no PostgreSQL real, executa migrations Flyway, responde no formato de envelope acordado, documenta-se sozinho e tem as fronteiras de arquitetura verificadas pelo CI.

**Architecture:** Monólito modular em Java 21 + Spring Boot. Esta fase não implementa nenhuma regra de negócio — ela constrói o chassi sobre o qual as fases 2, 3 e 4 assentam: projeto Maven, ambientes, migrations, tratamento global de erros, correlação de logs, OpenAPI e os três testes ArchUnit que sustentam as fronteiras de módulo.

**Tech Stack:** Java 21 (Temurin) · Spring Boot 3.5.6 · Spring Web · Spring Data JPA · Spring Validation · Spring Actuator · PostgreSQL 17 · Flyway · Maven Wrapper · springdoc-openapi 2.8.x · JUnit 5 · Testcontainers · ArchUnit 1.3.0 · Docker Compose · GitHub Actions

**Spec:** `docs/superpowers/specs/2026-08-26-nucleo-pagamento-acesso-design.md`

## Global Constraints

Estes valores valem para toda tarefa deste plano e dos seguintes. Copiados da spec.

- **Java 21.** Nenhum recurso de versão posterior.
- **Spring Boot 3.5.6.** Versão pinada no `pom.xml`; bump acontece em um lugar só.
- **PostgreSQL 17** (instância real: `17.6.1.155`, projeto Supabase `bjnplubfqoltaxfboodl`). Testcontainers usa `postgres:17-alpine`.
- **`spring.jpa.hibernate.ddl-auto=validate`** em todos os perfis. Nenhuma criação automática de tabela.
- **Toda alteração estrutural do banco passa por migration Flyway versionada.** Nenhuma migration destrutiva.
- **Prefixo de rota `/api/v1`.** Nenhum endpoint sem versão.
- **Envelope de sucesso:** `{ "success": true, "data": {} }`
- **Envelope de lista:** `{ "success": true, "data": [], "pagination": { "page", "size", "totalElements", "totalPages" } }`
- **Envelope de erro:** `{ "success": false, "error": { "code", "message", "status", "path", "traceId", "timestamp", "fieldErrors" } }`
- **`code` é contrato estável** em `SCREAMING_SNAKE_CASE`; `message` é texto em português e pode mudar.
- **Todo segredo vem de variável de ambiente.** Nenhum valor real commitado.
- **Timestamps em UTC**, tipo `timestamptz`. Identificadores em UUID.
- **Teto de paginação: `size` máximo 100.**
- **Stack trace nunca aparece na resposta no perfil `prod`.**
- **Pacote raiz:** `com.betobanco`. **Diretório do backend:** `backend/`.

## Pré-requisito de ambiente

Java 21 não está instalado na máquina. **Maven não é necessário** — o projeto usa o Maven Wrapper (`mvnw`), que baixa o Maven sozinho. Isso é tratado no Passo 1 da Tarefa 2.

Todos os comandos deste plano são para **Git Bash**. No PowerShell, troque `./mvnw` por `.\mvnw.cmd`.

## Estrutura de arquivos ao final desta fase

```
Beto_Banco/
├── .gitignore                                   (novo)
├── .github/workflows/backend.yml                (novo)
├── backend/
│   ├── mvnw · mvnw.cmd · .mvn/                  (wrapper Maven)
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .env.example
│   └── src/
│       ├── main/java/com/betobanco/
│       │   ├── BetoBancoApplication.java
│       │   ├── config/OpenApiConfig.java
│       │   └── shared/
│       │       ├── response/ApiResponse.java
│       │       ├── response/PageResponse.java
│       │       ├── response/PaginationMeta.java
│       │       ├── exception/ErrorCode.java
│       │       ├── exception/BusinessException.java
│       │       ├── exception/NotFoundException.java
│       │       ├── exception/GlobalExceptionHandler.java
│       │       ├── exception/ErrorPayload.java
│       │       ├── exception/FieldErrorItem.java
│       │       ├── pagination/PageRequestFactory.java
│       │       └── trace/TraceIdFilter.java
│       ├── main/resources/
│       │   ├── application.yml · application-dev.yml
│       │   ├── application-test.yml · application-prod.yml
│       │   ├── logback-spring.xml
│       │   └── db/migration/V1__baseline.sql
│       └── test/java/com/betobanco/
│           ├── ApplicationBootTest.java
│           ├── support/PostgresTestBase.java
│           ├── shared/response/ApiResponseTest.java
│           ├── shared/exception/GlobalExceptionHandlerTest.java
│           ├── shared/pagination/PageRequestFactoryTest.java
│           ├── shared/trace/TraceIdFilterTest.java
│           └── architecture/ModuleBoundariesTest.java
└── docker-compose.yml                           (novo, na raiz)
```

Cada arquivo tem uma responsabilidade única. `shared/` agrupa o que atravessa módulos; nada em `shared/` conhece regra de negócio.

---

### Task 1: Higiene do repositório

O repositório versiona 11.379 arquivos de `node_modules` e não possui `.gitignore`. Isso polui todo diff, torna o clone pesado e produz conflitos em arquivos gerados. Corrigir antes de adicionar código novo evita arrastar o problema.

**Files:**
- Create: `.gitignore`
- Modify: índice do Git (remoção de `frontend-react/node_modules` do rastreamento)

**Interfaces:**
- Consumes: nada
- Produces: nada em código. Estabelece que `node_modules`, `target/`, `dist/` e `.env` nunca são versionados.

- [ ] **Step 1: Criar o `.gitignore` na raiz do repositório**

```gitignore
# Node
node_modules/
dist/
.vite/
*.local

# Java / Maven
target/
!.mvn/wrapper/maven-wrapper.jar
*.class

# Ambiente
.env
.env.local
.env.*.local

# IDE
.idea/
.vscode/
*.iml

# Sistema operacional
.DS_Store
Thumbs.db
desktop.ini

# Saídas de build capturadas por engano
build_output.txt
build_output_clean.txt
tsc_output.txt
error.txt
```

- [ ] **Step 2: Confirmar quantos arquivos estão rastreados hoje**

```bash
git ls-files frontend-react/node_modules | wc -l
```

Esperado: `11379` (ou número próximo). Anote o valor.

- [ ] **Step 3: Remover do índice sem apagar do disco**

```bash
git rm -r --cached frontend-react/node_modules --quiet
git rm --cached frontend-react/dist -r --quiet 2>/dev/null || true
git rm --cached frontend-react/build_output.txt frontend-react/build_output_clean.txt frontend-react/tsc_output.txt frontend-react/error.txt --quiet 2>/dev/null || true
```

O `--cached` garante que os arquivos continuam no disco e o servidor Vite segue funcionando.

- [ ] **Step 4: Verificar que o rastreamento acabou e os arquivos permanecem**

```bash
git ls-files frontend-react/node_modules | wc -l
ls frontend-react/node_modules | head -3
```

Esperado: `0` na primeira linha, e a listagem de pacotes na segunda — provando que só o índice mudou.

- [ ] **Step 5: Commit**

```bash
git add .gitignore
git commit -m "chore: adiciona .gitignore e remove node_modules do rastreamento"
```

---

### Task 2: Esqueleto do backend que sobe

**Files:**
- Create: `backend/` inteiro, gerado pelo Spring Initializr (inclui `mvnw`, `pom.xml`, `src/`)
- Create: `backend/src/test/java/com/betobanco/ApplicationBootTest.java`
- Modify: `backend/src/main/java/com/betobanco/BetoBancoApplication.java` (renomear a classe gerada)

**Interfaces:**
- Consumes: nada
- Produces: `com.betobanco.BetoBancoApplication` — classe `@SpringBootApplication` que todas as tarefas seguintes usam como raiz de componente scan.

- [ ] **Step 1: Instalar o Java 21**

```bash
winget install --id EclipseAdoptium.Temurin.21.JDK -e --accept-source-agreements --accept-package-agreements
```

Feche e reabra o terminal. Verifique:

```bash
java -version
```

Esperado: uma linha contendo `21.` — por exemplo `openjdk version "21.0.x"`.

- [ ] **Step 2: Gerar o projeto com o Maven Wrapper embutido**

Rode a partir da raiz do repositório:

```bash
curl -sG https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d bootVersion=3.5.6 -d javaVersion=21 \
  -d groupId=com.betobanco -d artifactId=backend -d name=beto-banco-backend \
  -d packageName=com.betobanco \
  -d dependencies=web,security,data-jpa,validation,postgresql,flyway,actuator,lombok,testcontainers \
  -o backend.zip
unzip -q backend.zip -d backend-tmp && mv backend-tmp/backend . && rm -rf backend.zip backend-tmp
ls backend
```

Esperado: `mvnw`, `mvnw.cmd`, `pom.xml`, `src`, `.mvn`. O wrapper elimina a necessidade de instalar Maven.

- [ ] **Step 3: Renomear a classe principal**

O Initializr gera `BetoBancoBackendApplication`. Renomeie o arquivo e a classe para `BetoBancoApplication`, conforme a estrutura da spec:

```bash
mv backend/src/main/java/com/betobanco/BetoBancoBackendApplication.java \
   backend/src/main/java/com/betobanco/BetoBancoApplication.java
```

Conteúdo final de `backend/src/main/java/com/betobanco/BetoBancoApplication.java`:

```java
package com.betobanco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BetoBancoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BetoBancoApplication.class, args);
    }
}
```

Apague também o teste gerado `BetoBancoBackendApplicationTests.java`:

```bash
rm -f backend/src/test/java/com/betobanco/BetoBancoBackendApplicationTests.java
```

- [ ] **Step 4: Escrever o teste que falha**

Crie `backend/src/test/java/com/betobanco/ApplicationBootTest.java`:

```java
package com.betobanco;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationBootTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextoSobeComABeanPrincipal() {
        assertThat(context.getBean(BetoBancoApplication.class)).isNotNull();
    }
}
```

- [ ] **Step 5: Rodar o teste e confirmar que falha**

```bash
cd backend && ./mvnw -q test -Dtest=ApplicationBootTest
```

Esperado: **FALHA**. O motivo é que `spring-boot-starter-data-jpa` está no classpath sem nenhum `DataSource` configurado, e o Boot aborta com `Failed to configure a DataSource: 'url' attribute is not specified`. Essa falha é a prova de que o teste está realmente subindo o contexto.

- [ ] **Step 6: Fazer passar adiando o DataSource**

Substitua `backend/src/main/resources/application.properties` por `backend/src/main/resources/application.yml`:

```bash
rm backend/src/main/resources/application.properties
```

Crie `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: beto-banco
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

server:
  servlet:
    context-path: /api/v1

management:
  endpoints:
    web:
      exposure:
        include: health, info
  endpoint:
    health:
      probes:
        enabled: true
```

A exclusão do `DataSource` é **temporária** e sai na Tarefa 4, quando o banco entra de verdade. Sem ela, esta tarefa não teria um deliverable testável por si.

- [ ] **Step 7: Rodar o teste e confirmar que passa**

```bash
cd backend && ./mvnw -q test -Dtest=ApplicationBootTest
```

Esperado: **PASSA**.

- [ ] **Step 8: Commit**

```bash
git add backend .gitignore
git commit -m "feat(backend): esqueleto Spring Boot 3.5.6 com Java 21 e Maven wrapper"
```

---

### Task 3: Ambientes, segredos e Docker Compose

**Files:**
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-test.yml`
- Create: `backend/src/main/resources/application-prod.yml`
- Create: `backend/.env.example`
- Create: `docker-compose.yml` (raiz do repositório)
- Create: `backend/Dockerfile`

**Interfaces:**
- Consumes: `BetoBancoApplication` (Tarefa 2)
- Produces: os perfis `dev`, `test` e `prod`, e as variáveis `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `INFINITEPAY_API_KEY`, `INFINITEPAY_WEBHOOK_SECRET`, `CORS_ALLOWED_ORIGINS` — consumidas pelas fases 2 e 3.

- [ ] **Step 1: Criar os três perfis**

`backend/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/betobanco}
    username: ${DATABASE_USER:betobanco}
    password: ${DATABASE_PASSWORD:betobanco}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration

logging:
  level:
    com.betobanco: DEBUG
```

`backend/src/main/resources/application-test.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration
```

O perfil `test` não declara `datasource`: o Testcontainers injeta a URL em tempo de execução (Tarefa 4).

`backend/src/main/resources/application-prod.yml`:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 10
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  error:
    include-stacktrace: never
    include-message: never

logging:
  level:
    root: INFO
    com.betobanco: INFO
```

Em `prod` as variáveis **não têm valor padrão** de propósito: a aplicação deve recusar-se a subir sem elas, em vez de silenciosamente apontar para outro lugar.

- [ ] **Step 2: Documentar as variáveis sem valores reais**

`backend/.env.example`:

```dotenv
# Banco de dados (PostgreSQL 17)
DATABASE_URL=jdbc:postgresql://localhost:5432/betobanco
DATABASE_USER=betobanco
DATABASE_PASSWORD=

# Autenticacao (Fase 2) - minimo 32 bytes
JWT_SECRET=

# E-mail (Fase 3)
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=
SMTP_PASSWORD=

# Gateway de pagamento (Fase 3)
INFINITEPAY_API_KEY=
INFINITEPAY_WEBHOOK_SECRET=

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

- [ ] **Step 3: Criar o `docker-compose.yml` na raiz**

```yaml
services:
  postgres:
    image: postgres:17-alpine
    container_name: betobanco-postgres
    environment:
      POSTGRES_DB: betobanco
      POSTGRES_USER: betobanco
      POSTGRES_PASSWORD: betobanco
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U betobanco"]
      interval: 5s
      timeout: 5s
      retries: 10

  mailhog:
    image: mailhog/mailhog:latest
    container_name: betobanco-mailhog
    ports:
      - "1025:1025"
      - "8025:8025"

volumes:
  postgres-data:
```

A imagem do Postgres é `17-alpine` para casar com a instância real (`17.6.1.155`). Índice único parcial, `FOR UPDATE SKIP LOCKED` e `jsonb` se comportam de forma diferente entre versões maiores.

- [ ] **Step 4: Criar o `Dockerfile` do backend**

`backend/Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

O estágio de build separado mantém a imagem final sem JDK nem código-fonte, e o usuário não-root evita que o contêiner rode como root.

- [ ] **Step 5: Subir os serviços e verificar**

```bash
docker compose up -d
docker compose ps
```

Esperado: `betobanco-postgres` com status `healthy` e `betobanco-mailhog` `running`. A interface do MailHog fica em `http://localhost:8025`.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/application-dev.yml backend/src/main/resources/application-test.yml backend/src/main/resources/application-prod.yml backend/.env.example backend/Dockerfile docker-compose.yml
git commit -m "feat(backend): perfis de ambiente, segredos por variavel e docker-compose"
```

---

### Task 4: Banco de dados, Flyway e Testcontainers

Esta tarefa remove a exclusão temporária do `DataSource` da Tarefa 2 e prova, com um teste de integração real, que a aplicação conecta, que o Flyway executa e que `ddl-auto=validate` não quebra.

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__baseline.sql`
- Create: `backend/src/test/java/com/betobanco/support/PostgresTestBase.java`
- Create: `backend/src/test/java/com/betobanco/DatabaseIntegrationTest.java`
- Modify: `backend/src/main/resources/application.yml` (remover a exclusão de autoconfiguração)
- Modify: `backend/pom.xml` (adicionar `testcontainers:postgresql`)

**Interfaces:**
- Consumes: perfis da Tarefa 3
- Produces: `com.betobanco.support.PostgresTestBase` — classe-base anotada com `@SpringBootTest` e `@Testcontainers` que **todos** os testes de integração das fases seguintes estendem. Método público: nenhum; ela apenas configura o contêiner e registra as propriedades.

- [ ] **Step 1: Adicionar a dependência do Testcontainers para PostgreSQL**

Em `backend/pom.xml`, dentro de `<dependencies>`:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

A versão é gerenciada pelo BOM do Spring Boot; não fixe número aqui.

- [ ] **Step 2: Criar a migration baseline**

`backend/src/main/resources/db/migration/V1__baseline.sql`:

```sql
-- V1: baseline do Nucleo.
-- Nao cria nem altera nenhuma tabela. Existe para provar que o Flyway
-- esta ligado, que a tabela flyway_schema_history e criada e que o
-- versionamento comeca em 1. As tabelas do Nucleo entram a partir de V2,
-- na Fase 2.
SELECT 1;
```

- [ ] **Step 3: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/support/PostgresTestBase.java`:

```java
package com.betobanco.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class PostgresTestBase {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("betobanco")
                    .withUsername("betobanco")
                    .withPassword("betobanco");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registrarPropriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

O contêiner é `static` e iniciado uma vez por JVM, e não por classe de teste. Sem isso, cada classe subiria um PostgreSQL próprio e a suíte ficaria lenta o bastante para as pessoas pararem de rodá-la.

`backend/src/test/java/com/betobanco/DatabaseIntegrationTest.java`:

```java
package com.betobanco;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseIntegrationTest extends PostgresTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void bancoEhPostgres17() {
        String versao = jdbc.queryForObject("SHOW server_version", String.class);
        assertThat(versao).startsWith("17.");
    }

    @Test
    void flywayExecutouAMigrationBaseline() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(aplicadas).isGreaterThanOrEqualTo(1);
    }
}
```

- [ ] **Step 4: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw -q test -Dtest=DatabaseIntegrationTest
```

Esperado: **FALHA** com `NoSuchBeanDefinitionException` para `JdbcTemplate`, porque `application.yml` ainda exclui a autoconfiguração do `DataSource`.

- [ ] **Step 5: Remover a exclusão temporária**

Em `backend/src/main/resources/application.yml`, apague o bloco inteiro:

```yaml
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

O arquivo fica assim:

```yaml
spring:
  application:
    name: beto-banco

server:
  servlet:
    context-path: /api/v1

management:
  endpoints:
    web:
      exposure:
        include: health, info
  endpoint:
    health:
      probes:
        enabled: true
```

- [ ] **Step 6: Rodar e confirmar que passa**

```bash
cd backend && ./mvnw -q test -Dtest=DatabaseIntegrationTest
```

Esperado: **PASSA**, com os dois testes verdes. O Docker precisa estar rodando.

- [ ] **Step 7: Ajustar `ApplicationBootTest` para usar o banco**

Sem a exclusão, `ApplicationBootTest` passa a exigir um `DataSource`. Faça-o estender a base:

```java
package com.betobanco;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationBootTest extends PostgresTestBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextoSobeComABeanPrincipal() {
        assertThat(context.getBean(BetoBancoApplication.class)).isNotNull();
    }
}
```

Remova as anotações `@SpringBootTest` da classe — elas vêm de `PostgresTestBase`.

- [ ] **Step 8: Rodar a suíte inteira**

```bash
cd backend && ./mvnw -q test
```

Esperado: **todos passam**.

- [ ] **Step 9: Commit**

```bash
git add backend/pom.xml backend/src/main/resources backend/src/test
git commit -m "feat(backend): Flyway, conexao PostgreSQL 17 e base de testes com Testcontainers"
```

---

### Task 5: Correlação de logs (traceId)

O `traceId` é campo obrigatório do envelope de erro, então precisa existir antes do tratamento global de exceções.

**Files:**
- Create: `backend/src/main/java/com/betobanco/shared/trace/TraceIdFilter.java`
- Create: `backend/src/main/resources/logback-spring.xml`
- Create: `backend/src/test/java/com/betobanco/shared/trace/TraceIdFilterTest.java`

**Interfaces:**
- Consumes: nada
- Produces: `TraceIdFilter.MDC_KEY` (constante `String` = `"traceId"`) e `TraceIdFilter.HEADER` (constante `String` = `"X-Trace-Id"`). O `GlobalExceptionHandler` da Tarefa 7 lê `MDC.get(TraceIdFilter.MDC_KEY)`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/shared/trace/TraceIdFilterTest.java`:

```java
package com.betobanco.shared.trace;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void geraTraceIdQuandoClienteNaoEnvia() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.HEADER)).isNotBlank();
    }

    @Test
    void propagaTraceIdEnviadoPeloCliente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER, "trace-do-cliente");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("trace-do-cliente");
    }

    @Test
    void disponibilizaTraceIdNoMdcDuranteAChamadaELimpaDepois() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        String[] capturado = new String[1];
        doAnswer(invocation -> {
            capturado[0] = MDC.get(TraceIdFilter.MDC_KEY);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(capturado[0]).isNotBlank();
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
```

O terceiro teste é o que importa mais: sem a limpeza do MDC, threads reaproveitadas do pool carregam o `traceId` da requisição anterior, e os logs passam a mentir.

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw -q test -Dtest=TraceIdFilterTest
```

Esperado: **FALHA na compilação** — `TraceIdFilter` não existe.

- [ ] **Step 3: Implementar o filtro**

`backend/src/main/java/com/betobanco/shared/trace/TraceIdFilter.java`:

```java
package com.betobanco.shared.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
```

O `finally` é obrigatório: é ele que impede o vazamento de `traceId` entre requisições que compartilham a mesma thread.

- [ ] **Step 4: Rodar e confirmar que passa**

```bash
cd backend && ./mvnw -q test -Dtest=TraceIdFilterTest
```

Esperado: **PASSA**, três testes verdes.

- [ ] **Step 5: Configurar logs estruturados**

`backend/src/main/resources/logback-spring.xml`:

```xml
<configuration>
    <springProfile name="dev,test">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} %-5level [%X{traceId:-sem-trace}] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>
</configuration>
```

Adicione a dependência do encoder em `backend/pom.xml`:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>8.0</version>
</dependency>
```

Em `dev` o log é legível por humanos; em `prod` é JSON consumível por ferramenta.

- [ ] **Step 6: Rodar a suíte inteira**

```bash
cd backend && ./mvnw -q test
```

Esperado: **todos passam**.

- [ ] **Step 7: Commit**

```bash
git add backend/pom.xml backend/src/main/java/com/betobanco/shared/trace backend/src/main/resources/logback-spring.xml backend/src/test/java/com/betobanco/shared/trace
git commit -m "feat(backend): correlacao de requisicoes por traceId e logs estruturados"
```

---

### Task 6: Envelope de resposta

**Files:**
- Create: `backend/src/main/java/com/betobanco/shared/response/ApiResponse.java`
- Create: `backend/src/main/java/com/betobanco/shared/response/PaginationMeta.java`
- Create: `backend/src/main/java/com/betobanco/shared/response/PageResponse.java`
- Create: `backend/src/test/java/com/betobanco/shared/response/ApiResponseTest.java`

**Interfaces:**
- Consumes: nada
- Produces:
  - `ApiResponse.ok(T data)` → `ApiResponse<T>`
  - `ApiResponse.error(ErrorPayload error)` → `ApiResponse<Void>` (o tipo `ErrorPayload` nasce na Tarefa 7; nesta tarefa `ApiResponse` já reserva o campo `error` como `Object`, e a Tarefa 7 o especializa)
  - `PageResponse.from(org.springframework.data.domain.Page<T> page)` → `PageResponse<T>`

  Todos os controllers das fases 2 a 4 retornam um destes dois tipos.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/shared/response/ApiResponseTest.java`:

```java
package com.betobanco.shared.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sucessoSerializaComSuccessTrueEData() throws Exception {
        String json = mapper.writeValueAsString(ApiResponse.ok(new Exemplo("abc")));

        assertThat(json).isEqualTo("{\"success\":true,\"data\":{\"nome\":\"abc\"}}");
    }

    @Test
    void sucessoOmiteCampoErrorQuandoNulo() throws Exception {
        String json = mapper.writeValueAsString(ApiResponse.ok(new Exemplo("abc")));

        assertThat(json).doesNotContain("error");
    }

    @Test
    void listaPaginadaCarregaMetadadosCorretos() throws Exception {
        var page = new PageImpl<>(List.of(new Exemplo("a"), new Exemplo("b")),
                PageRequest.of(0, 20), 100);

        PageResponse<Exemplo> resposta = PageResponse.from(page);

        assertThat(resposta.success()).isTrue();
        assertThat(resposta.data()).hasSize(2);
        assertThat(resposta.pagination().page()).isEqualTo(0);
        assertThat(resposta.pagination().size()).isEqualTo(20);
        assertThat(resposta.pagination().totalElements()).isEqualTo(100);
        assertThat(resposta.pagination().totalPages()).isEqualTo(5);
    }

    record Exemplo(String nome) {}
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw -q test -Dtest=ApiResponseTest
```

Esperado: **FALHA na compilação** — as classes não existem.

- [ ] **Step 3: Implementar os três tipos**

`backend/src/main/java/com/betobanco/shared/response/ApiResponse.java`:

```java
package com.betobanco.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, Object error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(Object error) {
        return new ApiResponse<>(false, null, error);
    }
}
```

`backend/src/main/java/com/betobanco/shared/response/PaginationMeta.java`:

```java
package com.betobanco.shared.response;

public record PaginationMeta(int page, int size, long totalElements, int totalPages) {
}
```

`backend/src/main/java/com/betobanco/shared/response/PageResponse.java`:

```java
package com.betobanco.shared.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(boolean success, List<T> data, PaginationMeta pagination) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                true,
                page.getContent(),
                new PaginationMeta(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()));
    }
}
```

O `@JsonInclude(NON_NULL)` é o que garante que uma resposta de sucesso não carregue `"error": null` e que uma de erro não carregue `"data": null`.

- [ ] **Step 4: Rodar e confirmar que passa**

```bash
cd backend && ./mvnw -q test -Dtest=ApiResponseTest
```

Esperado: **PASSA**, três testes verdes.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/betobanco/shared/response backend/src/test/java/com/betobanco/shared/response
git commit -m "feat(backend): envelope padrao de resposta e metadados de paginacao"
```

---

### Task 7: Tratamento global de exceções

**Files:**
- Create: `backend/src/main/java/com/betobanco/shared/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/betobanco/shared/exception/BusinessException.java`
- Create: `backend/src/main/java/com/betobanco/shared/exception/NotFoundException.java`
- Create: `backend/src/main/java/com/betobanco/shared/exception/FieldErrorItem.java`
- Create: `backend/src/main/java/com/betobanco/shared/exception/ErrorPayload.java`
- Create: `backend/src/main/java/com/betobanco/shared/exception/GlobalExceptionHandler.java`
- Create: `backend/src/test/java/com/betobanco/shared/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `ApiResponse.error(Object)` (Tarefa 6), `TraceIdFilter.MDC_KEY` (Tarefa 5)
- Produces:
  - `enum ErrorCode` com `httpStatus()` → `int`. Valores criados aqui: `VALIDATION_ERROR`, `MALFORMED_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `RESOURCE_NOT_FOUND`, `CONFLICT`, `RATE_LIMIT_EXCEEDED`, `INTERNAL_ERROR`. As fases 2 e 3 acrescentam valores a este enum.
  - `BusinessException(ErrorCode code, String message)` — exceção-base que todo módulo lança.
  - `NotFoundException(String message)` — atalho para `RESOURCE_NOT_FOUND`.
  - `record ErrorPayload(String code, String message, int status, String path, String traceId, String timestamp, List<FieldErrorItem> fieldErrors)`
  - `record FieldErrorItem(String field, String message)`

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/shared/exception/GlobalExceptionHandlerTest.java`:

```java
package com.betobanco.shared.exception;

import com.betobanco.support.PostgresTestBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.betobanco.shared.trace.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest extends PostgresTestBase {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void excecaoDeNegocioViraEnvelopeDeErro() throws Exception {
        mockMvc.perform(get("/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.path").value("/teste/nao-encontrado"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.error.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void validacaoRetorna422ComListaDeCampos() throws Exception {
        mockMvc.perform(post("/teste/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("nome"));
    }

    @Test
    void corpoIlegivelRetorna400() throws Exception {
        mockMvc.perform(post("/teste/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isso nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void excecaoInesperadaNaoVazaDetalheInterno() throws Exception {
        mockMvc.perform(get("/teste/explode"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Erro interno do servidor"));
    }

    @TestConfiguration
    static class ControllerDeTeste {
        @Bean
        ControllerFalso controllerFalso() {
            return new ControllerFalso();
        }
    }

    @RestController
    @RequestMapping("/teste")
    static class ControllerFalso {

        @org.springframework.web.bind.annotation.GetMapping("/nao-encontrado")
        void naoEncontrado() {
            throw new NotFoundException("Recurso não encontrado");
        }

        @org.springframework.web.bind.annotation.GetMapping("/explode")
        void explode() {
            throw new IllegalStateException("detalhe interno que nao pode vazar");
        }

        @PostMapping("/validar")
        void validar(@Valid @RequestBody Entrada entrada) {
        }

        record Entrada(@NotBlank String nome) {}
    }
}
```

O último teste é o que protege o requisito de nunca expor detalhe interno: ele afirma que a mensagem devolvida é genérica, e não a da exceção original.

**Por que o `MockMvc` é construído à mão em vez de injetado.** O caminho óbvio seria `@AutoConfigureMockMvc(addFilters = false)` com um `MockMvc` injetado, mas `addFilters = false` desliga **todos** os filtros — inclusive o `TraceIdFilter` da Tarefa 5, que é quem popula o MDC. O `traceId` chegaria nulo e a asserção `$.error.traceId` falharia. Ligar os filtros também não serve: o starter de security passa a responder `401`. Construir o `MockMvc` registrando apenas o `TraceIdFilter` mantém a security fora do caminho e o trace dentro dele.

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw -q test -Dtest=GlobalExceptionHandlerTest
```

Esperado: **FALHA na compilação** — nenhuma das classes existe.

- [ ] **Step 3: Implementar os tipos de erro**

`backend/src/main/java/com/betobanco/shared/exception/ErrorCode.java`:

```java
package com.betobanco.shared.exception;

public enum ErrorCode {

    VALIDATION_ERROR(422),
    MALFORMED_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    RESOURCE_NOT_FOUND(404),
    CONFLICT(409),
    RATE_LIMIT_EXCEEDED(429),
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
```

`backend/src/main/java/com/betobanco/shared/exception/BusinessException.java`:

```java
package com.betobanco.shared.exception;

public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
```

`backend/src/main/java/com/betobanco/shared/exception/NotFoundException.java`:

```java
package com.betobanco.shared.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
```

`backend/src/main/java/com/betobanco/shared/exception/FieldErrorItem.java`:

```java
package com.betobanco.shared.exception;

public record FieldErrorItem(String field, String message) {
}
```

`backend/src/main/java/com/betobanco/shared/exception/ErrorPayload.java`:

```java
package com.betobanco.shared.exception;

import java.util.List;

public record ErrorPayload(
        String code,
        String message,
        int status,
        String path,
        String traceId,
        String timestamp,
        List<FieldErrorItem> fieldErrors) {
}
```

- [ ] **Step 4: Implementar o handler**

`backend/src/main/java/com/betobanco/shared/exception/GlobalExceptionHandler.java`:

```java
package com.betobanco.shared.exception;

import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> negocio(BusinessException ex, HttpServletRequest req) {
        return montar(ex.code(), ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validacao(MethodArgumentNotValidException ex,
                                                      HttpServletRequest req) {
        List<FieldErrorItem> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldErrorItem(e.getField(), e.getDefaultMessage()))
                .toList();
        return montar(ErrorCode.VALIDATION_ERROR, "Dados inválidos", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> ilegivel(HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
        return montar(ErrorCode.MALFORMED_REQUEST, "Corpo da requisição inválido", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> negado(AccessDeniedException ex,
                                                   HttpServletRequest req) {
        return montar(ErrorCode.FORBIDDEN, "Acesso negado", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> inesperada(Exception ex, HttpServletRequest req) {
        log.error("Erro nao tratado em {}", req.getRequestURI(), ex);
        return montar(ErrorCode.INTERNAL_ERROR, "Erro interno do servidor", req, List.of());
    }

    private ResponseEntity<ApiResponse<Void>> montar(ErrorCode code, String message,
                                                     HttpServletRequest req,
                                                     List<FieldErrorItem> campos) {
        ErrorPayload payload = new ErrorPayload(
                code.name(),
                message,
                code.httpStatus(),
                req.getRequestURI(),
                MDC.get(TraceIdFilter.MDC_KEY),
                Instant.now().toString(),
                campos);
        return ResponseEntity.status(code.httpStatus()).body(ApiResponse.error(payload));
    }
}
```

O handler de `Exception` registra a exceção completa no log — onde a equipe precisa dela — e devolve ao cliente apenas uma mensagem genérica. É essa separação que atende ao requisito de nunca expor stack trace.

- [ ] **Step 5: Rodar e confirmar que passa**

```bash
cd backend && ./mvnw -q test -Dtest=GlobalExceptionHandlerTest
```

Esperado: **PASSA**, quatro testes verdes.

- [ ] **Step 6: Rodar a suíte inteira**

```bash
cd backend && ./mvnw -q test
```

Esperado: **todos passam**.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/betobanco/shared/exception backend/src/test/java/com/betobanco/shared/exception
git commit -m "feat(backend): tratamento global de excecoes com envelope padronizado"
```

---

### Task 8: Paginação com teto

**Files:**
- Create: `backend/src/main/java/com/betobanco/shared/pagination/PageRequestFactory.java`
- Create: `backend/src/test/java/com/betobanco/shared/pagination/PageRequestFactoryTest.java`

**Interfaces:**
- Consumes: nada
- Produces: `PageRequestFactory.MAX_SIZE` (constante `int` = `100`) e o método estático `PageRequestFactory.of(Integer page, Integer size, String sort)` → `org.springframework.data.domain.Pageable`. Todo endpoint de listagem das fases 2 a 4 usa este método.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/shared/pagination/PageRequestFactoryTest.java`:

```java
package com.betobanco.shared.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class PageRequestFactoryTest {

    @Test
    void usaPadraoQuandoParametrosSaoNulos() {
        Pageable p = PageRequestFactory.of(null, null, null);

        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(20);
    }

    @Test
    void limitaSizeAoTeto() {
        Pageable p = PageRequestFactory.of(0, 100000, null);

        assertThat(p.getPageSize()).isEqualTo(PageRequestFactory.MAX_SIZE);
    }

    @Test
    void rejeitaValoresNegativos() {
        Pageable p = PageRequestFactory.of(-5, -10, null);

        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(20);
    }

    @Test
    void interpretaOrdenacaoDescendente() {
        Pageable p = PageRequestFactory.of(0, 20, "createdAt,desc");

        Sort.Order order = p.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void assumeAscendenteQuandoDirecaoOmitida() {
        Pageable p = PageRequestFactory.of(0, 20, "nome");

        Sort.Order order = p.getSort().getOrderFor("nome");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
```

O segundo teste é o que existe por segurança: sem o teto, `size=100000` transforma qualquer listagem em negação de serviço.

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw -q test -Dtest=PageRequestFactoryTest
```

Esperado: **FALHA na compilação** — `PageRequestFactory` não existe.

- [ ] **Step 3: Implementar**

`backend/src/main/java/com/betobanco/shared/pagination/PageRequestFactory.java`:

```java
package com.betobanco.shared.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {

    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    private PageRequestFactory() {
    }

    public static Pageable of(Integer page, Integer size, String sort) {
        int pagina = (page == null || page < 0) ? 0 : page;
        int tamanho = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(pagina, tamanho, parseSort(sort));
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        String[] partes = sort.split(",");
        String campo = partes[0].trim();
        if (campo.isEmpty()) {
            return Sort.unsorted();
        }
        Sort.Direction direcao = (partes.length > 1 && "desc".equalsIgnoreCase(partes[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direcao, campo);
    }
}
```

- [ ] **Step 4: Rodar e confirmar que passa**

```bash
cd backend && ./mvnw -q test -Dtest=PageRequestFactoryTest
```

Esperado: **PASSA**, cinco testes verdes.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/betobanco/shared/pagination backend/src/test/java/com/betobanco/shared/pagination
git commit -m "feat(backend): fabrica de paginacao com teto de tamanho de pagina"
```

---

### Task 9: Documentação OpenAPI

**Files:**
- Create: `backend/src/main/java/com/betobanco/config/OpenApiConfig.java`
- Create: `backend/src/test/java/com/betobanco/config/OpenApiConfigTest.java`
- Modify: `backend/pom.xml` (adicionar springdoc)

**Interfaces:**
- Consumes: nada
- Produces: a UI do Swagger em `/api/v1/swagger-ui.html` e o documento em `/api/v1/v3/api-docs`. As fases seguintes apenas anotam seus controllers com `@Tag`.

- [ ] **Step 1: Adicionar a dependência**

Em `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

- [ ] **Step 2: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/config/OpenApiConfigTest.java`:

```java
package com.betobanco.config;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
class OpenApiConfigTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentoOpenApiEhPublicadoComTitulo() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Beto Banco API"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }
}
```

- [ ] **Step 3: Rodar e confirmar que falha**

```bash
cd backend && ./mvnw -q test -Dtest=OpenApiConfigTest
```

Esperado: **FALHA** — o título vem como `OpenAPI definition`, o padrão do springdoc, e não `Beto Banco API`.

- [ ] **Step 4: Implementar a configuração**

`backend/src/main/java/com/betobanco/config/OpenApiConfig.java`:

```java
package com.betobanco.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI betoBancoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Beto Banco API")
                .version("v1")
                .description("API da plataforma de preparação bancária Beto Banco."));
    }
}
```

- [ ] **Step 5: Rodar e confirmar que passa**

```bash
cd backend && ./mvnw -q test -Dtest=OpenApiConfigTest
```

Esperado: **PASSA**.

- [ ] **Step 6: Commit**

```bash
git add backend/pom.xml backend/src/main/java/com/betobanco/config backend/src/test/java/com/betobanco/config
git commit -m "feat(backend): documentacao OpenAPI com springdoc"
```

---

### Task 10: Fronteiras de arquitetura verificadas pelo build

As três regras da spec só valem se falharem sozinhas. Cada regra é escrita, provada contra uma violação deliberada, e só então mantida.

**Files:**
- Create: `backend/src/test/java/com/betobanco/architecture/ModuleBoundariesTest.java`
- Modify: `backend/pom.xml` (adicionar ArchUnit)

**Interfaces:**
- Consumes: nada
- Produces: nenhuma API. Estabelece três invariantes que valem para todo código das fases 2 a 4.

- [ ] **Step 1: Adicionar a dependência**

Em `backend/pom.xml`:

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Escrever as três regras**

`backend/src/test/java/com/betobanco/architecture/ModuleBoundariesTest.java`:

```java
package com.betobanco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundariesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.betobanco");
    }

    @Test
    void nenhumModuloAcessaEntityOuRepositoryDeOutro() {
        for (String modulo : new String[]{
                "users", "auth", "students", "catalog", "entitlements",
                "payments", "webhooks", "email", "audit", "dashboard"}) {
            ArchRule regra = noClasses()
                    .that().resideOutsideOfPackage("com.betobanco." + modulo + "..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.betobanco." + modulo + ".entity..",
                            "com.betobanco." + modulo + ".repository..")
                    .because("modulos so podem se comunicar pelo pacote api/ do outro modulo");
            regra.allowEmptyShould(true).check(classes);
        }
    }

    @Test
    void nenhumControllerAceitaUserIdVindoDoCliente() {
        ArchRule regra = noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should(ArchConditions.temParametroDeIdentidade())
                .because("a identidade vem do token, nunca do cliente");
        regra.allowEmptyShould(true).check(classes);
    }

    @Test
    void nenhumControllerRetornaEntidadeJpa() {
        ArchRule regra = noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should(ArchConditions.retornaClasseAnotadaComEntity())
                .because("controllers devolvem DTO, nunca @Entity");
        regra.allowEmptyShould(true).check(classes);
    }

    static class ArchConditions {

        static com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>
        temParametroDeIdentidade() {
            return new com.tngtech.archunit.lang.ArchCondition<>("aceitar userId do cliente") {

                private static final java.util.Set<String> PROIBIDOS =
                        java.util.Set.of("userid", "user_id", "studentid", "student_id",
                                "alunoid", "aluno_id");

                private boolean nomeProibido(String valor) {
                    return valor != null
                            && PROIBIDOS.contains(valor.toLowerCase().replace("-", "_"));
                }

                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass item,
                                  com.tngtech.archunit.lang.ConditionEvents events) {
                    item.getMethods().forEach(metodo ->
                            metodo.getParameters().forEach(parametro -> {
                                String declarado = null;

                                if (parametro.isAnnotatedWith(PathVariable.class)) {
                                    PathVariable a = parametro.getAnnotationOfType(PathVariable.class);
                                    declarado = !a.value().isEmpty() ? a.value() : a.name();
                                } else if (parametro.isAnnotatedWith(RequestParam.class)) {
                                    RequestParam a = parametro.getAnnotationOfType(RequestParam.class);
                                    declarado = !a.value().isEmpty() ? a.value() : a.name();
                                }

                                if (nomeProibido(declarado)) {
                                    events.add(com.tngtech.archunit.lang.SimpleConditionEvent
                                            .satisfied(item, metodo.getFullName()
                                                    + " aceita '" + declarado + "' do cliente"));
                                }
                            }));
                }
            };
        }

        static com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>
        retornaClasseAnotadaComEntity() {
            return new com.tngtech.archunit.lang.ArchCondition<>("retornar @Entity") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass item,
                                  com.tngtech.archunit.lang.ConditionEvents events) {
                    item.getMethods().forEach(metodo -> {
                        var retorno = metodo.getRawReturnType();
                        if (retorno.isAnnotatedWith(Entity.class)) {
                            events.add(com.tngtech.archunit.lang.SimpleConditionEvent
                                    .satisfied(item, metodo.getFullName() + " retorna @Entity"));
                        }
                    });
                }
            };
        }
    }
}
```

O `allowEmptyShould(true)` é necessário porque nesta fase ainda não existem módulos de negócio nem controllers — sem ele, o ArchUnit falha por não encontrar classes a avaliar.

- [ ] **Step 3: Rodar e confirmar que passa vazio**

```bash
cd backend && ./mvnw -q test -Dtest=ModuleBoundariesTest
```

Esperado: **PASSA**, três testes verdes. Passar aqui não prova nada ainda — o próximo passo é que prova.

- [ ] **Step 4: Provar que as regras realmente pegam violações**

Crie temporariamente `backend/src/main/java/com/betobanco/users/entity/UsuarioTemp.java`:

```java
package com.betobanco.users.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "usuario_temp")
public class UsuarioTemp {
    @Id
    private UUID id;

    public UUID getId() {
        return id;
    }
}
```

E `backend/src/main/java/com/betobanco/dashboard/controller/ViolacaoTemp.java`:

```java
package com.betobanco.dashboard.controller;

import com.betobanco.users.entity.UsuarioTemp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ViolacaoTemp {

    @GetMapping("/violacao")
    public UsuarioTemp buscarPorUserId(@RequestParam("userId") UUID userId) {
        return new UsuarioTemp();
    }
}
```

Rode:

```bash
cd backend && ./mvnw -q test -Dtest=ModuleBoundariesTest
```

Esperado: **FALHA nos três testes**. A classe viola simultaneamente: importa `entity` de outro módulo, aceita `userId` por `@RequestParam` e retorna `@Entity`. Se algum dos três passar, a regra correspondente está quebrada e precisa de correção antes de seguir.

- [ ] **Step 5: Remover as classes de violação**

```bash
rm -rf backend/src/main/java/com/betobanco/users backend/src/main/java/com/betobanco/dashboard
cd backend && ./mvnw -q test -Dtest=ModuleBoundariesTest
```

Esperado: **PASSA** novamente.

- [ ] **Step 6: Rodar a suíte inteira**

```bash
cd backend && ./mvnw -q test
```

Esperado: **todos passam**.

- [ ] **Step 7: Commit**

```bash
git add backend/pom.xml backend/src/test/java/com/betobanco/architecture
git commit -m "test(backend): tres regras ArchUnit de fronteira de modulo verificadas no build"
```

---

### Task 11: Integração contínua

**Files:**
- Create: `.github/workflows/backend.yml`

**Interfaces:**
- Consumes: `backend/mvnw`, `backend/Dockerfile`
- Produces: um workflow que roda a cada push e pull request, executando build, testes (incluindo Testcontainers) e build da imagem.

- [ ] **Step 1: Criar o workflow**

`.github/workflows/backend.yml`:

```yaml
name: Backend

on:
  push:
    paths:
      - 'backend/**'
      - '.github/workflows/backend.yml'
  pull_request:
    paths:
      - 'backend/**'
      - '.github/workflows/backend.yml'

jobs:
  build-e-testes:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Configurar Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Build e testes
        working-directory: backend
        run: ./mvnw -B verify

      - name: Build da imagem Docker
        working-directory: backend
        run: docker build -t beto-banco-backend:${{ github.sha }} .
```

O runner `ubuntu-latest` já traz Docker, que o Testcontainers exige. O `cache: maven` evita rebaixar dependências a cada execução.

- [ ] **Step 2: Verificar localmente o mesmo comando que o CI roda**

```bash
cd backend && ./mvnw -B verify
```

Esperado: `BUILD SUCCESS`, com todos os testes verdes. Se passar aqui e falhar no CI, a diferença é ambiente, não código.

- [ ] **Step 3: Commit (sem push)**

```bash
git add .github/workflows/backend.yml
git commit -m "ci: workflow de build, testes e imagem Docker do backend"
```

**Não execute `git push`.** Publicar a branch no remoto `kauaxp77/Beto_Banco` é efeito fora do repositório local e depende de autorização explícita do responsável pelo projeto. O push e a verificação do CI ficam para o encerramento da fase.

- [ ] **Step 4: Confirmar que o CI passou** *(após autorização do push)*

Com a branch publicada, abra a aba Actions do repositório `kauaxp77/Beto_Banco` e confirme que o workflow **Backend** terminou verde. Até lá, a garantia local é o Passo 2, que roda exatamente o mesmo comando do CI.

---

## Critério de conclusão da Fase 1

A fase está pronta quando:

1. `cd backend && ./mvnw -B verify` termina com `BUILD SUCCESS` e todos os testes verdes
2. `docker compose up -d` sobe PostgreSQL 17 saudável e MailHog
3. A aplicação sobe com o perfil `dev` e `GET http://localhost:8080/api/v1/actuator/health` devolve `{"status":"UP"}`
4. `http://localhost:8080/api/v1/swagger-ui.html` abre com o título "Beto Banco API"
5. `git ls-files frontend-react/node_modules | wc -l` devolve `0`
6. O workflow **Backend** está verde no GitHub Actions
7. Os três testes ArchUnit passam e foram comprovadamente capazes de reprovar uma violação (Tarefa 10, Passo 4)

Nenhuma regra de negócio foi implementada nesta fase, e isso é intencional. O que existe ao final é um chassi verificado.

---

## Pendências registradas para as fases seguintes

**Colisão de nome do diretório do frontend.** O `vercel.json` na raiz já contém a regra `"/(.*)" → "/frontend/$1"`. A spec (seção 9.1) prevê criar o novo frontend em `frontend/`. Antes da Fase 4, é preciso decidir entre renomear o diretório novo para `web/` ou reescrever a regra de rewrite. Deixar como está produziria um conflito silencioso entre o código-fonte e a saída de build servida pela Vercel.

**Documentação do webhook da InfinitePay.** Necessária antes da Fase 3. Formato de payload, esquema de assinatura e eventos de split.

**Versão do formato de hash de senha do Supabase.** Verificar na Fase 2, ao escrever a migration de usuários. Há apenas 2 registros em `public.profiles`, então o custo de errar é baixo, mas o caminho precisa estar definido.

**Dados legados a preservar.** O banco real contém 45 questões, 223 alternativas, 6 tentativas, 75 respostas e 4 configurações de CMS. Nenhuma migration pode tocá-los. Eles pertencem aos sub-projetos 2 e 3.
