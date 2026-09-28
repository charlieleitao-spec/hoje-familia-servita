const CACHE='familia-servita-2.8.8';
const ASSETS=['./','./index.html','./manifest.webmanifest?v=2.8.8','./servita.json','./icon-192.png','./icon-512.png'];

self.addEventListener('install',e=>{
  self.skipWaiting();
  e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)));
});

self.addEventListener('activate',e=>{
  e.waitUntil(
    caches.keys()
      .then(ks=>Promise.all(ks.filter(k=>k.startsWith('familia-servita-')&&k!==CACHE).map(k=>caches.delete(k))))
      .then(()=>self.clients.claim())
  );
});

self.addEventListener('fetch',e=>{
  if(e.request.method!=='GET')return;
  e.respondWith(
    fetch(e.request).then(res=>{
      if(res.ok && new URL(e.request.url).origin===self.location.origin){
        const copy=res.clone();
        caches.open(CACHE).then(c=>c.put(e.request,copy));
      }
      return res;
    }).catch(()=>caches.match(e.request).then(cached=>cached||(e.request.mode==='navigate'?caches.match('./index.html'):Response.error())))
  );
});
