const ASSETS=['./','./index.html','./manifest.webmanifest','./servita.json'];

// v3: o nome do cache não é mais um número fixo escrito à mão — ele é lido
// direto do rodapé do index.html ("Versão X.Y.Z"). Assim, publicar uma nova
// versão do app já basta para o service worker perceber a mudança e trocar
// de cache sozinho, sem precisar lembrar de editar este arquivo também.
let cacheNamePromise=null;
function getCacheName(){
  if(!cacheNamePromise){
    cacheNamePromise=fetch('./index.html',{cache:'no-store'})
      .then(res=>res.text())
      .then(text=>{
        const m=text.match(/Vers[ãa]o\s+([\d.]+[\w-]*)/i);
        return 'familia-servita-'+(m?m[1]:'dev');
      })
      .catch(()=>'familia-servita-dev');
  }
  return cacheNamePromise;
}

self.addEventListener('install',e=>{
  self.skipWaiting();
  e.waitUntil(
    getCacheName().then(name=>caches.open(name)).then(c=>c.addAll(ASSETS))
  );
});

self.addEventListener('activate',e=>{
  e.waitUntil(
    getCacheName()
      .then(name=>caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==name).map(k=>caches.delete(k)))))
      .then(()=>self.clients.claim())
  );
});

self.addEventListener('fetch',e=>{
  e.respondWith(
    fetch(e.request).then(res=>{
      const copy=res.clone();
      getCacheName().then(name=>caches.open(name)).then(c=>c.put(e.request,copy));
      return res;
    }).catch(()=>caches.match(e.request))
  );
});
