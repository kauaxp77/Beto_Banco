# Beto Banco — Melhorias de experiência do usuário (roadmap UX)

Prompt pronto para orientar as próximas sessões de desenvolvimento.
Implementar em ordem, um item por vez, com testes e commit próprios.

## Contexto do projeto (leia antes de codar)
- Backend: Java 21 + Spring Boot em `backend/` (módulos comunicam-se apenas
  pelos pacotes `api/` — regra ArchUnit; identidade sempre do token JWT;
  controllers devolvem DTO, nunca @Entity).
- Frontend: React 19 + TypeScript + Vite em `frontend/` (TanStack Query,
  CSS próprio com tokens grafite/dourado em `src/styles/tokens.css`).
- Banco: PostgreSQL (Supabase) com Flyway em
  `backend/src/main/resources/db/migration` (próxima é V11).
- Acesso a conteúdo SEMPRE validado por entitlement no servidor; 404 idêntico
  para recurso alheio e inexistente.
- Toda melhoria: migração + endpoint testado (Testcontainers) + UI testada
  (Vitest) + build/lint limpos antes de commitar.

## Prioridade 1 — Retenção diária do aluno
1. Retomar o vídeo no ponto exato (posição salva por aluno/aula, debounce
   ~10s; barra de "assistido parcialmente" na lista).
2. Notificações in-app (sininho): resposta do professor, novo aviso, nova
   aula, certificado disponível; contador de não lidas.
3. Caderno de erros: questões erradas por matéria; "refazer só as que
   errei"; sai do caderno após 2 acertos seguidos.
4. Anotações do aluno por aula (autosave) + página "Minhas anotações".

## Prioridade 2 — Estudo dirigido
5. Metas semanais (aulas/questões) integradas ao streak.
6. Busca global em todos os cursos (atalho "/").
7. Estatísticas por matéria com "ponto fraco da semana".
8. Favoritar aulas + página "Salvas para revisar".

## Prioridade 3 — Player e conforto
9. Velocidade do player lembrada, autoplay da próxima aula, "continuar
   assistindo" na home.
10. Modo áudio no PWA (Media Session API).
11. Acessibilidade: teclado completo, foco visível, contraste AA.

## Prioridade 4 — Prova social e conquistas
12. Conquistas/badges com toast comemorativo.
13. Compartilhar certificado (LinkedIn + Open Graph na página pública).
14. Onboarding de primeiro login (tour de 4 passos, pulável).

## Prioridade 5 — Professor a serviço do aluno
15. Conteúdo programado (drip) com contagem regressiva.
16. Reordenar módulos/aulas por arrastar.
17. Duplicar curso.
18. Avisos com texto rico (sanitizado).

## Regras de entrega
- Sem dependência nova sem justificativa; preferir soluções nativas.
- Interface em pt-BR, identidade grafite + dourado.
- Decisão de produto pendente → propor a opção mais simples e seguir.
