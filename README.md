<div align="center">
  <img src="frontend/images/logo/aprovacao-logo.svg" alt="Aprovação Passo a Passo Logo" width="300" />
  
  # Aprovação Passo a Passo
  
  **Plataforma de Alta Performance para Preparatórios Bancários**
  
  [![Deploy Status](https://img.shields.io/badge/Deploy-Vercel_Ready-000000?style=for-the-badge&logo=vercel&logoColor=white)](#-deploy-na-vercel)
  [![Tech Stack](https://img.shields.io/badge/Frontend-HTML_|_CSS_|_JS-3178C6?style=for-the-badge)](#-tecnologias)
  [![Backend](https://img.shields.io/badge/Backend-Java_|_Spring_Ready-6DB33F?style=for-the-badge&logo=spring)](#-arquitetura)

</div>

<br>

Um catálogo de cursos premium voltado para concursos bancários. Este projeto foi desenvolvido com forte foco em **UI/UX Design**, entregando uma interface responsiva, com identidade visual sofisticada (elementos neon/glassmorphism) e altíssima performance estrutural. 

O projeto conta com um **Frontend 100% estático otimizado** e uma arquitetura robusta preparada para uma futura integração com um **Backend Java/Spring**.

---

## 🚀 Destaques do Projeto

- 🎨 **UI/UX Premium:** Design imersivo com efeitos parallax sutis, glassmorphism e identidade visual imponente desenhada para converter.
- ⚡ **Performance Caching:** Arquitetura estática otimizada, já estruturada para caches massivos no Edge via Vercel.
- 📱 **100% Responsivo:** O layout se adapta perfeitamente, usando as mais modernas especificações de Web Design para abranger desde telas Ultra-Wide até dispositivos móveis.
- 🛠️ **Arquitetura Desacoplada:** Frontend limpo na raiz pronto para o deploy e um scaffold robusto aguardando a API Java/Spring.

---

## 💻 Tecnologias

| Área | Tecnologia | Detalhes |
| :--- | :--- | :--- |
| **Frontend** | HTML5, CSS3, Vanilla JS | Focado em performance máxima sem frameworks pesados no client-side. |
| **Design** | Layouts Responsivos, SVGs | Componentes isolados, SVGs inline gerados e ícones. |
| **Backend** | Java (Spring Boot) | Estrutura de pacotes e base pronta para receber API REST. |
| **Deploy** | Vercel | Configuração `vercel.json` nativa com Edge Caching para arquivos estáticos. |

---

## ⚙️ Como Executar Localmente

Como a aplicação foca na leveza, o setup para o lado cliente é instantâneo.

1. **Clone o repositório:**
```sh
git clone https://github.com/kauaxp77/Aprova-o-passo-a-passo.git
```
2. **Inicie o servidor estático:**
Vá até a pasta `frontend/` e inicie um servidor. Exemplo usando Python:
```sh
cd frontend
python -m http.server 8000
```
3. **Acesse no Navegador:**
Abra [http://localhost:8000](http://localhost:8000) e todo o catálogo e a landing page do projeto funcionarão em perfeição.

---

## 🌐 Deploy na Vercel

O projeto conta com a arquitetura perfeitamente alinhada para rodar **gratuitamente na Vercel**, sem a necessidade de comandos difíceis de build nem redirects agressivos.

O arquivo `vercel.json` já conta com **Edge Caching** (cache no servidor de borda) de todas as imagens, CSS e JS por até 1 ano, permitindo que a aplicação voe 🚀!

**Como efetuar o deploy em 3 passos:**
1. Logue na [Vercel](https://vercel.com/) e clique em **Add New Project**.
2. Importe este Repositório.
3. Nas configurações finais do projeto, altere o campo **Root Directory** para `frontend`.
4. Clique em **Deploy**.

*A Vercel lerá nossas configurações otimizadas automaticamente e sua plataforma estará online.*

---

## 📂 Arquitetura de Diretórios

O projeto já segue um padrão profissional de separação de responsabilidades (Frontend Isolado vs Backend).

```text
APPBANCOS/
├── frontend/                  📦 Aplicação Cliente (Vercel Root)
│   ├── index.html             # Landing Page de Conversão
│   ├── bancos.html            # Catálogo interativo
│   ├── css/                   # Estilos separados e isolados
│   ├── js/                    # Lógicas e persistência via Vanilla JS
│   ├── assets/                # Fundos com efeitos avançados
│   └── vercel.json            # Edge Network e Caching settings
├── backend/                   ⚙️ Scaffold de API (Futuro Deploy)
│   └── java/
│       ├── controller/
│       ├── repository/
│       └── service/
└── database/                  🗄️ Dados
    └── schema.sql             # Estrutura do BD relacional
```

---

<div align="center">
Feito com dedicação ☕ para construir o futuro de centenas de bancários aprovados! 
</div>
