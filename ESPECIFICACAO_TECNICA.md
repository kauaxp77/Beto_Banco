# Especificação Técnica - Aprovação Passo a Passo

O projeto consiste em uma plataforma de e-commerce e vitrine acadêmica focada em concursos bancários, utilizando uma arquitetura modular que separa rigorosamente o servidor de apresentação (Frontend) da futura lógica transacional (Backend).

## 1. Visão Arquitetural

A infraestrutura foi desenhada pensando em **Escalabilidade** e **Alta Disponibilidade**. O modelo implementado segue o padrão de Client-Server moderno:
* O *Client-Side* é fortemente cacheável e não depende de Single Page Application (SPA) reativas pesadas, focando em SEO e First Contentful Paint.
* O *Server-Side* encontra-se pré-mapeado para uma arquitetura orientada a serviços REST (Java MVC).

## 2. Tecnologias Utilizadas

### 2.1 Frontend
- **Linguagens principais:** HTML5 Semântico, CSS3 e Vanilla JavaScript.
- **Design System Local:** Sistema de cores nativo em CSS Variables (Custom Properties), layout robusto baseado primariamente em CSS Grid e Flexbox, e forte emprego de Glassmorphism.
- **Tratamento de Assets:** Renderização modular de imagens com SVG inline-fallbacks (quando e se a imagem não é localizada). Otimização em Edge.

### 2.2 Backend (Camada Futura Pre-definida)
- **Linguagem:** Java.
- **Framework Opcionado:** Spring Boot (MVC, JPA, Security).
- **Estruturação:** Modelo dividido em Controller, Service, Model, Repository e Config.

### 2.3 Banco de Dados Relacional
- **Esquema Inicial (`database/schema.sql`):** Tabelas preparadas e estruturadas usando padrões DDL estritos para relacionamentos das entidades (bancos bancários, status de cursos, links).

## 3. Infraestrutura e Deploy (Vercel)

- **Continuous Deployment (CD):** Deploy automatizado integrado ao ecossistema da Vercel.
- **Vercel Config Routing (`vercel.json`):** 
  - Regras de roteamento global reescrevendo requisições `**` para o diretório `/frontend/`.
  - Controle severo de cache estático via Headers HTTP (Cache-Control: public, max-age=31536000, immutable).

## 4. Estrutura Modular (Tree Map)

- `/frontend/` -> Contém todo o ambiente de client-side (arquivos HTML roteáveis diretamente).
- `/frontend/css/` -> Arquivos de estilo segregados por contexto (bancos, cursos, header, variáveis globais).
- `/frontend/js/` -> Scripts isolados para controle de UI, com injeção dinâmica de cards e DOM handling via Vanilla JS puro.
- `/database/` -> Queries SQL puras para levantamento e "mock" da base estrutural de dados que alimentará a futura API.
