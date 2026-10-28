const CACHE='familia-servita-2.9.35-daily-v1';
const ASSETS=['./','./index.html','./biblioteca-servita.html','./data/biblioteca-servita-catalogo.json','./data/pdfs/biblioteca/constituicoes-regra-diretorio-osm-2015.pdf','./data/pdfs/biblioteca/ratio-institutionis-osm-2015.pdf','./data/pdfs/biblioteca/regra-vida-ordem-secular-1995.pdf','./causas-beatos.html','./missas-proprias.html','./manifest.webmanifest?v=2.9.35','./servita.json','./santoral.json','./icon-192.png?v=2.9.35','./icon-512.png?v=2.9.35','./favicon-32.png?v=2.9.35','./favicon-64.png?v=2.9.35'];

self.addEventListener('install',e=>{
  e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)).then(()=>self.skipWaiting()));
});

self.addEventListener('activate',e=>{
  e.waitUntil(
    caches.keys()
      .then(ks=>Promise.all(ks.filter(k=>k.startsWith('familia-servita-')&&k!==CACHE).map(k=>caches.delete(k))))
      .then(()=>self.clients.claim())
  );
});

self.addEventListener('fetch',e=>{
  if(e.request.method!=='GET'||new URL(e.request.url).origin!==self.location.origin)return;
  e.respondWith((async()=>{
    const controller=new AbortController();
    const timeout=setTimeout(()=>controller.abort(),8000);
    const cache=await caches.open(CACHE);
    const cached=await cache.match(e.request)
      ||(e.request.mode==='navigate'?await cache.match(e.request,{ignoreSearch:true}):null);
    try{
      const response=await fetch(e.request,{signal:controller.signal});
      if(response.ok)e.waitUntil(cache.put(e.request,response.clone()).catch(()=>{}));
      if(!response.ok&&cached)return cached;
      return response;
    }catch(error){
      return cached||Response.error();
    }finally{clearTimeout(timeout)}
  })());
});

