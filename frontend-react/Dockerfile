# =============================================================================
# Frontend -- Documento Mestre V4.0, secao 23: em producao o front vai para a
# Vercel. Esta imagem existe para rodar a stack inteira localmente e em staging
# do mesmo jeito, com o build de producao (nao o dev server) sendo servido.
# =============================================================================

# --- Estagio 1: build --------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /construcao

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Caminho relativo: o nginx do estagio seguinte serve o SPA e faz proxy de
# /api para o conteiner da API. Mesma origem, entao nao ha preflight de CORS --
# uma classe inteira de erro que simplesmente deixa de existir.
ENV VITE_API_URL=/api/v1
RUN npm run build

# --- Estagio 2: execucao -----------------------------------------------------
FROM nginx:1.27-alpine AS runtime

COPY --from=build /construcao/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
