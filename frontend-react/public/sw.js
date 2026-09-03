/**
 * Service worker. Documento Mestre V4.0 -- Secao 09, Mobile.
 *
 *   "Decisao: PWA instalavel, nao app nativo -- mesmo codigo, sem fila de
 *    aprovacao de loja. Requisitos: [...] download de PDF para leitura offline."
 *
 * Estrategias, por tipo de recurso:
 *   - Navegacao: rede primeiro, com a pagina offline como reserva. Cache
 *     primeiro deixaria o aluno preso em uma versao antiga do app depois de um
 *     deploy, o que e pior do que uma espera curta.
 *   - Estatico com hash no nome (build do Vite): cache primeiro, porque o nome
 *     muda a cada build e o conteudo e imutavel.
 *   - Material baixado (PDF, mapa mental): cache separado, com nome proprio, que
 *     a limpeza de versao nao apaga. E o unico conteudo que o aluno espera
 *     encontrar offline.
 *
 * O que NUNCA e cacheado, de proposito:
 *   - Qualquer resposta de /api/ -- inclui matricula, progresso e dado pessoal,
 *     e um cache do navegador sobreviveria ao logout.
 *   - Video. Secao 10 exige player com dominio restrito e link expiravel;
 *     guardar o video em cache anularia as duas coisas.
 */

const VERSAO = 'v4.0.0';
const CACHE_APP = `app-${VERSAO}`;
const CACHE_MATERIAIS = 'materiais-do-aluno'; // sem versao: sobrevive a deploy
const PAGINA_OFFLINE = '/offline.html';

const ESSENCIAIS = ['/', PAGINA_OFFLINE, '/manifest.webmanifest'];

self.addEventListener('install', (evento) => {
  evento.waitUntil(
    caches.open(CACHE_APP).then((cache) => cache.addAll(ESSENCIAIS)).then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (evento) => {
  evento.waitUntil(
    caches
      .keys()
      .then((nomes) =>
        Promise.all(
          nomes
            .filter((nome) => nome.startsWith('app-') && nome !== CACHE_APP)
            .map((nome) => caches.delete(nome)),
        ),
      )
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (evento) => {
  const { request } = evento;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);

  // Nunca cachear API nem video.
  if (url.pathname.startsWith('/api/') || url.hostname.includes('pandavideo') || url.hostname.includes('vimeo')) {
    return;
  }

  // Material do aluno: cache primeiro, para abrir offline.
  if (url.pathname.startsWith('/materiais/')) {
    evento.respondWith(
      caches.open(CACHE_MATERIAIS).then(async (cache) => {
        const guardado = await cache.match(request);
        if (guardado) return guardado;
        const resposta = await fetch(request);
        if (resposta.ok) cache.put(request, resposta.clone());
        return resposta;
      }),
    );
    return;
  }

  // Navegacao: rede primeiro, offline como reserva.
  if (request.mode === 'navigate') {
    evento.respondWith(
      fetch(request).catch(() => caches.match(PAGINA_OFFLINE).then((r) => r || Response.error())),
    );
    return;
  }

  // Estatico do build: cache primeiro.
  if (url.origin === self.location.origin && /\.(js|css|woff2?|svg|png|jpg|webp)$/.test(url.pathname)) {
    evento.respondWith(
      caches.open(CACHE_APP).then(async (cache) => {
        const guardado = await cache.match(request);
        if (guardado) return guardado;
        try {
          const resposta = await fetch(request);
          if (resposta.ok) cache.put(request, resposta.clone());
          return resposta;
        } catch (erro) {
          return guardado || Response.error();
        }
      }),
    );
  }
});

/**
 * Secao 22 -- o titular exclui a conta ou revoga consentimento: o app manda
 * limpar tudo que ficou no dispositivo, inclusive os materiais baixados.
 */
self.addEventListener('message', (evento) => {
  if (evento.data?.tipo === 'limpar-dados-do-titular') {
    evento.waitUntil(caches.keys().then((nomes) => Promise.all(nomes.map((n) => caches.delete(n)))));
  }
});
