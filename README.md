# Beto Banco - Plataforma de Simulação Bancária 🏦🥇

Beto Banco é uma plataforma de estudo premium projetada para otimizar o desempenho de concurseiros em certames da Caixa Econômica, Banco do Brasil e Banco Central. O sistema oferece Simulados com metodologia ativa baseada em Real Engine (com cronômetro real, estatísticas, correção heurística e rankeamento), Painel Administrativo com análise de dados para instrutores e um Dashboard intuitivo focado na conversão e desempenho do aluno.

## 🚀 Tecnologias e Arquitetura

O projeto adota uma arquitetura Serverless orientada a serviços com frontend moderno (Vite) e backend (BaaS) gerenciado pelo Supabase.

### 🌐 Frontend (React + Vite)
- **Vite & React 18**: Perfomance de build e renderização ultra rápida usando JSX.
- **React Router Dom (v6)**: Gerenciamento inteligente de rotas privadas (Dashboard Aluno) e Restritas (Dashboard Admin).
- **Lucide React**: Iconografia padronizada em vetor.
- **Recharts**: Gráficos dinâmicos vetoriais de KPIs e engajamento.
- **Context API (`AuthContext`)**: Gerenciamento de estado de usuário em toda a árvore de componentes para autorização JWT.
- **React Hot Toast**: Central de notificações assíncronas do sistema e feedback interativo.

### 💾 Backend (Supabase + PostgreSQL)
- **Supabase Auth**: Autenticação com sessão JWT assíncrona.
- **PostgreSQL Database**:
  - `profiles`: Relação (1:1) de usuários logados.
  - `simulado_attempts`: Banco transacional para salvar desempenho individual do aluno.
  - `site_settings`: Entidade EAV para Content Management System dinâmico via Admin (Taxas e Integrações).
- **RLS (Row Level Security)**: Autenticidade forçada direto na engine SQL Database para barrar leitura indesejada.

## 🌟 Módulos Principais

1. **Dashboard do Aluno**: Central de relatórios após cada bateria de testes, cronômetro nativo persistente e histórico evolutivo.
2. **Motor de Simulado (SimuladoEngine)**: Tela de foco absoluto, imitando as páginas oficias de bancas reais (CESGRANRIO e CEBRASPE), controle anti-cola e log assíncrono.
3. **Painel de Administração (SaaS Hub)**:
   - *Overview*: KPIs globais com Recharts (Gráficos de barras e linhas dinâmicos).
   - *Gestão de Alunos*: Controle de dados sensíveis dos inscritos do Hotmart.
   - *Módulo Cursos & Simulados*: Gestão de currículos e vídeo-aulas (integração VIMEO/Yotube Placeholder).
   - *Construtor de Questões (QuizBuilder)*: Gestão de Banco de Questões (CRUD via REST Supabase).

## 💻 Instalação & Setup (Local)

1. Clone o repositório na sua máquina local:
   ```bash
   git clone https://github.com/kauaxp77/Beto_Banco.git
   ```

2. Entre no diretório do projeto Frontend:
   ```bash
   cd Beto_Banco/frontend-react
   ```

3. Instale as dependências Node (requer `npm` v9+):
   ```bash
   npm install --legacy-peer-deps
   ```

4. Configure o arquivo `.env.local`:
   Na raiz do frontend crie um `.env.local` e informe suas credenciais Supabase:
   ```env
   VITE_SUPABASE_URL=seu-url-supabase-aqui
   VITE_SUPABASE_ANON_KEY=sua-api-key-anonima-aqui
   ```

5. Inicie o Servidor de Desenvolvimento:
   ```bash
   npm run dev
   ```
O frontend estará acessível em `http://localhost:5173`. Para acessar os painéis simulados você deve clicar em `/login` e criar uma conta.

---
### Produção:
O bundle de build deve ser acionado por `npm run build` e hospedado na Vercel ou Netlify usando pastas em Dist Mode. 

Feito com estratégia pedagógica e tecnologia avançada para escalar Cursos Jurídicos e Carreiras Bancárias.
