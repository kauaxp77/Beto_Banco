# Deploy no Vercel - Guia RÃ¡pido

## ðŸ“‹ Requisitos
- Conta no [Vercel](https://vercel.com)
- RepositÃ³rio GitHub do projeto (jÃ¡ configurado)

## ðŸš€ OpÃ§Ã£o 1: Deploy via Dashboard Vercel (Recomendado)

### Passo 1: Conectar ao Vercel
1. Acesse [https://vercel.com](https://vercel.com)
2. Clique em **"New Project"**
3. Clique em **"Import Git Repository"**
4. Selecione sua conta GitHub (`Felpzcvl`)
5. Busque por `aprova-o-passo-a-passo1.0` ou `aprovacao-passo-passo2.0`
6. Clique em **"Import"**

### Passo 2: Configurar Projeto
- **Project Name**: `aprova-passo-passo` (ou o que preferir)
- **Framework Preset**: `Other` (Ã© um projeto estÃ¡tico)
- **Root Directory**: deixe em branco (padrÃ£o Ã© raiz)
- **Build Command**: deixe vazio
- **Output Directory**: `frontend`
- **Environment Variables**: nenhuma necessÃ¡ria

### Passo 3: Deploy
- Clique em **"Deploy"**
- Aguarde 1-2 minutos
- Seu site estarÃ¡ disponÃ­vel em `https://[seu-projeto].vercel.app`

---

## ðŸš€ OpÃ§Ã£o 2: Deploy via CLI (Terminal)

### Passo 1: Instalar Vercel CLI
```bash
npm install -g vercel
```

### Passo 2: Fazer Login
```bash
vercel login
```
Siga as instruÃ§Ãµes no navegador para autenticar.

### Passo 3: Fazer Deploy
```bash
cd "C:\Users\felli\Downloads\APPBANCOS"
vercel --prod
```

Responda Ã s perguntas:
- **Project name?** â†’ `aprova-passo-passo` (ou seu nome preferido)
- **Which scope?** â†’ Escolha sua conta
- **Linked to existing project?** â†’ `N` (primeira vez)
- **Directory?** â†’ `./frontend`

O projeto serÃ¡ deployado em produÃ§Ã£o automaticamente.

---

## ðŸ“ Estrutura de Arquivos
O `vercel.json` jÃ¡ estÃ¡ configurado para:
- âœ… Servir arquivos estÃ¡ticos da pasta `frontend/`
- âœ… Limpar URLs (sem `.html` na URL)
- âœ… Cache de assets (imagens, CSS, JS)
- âœ… Redirecionamentos automÃ¡ticos para pÃ¡ginas HTML

---

## ðŸ”— URLs Depois do Deploy

ApÃ³s o deploy, suas pÃ¡ginas estarÃ£o disponÃ­veis em:

| PÃ¡gina | URL |
|--------|-----|
| Landing | `https://[projeto].vercel.app/` |
| Bancos | `https://[projeto].vercel.app/bancos` |
| Cursos | `https://[projeto].vercel.app/cursos` |
| Perfil | `https://[projeto].vercel.app/perfil` |

---

## âœ… Verificar Deploy

1. Acesse o dashboard do Vercel: [https://vercel.com/dashboard](https://vercel.com/dashboard)
2. Clique no seu projeto
3. Veja a URL gerada
4. Clique em **"Visit"** para testar

---

## ðŸ”„ Deploy AutomÃ¡tico

Toda vez que vocÃª fazer `git push` para o repositÃ³rio GitHub:
- Vercel detecta automaticamente
- Faz novo build e deploy
- Seu site atualiza em ~1 minuto

---

## ðŸ“ Notas Importantes

- âœ… Favicon funciona automaticamente
- âœ… Imagens de fundo com `background-attachment: fixed` funcionam
- âœ… Todos os links relativos funcionam
- âœ… WhatsApp links funcionam (href com `wa.me/`)

---

## â“ Problemas Comuns

### "Domain already taken"
Use um nome diferente para o projeto

### "Build failed"
O `vercel.json` estÃ¡ configurado para nÃ£o fazer build (Ã© estÃ¡tico)

### "Pages nÃ£o carregam"
Verifique se todos os arquivos foram commitados no Git e fizeram push

---

## ðŸ“ž PrÃ³ximos Passos

1. Escolha OpÃ§Ã£o 1 ou 2 acima
2. Complete o deploy
3. Teste no navegador
4. Compartilhe a URL do Vercel com seu pÃºblico!

ðŸŽ‰ Seu site estarÃ¡ online e acessÃ­vel globalmente!
