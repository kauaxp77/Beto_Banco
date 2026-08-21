# Beto Banco - Plataforma SaaS de Preparação Bancária 🏦🥇

Beto Banco é uma plataforma premium de altíssimo desempenho, projetada especificamente para otimizar a aprovação de candidatos nos concursos bancários mais concorridos do Brasil (Caixa Econômica, Banco do Brasil e Banco Central). Construída sob um motor Next-Gen (Vite + React 18) aliado a banco de dados em tempo real (Supabase PostgreSQL), a plataforma introduz uma pedagogia focada em dados (Data-Driven Learning).

## ✨ Funcionalidades e Features Detalhadas

A plataforma é dividida fundamentalmente em 3 grandes ambientes integrados, oferecendo uma Jornada do Usuário (UX) intuitiva e livre de distrações.

### 📌 1. Landing Page (Portal de Conversão)
Uma vitrine elegante com design "Premium Black & Gold", responsiva e focada puramente na captação de leads.
- **Hero Section Dinâmico:** Apresentação persuasiva com Micro-Animações e prova social flutuante de aprovações. Background radial com desfoque de movimento, imergindo o usuário na Identidade Visual.
- **Funil de Conversão Integrado:** Todos os "Call to Actions" direcionam o tráfego para a página privada de `/login`, exigindo cadastro (Supabase Auth) para maximizar o número de e-mails capturados.
- **Módulo de Cursos (Grid Viewer):** Listagem dos blocos modulares bancários com indicativos visuais (Tags) das bancas suportadas.
- **Gestão de Depoimentos:** Carrossel de provas sociais renderizados em tempo real via tabela no banco.

### 🦅 2. Painel do Aluno (SaaS Workspace)
O ambiente restrito aos alunos aprovados, criado para simular o ambiente de prova e forçar métricas de evolução.
- **Login e Autenticação Jwt Seamless:** Controle dinâmico de autorização via RLS (Row Level Security). O aluno não consegue acessar áreas sensíveis.
- **Dashboard Global (Estatísticas Visuais):** O aluno acompanha quantas questões acertou via Gráficos de Pizza interativos e histórico temporal, utilizando renderizadores da biblioteca `Recharts`.
- **Motor de Simulado (SimuladoEngine):** A feature de ouro da plataforma. Recria exatamente o cenário da CEBRASPE e CESGRANRIO.
  - *Timer Persistente:* Bloqueia a submissão e força a entrega da prova se esgotar as 4 horas.
  - *Heurística Right/Wrong:* Sistema dinâmico anti-cola (não permite desfazer uma escolha para simular testes reais).
  - *Analytics Pós-Teste (Gabarito Oficial):* Tela com pontuação imediata (Taxa de Acertos) sincronizando com a base transacional (Supabase Database).

### �️ 3. Painel Administrativo (SaaS Hub)
O Cockpit de gerência técnica voltado ao instrutor ou curador de conteúdo, completamente isolado via flag JWT (Usuário Elevado).
- **Métricas Globais Reais (Graficos em Barras/Linhas):** Dashboard com Recharts visualizando pico de usuários ao vivo (Signups, Conclusões de Simulado) no mês vigente.
- **Gestão de Alunos (CRUD):** Tabela mapeando leads capturados e alunos matriculados ativos (Nome, Email, UID Seguro do Supabase).
- **Landing Page CMS:** Configurações integradas que alteram dados diretamente da Landing Page (Ex: Links promocionais, Vídeos URL e Textos do Hero) através do banco de dados relacional EAV (`site_settings`).
- **QuizBuilder (Gestor de Provas):** Módulo de controle do Banco de Questões; Adiciona, edita, exclui ou arquiva novas provas para alimentar o ambiente de testes do aluno, usando notificações em tela via componente (`React Hot Toast`).

## 🚀 Tecnologias e Arquitetura do Sistema

A arquitetura do projeto adota modelo SPAs (Single Page Application) com controle de roteamento asíncrono para garantir navegabilidade de "Zero Page Reloading".

### 💻 Stack Frontend
- **React v18 & Vite Server**: Pipeline de construção hiper veloz otimizando HMR (Hot Module Replacement) durante o desenvolvimento.
- **React Router v6**: Sistema de enroteamento protegendo rotas Auth-Only via arquitetura aninhada (Nested Routes e Navigate Blocks).
- **Recharts (D3.js base)**: Renderização vetorial matemática.
- **Lucide React**: Biblioteca iconográfica de 24x24 px (Custom Stroke) injetando SVGs diretos sem gargalo HTTP.
- **React Hot Toast**: Stack Notifications (Notificações Flutuantes Não Invasivas).
- **Vanilla CSS (Design System Próprio)**: Tokens declarativos em `:root` definindo Grid Flex, animações Keyframes suaves sem depender de frameworks externos inchados (Boostrap/Tailwind).

### 🗄️ Arquitetura Backend (Supabase PostgreSQL)
- **Supabase Authentication**: Gestão de Senhas Hash (Crypt) e Tokens (Cookie & JWT).
- **Modelagem Relacional de Dados**:
  - `profiles`: Vincula e centraliza metadados dos perfis, usando triggers (`auth.users`) quando da criação.
  - `simulado_attempts`: Banco transacional escalável com FK apontada para IDs autogerados.
  - `site_settings`: CMS dinâmico leve.
- **RLS Assíncrono**: Regras estritas declarando que *Apenas usuários de cargo ROLE_ADMIN possuem política UPDATE/DELETE em registros sensíveis*.

## 💻 Setup, Build e Deploy

Este projeto é modelado utilizando ferramentas nativas do ecossistema NPM de vanguarda.

1. Baixe o código fonte para a sua máquina local:
   ```bash
   git clone https://github.com/kauaxp77/Beto_Banco.git
   ```

2. Carregue os Módulos do Sistema pela pasta central:
   ```bash
   cd Beto_Banco/frontend-react
   npm install --legacy-peer-deps
   ```

3. Declare suas Variáveis de Ambiente no arquivo `.env.local` (*Recomendado usar Supabase Local ou Cloud Base*):
   ```env
   VITE_SUPABASE_URL=seudominio.supabase.co
   VITE_SUPABASE_ANON_KEY=suachavedeAPIJWT
   ```

4. Boot de Sistema Local:
   ```bash
   npm run dev
   ```
O frontend estará acessível em `http://localhost:5173`. Você deve acessar `/login` pelo navegador para se credenciar antes de tentar abrir páginas administrativas.

---
### ☁️ Produção e Integração (Deploy Automático)
O framework base suporta hospedagem direta sem complicações, o comando oficial de transpilação de arquivos e minificação de Node (Babel):
```bash
npm run build
```
Enviará seu projeto puro (HTML/CSS/JS) com os bundles minificados para a matriz `/dist`. Use este diretório para realizar Deployers em Clouds confiáveis (Vercel, Railway, Render ou AWS Amplify). 

> *Plataforma UX Desenvolvida focado na maximização de leads qualitativos em Carreiras Administrativas/Jurídicas.*
