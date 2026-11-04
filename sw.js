const CACHE = 'sticksense-v1';
const FILES = ['.', 'index.html', 'style.css', 'icon.svg', 'manifest.webmanifest', 'src/app.js', 'src/zip.js', 'src/saves.js', 'src/profiles.js', 'src/store.js'];
self.addEventListener('install', (e) => e.waitUntil(caches.open(CACHE).then((c) => c.addAll(FILES))));
self.addEventListener('activate', (e) => e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE).map((k) => caches.delete(k))))));
self.addEventListener('fetch', (e) => e.respondWith(caches.match(e.request).then((r) => r || fetch(e.request))));
