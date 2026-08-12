# AprovaÃ§Ã£o Passo a Passo

Plataforma visual para venda de cursos preparatÃ³rios de concursos bancÃ¡rios. O projeto apresenta um catÃ¡logo premium de bancos, cards de compra, identidade neon/cyberpunk e uma estrutura inicial para integraÃ§Ã£o futura com backend Java e banco de dados.

> Estado atual: o frontend estÃ¡tico e a pÃ¡gina de catÃ¡logo estÃ£o funcionais. O backend e os scripts SQL existem como base de evoluÃ§Ã£o, mas ainda nÃ£o estÃ£o conectados Ã  interface.

## SumÃ¡rio

- [VisÃ£o geral](#visÃ£o-geral)
- [Tecnologias e estado do projeto](#tecnologias-e-estado-do-projeto)
- [Como executar](#como-executar)
- [Estrutura de diretÃ³rios](#estrutura-de-diretÃ³rios)
- [Frontend](#frontend)
- [CatÃ¡logo de bancos](#catÃ¡logo-de-bancos)
- [Assets e identidade visual](#assets-e-identidade-visual)
- [Banco de dados](#banco-de-dados)
- [Backend](#backend)
- [CustomizaÃ§Ã£o e manutenÃ§Ã£o](#customizaÃ§Ã£o-e-manutenÃ§Ã£o)
- [LimitaÃ§Ãµes atuais](#limitaÃ§Ãµes-atuais)

## VisÃ£o geral

```mermaid
flowchart LR
	Browser[Navegador] --> Pages[HTML em frontend/html]
	Pages --> Scripts[JavaScript em frontend/js]
	Pages --> Styles[CSS em frontend/css]
	Scripts --> Catalog[CatÃ¡logo local de bancos]
	Catalog --> Images[Imagens e SVGs em frontend/images]
	Database[database/schema.sql e inserts.sql] -. futuro consumo .-> Backend[backend Java]
	Backend -. futura API .-> Scripts
```

A entrada principal Ã© a landing page em `frontend/index.html`. Ela apresenta a proposta da plataforma, o responsÃ¡vel pelo projeto e a transiÃ§Ã£o para o catÃ¡logo bancÃ¡rio em `frontend/bancos.html`, onde ficam os cursos por instituiÃ§Ã£o.

## Tecnologias e estado do projeto

| Camada | Tecnologia atual | Estado |
| --- | --- | --- |
| Frontend | HTML5, CSS3 e JavaScript puro | Implementado e executÃ¡vel sem build |
| Design | SVG, PNG, Google Fonts, layout responsivo | Implementado |
| CatÃ¡logo | Array local em `frontend/js/bancos.js` | Implementado |
| Banco de dados | Scripts SQL para tabela `banks` | Estrutura pronta, nÃ£o integrada |
| Backend | Estrutura de diretÃ³rios Java e `application.properties` | Scaffold inicial, sem cÃ³digo Java ou build configurado |

NÃ£o hÃ¡ `package.json`, `pom.xml`, `build.gradle`, dependÃªncias npm, wrapper Maven ou Gradle no repositÃ³rio atual. Portanto, nÃ£o Ã© necessÃ¡rio instalar pacotes para visualizar o frontend.

## Como executar

### OpÃ§Ã£o 1: abrir diretamente

Abra `frontend/index.html` no navegador. A pÃ¡gina foi validada usando a URL local do arquivo e nÃ£o exige servidor para renderizar a landing ou o catÃ¡logo.

Use os CTAs **Explorar bancos** ou **Ver todos os bancos** para acessar o catÃ¡logo. TambÃ©m Ã© possÃ­vel abrir `frontend/bancos.html` diretamente.

### OpÃ§Ã£o 2: servir por HTTP local

Recomendado para testar como a aplicaÃ§Ã£o se comportarÃ¡ em hospedagem estÃ¡tica:

```powershell
Set-Location frontend
python -m http.server 5500
```

Depois, abra:

```text
http://localhost:5500/index.html
```

TambÃ©m Ã© possÃ­vel usar qualquer extensÃ£o de servidor estÃ¡tico do VS Code.

## Estrutura de diretÃ³rios

```text
APPBANCOS/
|-- README.md
|-- frontend/
|   |-- assets/
|   |   |-- neon-cyber-bg.svg          # Fundo neon ativo do catÃ¡logo
|   |   `-- neon-corporate-bg.svg      # VariaÃ§Ã£o alternativa de fundo
|   |-- css/
|   |   |-- style.css                  # Estilos globais
|   |   |-- landing.css                # Landing page de apresentaÃ§Ã£o
|   |   |-- bancos.css                 # CatÃ¡logo bancÃ¡rio e cards
|   |   |-- responsive.css             # Breakpoints
|   |   |-- cards.css
|   |   |-- cursos.css
|   |   |-- dashboard.css
|   |   |-- header.css
|   |   `-- outros-bancos.css
|   |-- html/
|   |   |-- index.html                 # Landing page e entrada principal
|   |   |-- bancos.html                # CatÃ¡logo de cursos por banco
|   |   |-- curso.html
|   |   |-- cursos.html
|   |   |-- dashboard.html
|   |   |-- login.html
|   |   |-- outros-bancos.html
|   |   `-- perfil.html
|   |-- images/
|   |   |-- bancos/                    # Capas e logos dos bancos
|   |   |-- logo/                      # Marca da plataforma
|   |   |-- banners/
|   |   |-- cursos/
|   |   |-- icones/
|   |   `-- icons/
|   `-- js/
|       |-- landing.js                 # RevelaÃ§Ã£o e movimento sutil da landing
|       |-- bancos.js                  # RenderizaÃ§Ã£o do catÃ¡logo
|       |-- main.js                    # Tema e fallback de arte
|       |-- cursos.js
|       |-- dashboard.js
|       |-- login.js
|       |-- outros-bancos.js
|       `-- usuario.js
|-- backend/
|   |-- java/
|   |   |-- config/
|   |   |-- controller/
|   |   |-- model/
|   |   |-- repository/
|   |   |-- security/
|   |   `-- service/
|   `-- resources/
|       `-- application.properties
`-- database/
		|-- schema.sql
		`-- inserts.sql
```

## Frontend

### Landing page

Arquivo: `frontend/index.html`

A landing reÃºne quatro Ã¡reas:

- hero com o posicionamento da plataforma e CTA para a seleÃ§Ã£o de bancos;
- apresentaÃ§Ã£o do Prof. BetÃ£o e do propÃ³sito do projeto;
- convite final para explorar as instituiÃ§Ãµes bancÃ¡rias disponÃ­veis.
- rodapÃ© temÃ¡tico com contatos, mensagem, suporte e CTA para o WhatsApp informado.

Os estilos especÃ­ficos ficam em `frontend/css/landing.css`, isolados das regras de `bancos.css`. O arquivo `frontend/js/landing.js` revela seÃ§Ãµes ao entrar na viewport, aplica parallax discreto para ponteiros precisos e respeita `prefers-reduced-motion`.

### CatÃ¡logo bancÃ¡rio

Arquivo: `frontend/bancos.html`

Responsabilidades principais:

- Exibe a marca `AprovaÃ§Ã£o Passo a Passo`.
- Renderiza os cards dentro de `#banksGrid` a partir de um template HTML.
- Usa o background responsivo `frontend/assets/neon-cyber-bg.svg` em tela cheia.
- Mostra menu contextual por card, fechado ao clicar fora ou pressionar `Esc`.
- MantÃ©m o CTA de compra e o botÃ£o de carrinho para cursos publicados.

### Layout responsivo

O catÃ¡logo usa a seguinte grade no estado atual:

| Largura | Colunas |
| --- | --- |
| Desktop | 5 |
| Ate 1024px | 3 |
| Ate 768px | 2 |
| Ate 640px | 1 |

Os estilos principais da tela ficam em `frontend/css/bancos.css`; os breakpoints complementares ficam em `frontend/css/responsive.css`.

### Tema e fallback visual

`frontend/js/main.js` fornece:

- persistencia do tema no `localStorage` com a chave `aprovacao-passoa-passo-theme`;
- os temas `dark` e `midnight`;
- geracao de uma arte SVG em data URI como ultimo fallback de imagem de banco.

O catalogo atual nao mostra um botao de alternancia de tema no cabecalho, mas a infraestrutura de tema permanece disponivel para outras telas ou futuras interfaces.

## CatÃ¡logo de bancos

O catalogo e definido localmente no array `banks` de `frontend/js/bancos.js`.

| Ordem | Banco | Sigla | Estado atual |
| --- | --- | --- | --- |
| 1 | Banco do Brasil | BB | Publicado |
| 2 | Caixa Economica Federal | CAIXA | Publicado |
| 3 | Banco da Amazonia | BASA | Publicado |
| 4 | Banco do Nordeste | BNB | Publicado |
| 5 | Banco Nacional de Desenvolvimento Economico e Social | BNDES | Publicado |
| 6 | Banco de Brasilia | BRB | Publicado |
| 7 | Banrisul | BANRISUL | Publicado |
| 8 | Banestes | Banco do Estado do Espirito Santo | Publicado |
| 9 | Banpara | Banco do Estado do Para | Publicado |
| 10 | Banese | Banco do Estado de Sergipe | Publicado |
| 11 | BDMG | Banco de Desenvolvimento de Minas Gerais | Publicado |
| 12 | BRDE | Banco Regional de Desenvolvimento do Extremo Sul | Publicado |
| 13 | BANDES | Banco de Desenvolvimento do Espirito Santo | Publicado |

### Regras de carregamento das capas

Para cada banco, o renderer tenta as fontes nesta ordem:

1. `frontend/images/bancos/<imageKey>.png`;
2. `frontend/images/bancos/<imageKey>.svg`;
3. arte SVG gerada no navegador por `createBankArtwork`.

As capas originais em PNG estao presentes para `bb`, `caixa`, `basa`, `bnb`, `bndes`, `brb`, `banrisul`, `banese`, `banpara` e `banestes`. As capas de `bdmg`, `brde` e `bandes` possuem SVG local como fallback visual atual.

Os links `linkCompra` atuais usam `https://example.com/...` e devem ser substituidos pelos links reais de checkout antes de publicar a plataforma.

## Assets e identidade visual

| Asset | Uso |
| --- | --- |
| `frontend/assets/banking-architecture-bg.png` | Fundo oficial enviado, com arquitetura, logos bancÃ¡rias e iluminaÃ§Ã£o azul/dourada |
| `frontend/assets/neon-cyber-bg.svg` | Asset legado do fundo neon anterior, nÃ£o aplicado nas telas principais |
| `frontend/assets/neon-corporate-bg.svg` | Versao alternativa de ambiente corporativo futurista |
| `frontend/images/professor/prof-betao.png` | Foto oficial do Prof. BetÃ£o usada na landing page |
| `frontend/images/professor/prof-betao-signature-white.png` | Assinatura branca do Prof. BetÃ£o sobreposta Ã  foto da landing |
| `frontend/images/logo/aprovacao-logo.svg` | Logo exibida no cabecalho do catalogo |
| `frontend/images/bancos/*.png` | Capas originais recebidas para os cards |
| `frontend/images/bancos/*.svg` | Capas complementares e fallbacks locais |

O fundo oficial usa `background-size: cover` e `background-position: center center`, mantendo a arquitetura central como ponto de foco e preservando a proporÃ§Ã£o original. Um overlay discreto de contraste Ã© aplicado separadamente para a leitura dos cards e textos, sem alterar o arquivo de imagem.

A foto de `prof-betao.png` possui transparÃªncia e Ã© exibida com `object-fit: contain`, preservando o enquadramento original dentro da moldura da landing. A assinatura derivada do arquivo fornecido Ã© mantida em branco, com fundo transparente, e fica sobreposta no canto inferior direito da moldura.

## Banco de dados

Arquivos:

- `database/schema.sql`
- `database/inserts.sql`

### Tabela `banks`

| Coluna | Tipo | Descricao |
| --- | --- | --- |
| `id` | `INTEGER` | Identificador primario |
| `name` | `VARCHAR(200)` | Nome do banco/curso |
| `sigla` | `VARCHAR(20)` | Sigla ou identificacao curta |
| `status` | `VARCHAR(20)` | Estado de publicacao |
| `link_compra` | `TEXT` | URL de compra |
| `image_path` | `TEXT` | Caminho da imagem de capa |
| `created_at` | `TIMESTAMP` | Data de criacao, com valor padrao atual |

O schema tambem cria indices para `status` e `sigla`.

### Como preparar dados

Execute `schema.sql` antes de `inserts.sql` no banco SQL escolhido. O projeto nao inclui driver, URL de conexao, credenciais ou ferramenta de migracao; esses detalhes precisam ser definidos ao implementar o backend.

Os inserts de exemplo atualmente contem cinco bancos, enquanto o frontend possui treze itens no catalogo local. A integracao futura deve unificar essas duas fontes de dados.

## Backend

O diretorio `backend/java` organiza a futura aplicacao em camadas:

| Diretorio | Responsabilidade planejada |
| --- | --- |
| `config/` | Configuracoes da aplicacao |
| `controller/` | Endpoints HTTP |
| `model/` | Entidades e DTOs |
| `repository/` | Persistencia |
| `security/` | Autenticacao e autorizacao |
| `service/` | Regras de negocio |

Atualmente esses diretorios contem somente arquivos `.gitkeep`; nao ha classes Java, classe principal do Spring Boot ou arquivo de build. O unico arquivo de configuracao existente e `backend/resources/application.properties`:

```properties
spring.application.name=aprovacao-passo-a-passo
server.port=8080
```

Para transformar essa estrutura em uma API funcional, sera necessario adicionar, no minimo:

1. Um projeto Spring Boot com `pom.xml` ou `build.gradle`.
2. Uma classe principal da aplicacao.
3. Configuracao de datasource e driver do banco escolhido.
4. Entidade, repositorio, servico e controller para `banks`.
5. Endpoints consumidos pelo frontend em substituicao ao array local.
6. Autenticacao, autorizacao e validacao de URLs de compra antes de producao.

## CustomizaÃ§Ã£o e manutenÃ§Ã£o

### Adicionar ou alterar um banco

1. Edite o array `banks` em `frontend/js/bancos.js`.
2. Defina `id`, `name`, `sigla`, `subtitle`, `imageKey`, `status`, `linkCompra` e `colors`.
3. Adicione a capa em `frontend/images/bancos/` usando `<imageKey>.png`.
4. Opcionalmente adicione `<imageKey>.svg` como fallback.
5. Se o banco tambem for persistido, atualize os scripts SQL ou a futura API.

### Alterar o fundo oficial

O asset atual Ã© `frontend/assets/banking-architecture-bg.png`. Ele Ã© aplicado em `body::before`, `body.bancos-showcase::before` e `body.landing-page::before`; mantenha a mesma proporÃ§Ã£o ao substituÃ­-lo. Os antigos LEDs de borda foram removidos das telas principais.

### Atualizar a imagem do professor

Substitua `frontend/images/professor/prof-betao.png` por uma imagem autorizada do Prof. BetÃ£o. Prefira uma foto vertical com transparÃªncia; o frame da landing usa `object-fit: contain` para preservar a composiÃ§Ã£o.

### Alterar a grade e o tamanho dos cards

Edite as regras finais de `frontend/css/bancos.css`:

- `.bancos-showcase .banks-grid` controla colunas e espacamento;
- `.bank-card` controla a altura e a proporcao de capa/conteudo;
- `.bank-card__media` e `.bank-card__body` controlam as duas partes do card.

## LimitaÃ§Ãµes atuais

- Nao ha API, autenticacao real, checkout real ou persistencia integrada.
- Os links de compra sao placeholders em `example.com`.
- As acoes do menu do card sao visuais; nao salvam alteracoes.
- Nao ha suite automatizada de testes, linter configurado ou pipeline de CI no repositorio.
- O backend nao pode ser iniciado ate que um projeto Java/Spring executavel seja adicionado.
- Os dados do SQL e do frontend ainda nao estao sincronizados.

## VerificaÃ§Ã£o manual recomendada

Antes de publicar, valide ao menos:

1. O carregamento de `frontend/index.html` em desktop e celular, incluindo os CTAs para o catÃ¡logo.
2. O carregamento e o enquadramento da foto do professor na landing.
3. A exibicao das 13 capas e o fallback de imagem quando um asset estiver ausente.
4. O menu de cada card, incluindo fechamento por clique externo e tecla `Esc`.
5. Os links reais de compra em uma nova aba.
6. A execucao de `schema.sql` e `inserts.sql` no banco selecionado.
7. A substituicao do catalogo local por uma API quando o backend estiver pronto.

## LicenÃ§a

Nenhum arquivo de licenca foi encontrado no repositorio. Defina uma licenca antes de distribuir ou abrir o projeto para contribuicoes externas.
#   A P R O V A - O - P A S S O - A - P A S S O 1 3 . 0 
 
 
