/*
 * Service worker do Beto Banco (PWA).
 *
 * Estrategia deliberadamente conservadora:
 *  - API (/api/) NUNCA passa pelo cache — dados de curso, progresso e
 *    pagamento sao sempre frescos.
 *  - Assets estaticos do proprio site: stale-while-revalidate — abre rapido
 *    (e offline, se ja visitado) e se atualiza em segundo plano.
 *  - Navegacoes: rede primeiro com fallback ao ultimo index em cache, para
 *    o app abrir mesmo sem conexao.
 */
const CACHE = 'betobanco-v1';

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(['/', '/manifest.webmanifest'])),
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((chaves) =>
        Promise.all(chaves.filter((c) => c !== CACHE).map((c) => caches.delete(c))),
      )
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  const url = new URL(req.url);

  if (req.method !== 'GET' || url.origin !== self.location.origin) return;
  if (url.pathname.startsWith('/api/')) return;

  // Navegacao: rede primeiro, fallback ao shell em cache.
  if (req.mode === 'navigate') {
    event.respondWith(
      fetch(req)
        .then((res) => {
          const copia = res.clone();
          caches.open(CACHE).then((cache) => cache.put('/', copia));
          return res;
        })
        .catch(() => caches.match('/')),
    );
    return;
  }

  // Assets: stale-while-revalidate.
  event.respondWith(
    caches.match(req).then((emCache) => {
      const daRede = fetch(req)
        .then((res) => {
          if (res.ok) {
            const copia = res.clone();
            caches.open(CACHE).then((cache) => cache.put(req, copia));
          }
          return res;
        })
        .catch(() => emCache);
      return emCache || daRede;
    }),
  );
});
