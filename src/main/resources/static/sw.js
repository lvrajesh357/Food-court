const CACHE_NAME = 'foodcourt-static-v3';
const STATIC_ASSETS = [
  '/css/theme-toggle.css',
  '/js/theme-toggle.js',
  '/images/icon-192.png',
  '/images/icon-512.png',
  '/manifest.json'
];

self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(CACHE_NAME).then(function (cache) {
      return cache.addAll(STATIC_ASSETS);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(
        keys.filter(function (k) { return k !== CACHE_NAME; })
            .map(function (k) { return caches.delete(k); })
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', function (event) {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);

  // CSS / JS: serve the cached copy instantly, but ALWAYS refresh it in the background
  // (stale-while-revalidate) so a new deploy reaches phones after one reload instead of never.
  if (url.pathname.startsWith('/css/') || url.pathname.startsWith('/js/')) {
    event.respondWith(
      caches.open(CACHE_NAME).then(function (cache) {
        return cache.match(req).then(function (cached) {
          const network = fetch(req).then(function (res) {
            if (res && res.ok) { cache.put(req, res.clone()); }   // never cache 404/500 pages
            return res;
          }).catch(function () { return cached; });
          return cached || network;
        });
      })
    );
    return;
  }

  // Images rarely change: cache-first for fast repeat loads (successful responses only).
  if (url.pathname.startsWith('/images/')) {
    event.respondWith(
      caches.match(req).then(function (cached) {
        return cached || fetch(req).then(function (res) {
          if (res && res.ok) {
            const resClone = res.clone();
            caches.open(CACHE_NAME).then(function (cache) { cache.put(req, resClone); });
          }
          return res;
        });
      })
    );
    return;
  }

  // Everything else (server-rendered pages, cart/session-dependent content):
  // network-first, since this app relies on live session state and must
  // never show stale cached HTML. Only falls back to a simple offline
  // notice if there's genuinely no connection.
  event.respondWith(
    fetch(req).catch(function () {
      return new Response(
        '<h2>You are offline</h2><p>FoodCourt needs an internet connection to load this page. Please reconnect and try again.</p>',
        { headers: { 'Content-Type': 'text/html' } }
      );
    })
  );
});
