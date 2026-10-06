const CACHE='sitio-chips-v2-5';const FILES=['/index.html','/app.css','/app.js','/excel.js','/manifest.json','/icon.svg'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(FILES)))});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('sitio-chips-')&&k!==CACHE).map(k=>caches.delete(k)))))});
self.addEventListener('fetch',e=>{const url=new URL(e.request.url);if(url.origin!==self.location.origin||url.pathname.startsWith('/api/')||e.request.method!=='GET')return;if(e.request.mode==='navigate'){e.respondWith(fetch(e.request).catch(()=>caches.match('/index.html')));return}if(FILES.includes(url.pathname))e.respondWith(caches.match(e.request).then(hit=>hit||fetch(e.request)))});
