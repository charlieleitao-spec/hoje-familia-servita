const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.join(__dirname,'..');
const html = fs.readFileSync(path.join(root,'../somos-servos.html'),'utf8');
const source = html.match(/<script>([\s\S]*?)<\/script>/)[1]
  .replace("const API_URL = '';", "const API_URL = 'https://script.google.com/macros/s/TEST/exec';");
const KEY = 'familia-servita:somos-servos:v1';
const feed = () => ({schemaVersion:1,ok:true,stale:false,generatedAt:'2026-10-22T10:00:00Z',
  posts:[{title:'Nossa Senhora',url:'https://somosservos.blogspot.com/2026/10/maria.html',published:'2026-10-22T09:00:00Z',thumbnail:null}],materials:[]});
class Element {
  constructor(){this.children=[];this.textContent='';this.events={};}
  append(x){this.children.push(x);}
  replaceChildren(){this.children=[];}
  addEventListener(name,fn){this.events[name]=fn;}
  remove(){this.removed=true;}
}
async function client({data=feed(),storage=new Map(),fail=false,quota=false,raw,timeout=false}={}) {
  const elements = new Map();
  const context = {URL,Date,JSON,Number,AbortController,
    localStorage:{getItem:k=>storage.get(k)||null,setItem:(k,v)=>{if(quota)throw Error('Quota');storage.set(k,v);}},
    document:{getElementById:id=>{if(!elements.has(id))elements.set(id,new Element());return elements.get(id);},createElement:()=>new Element()},
    window:{addEventListener(){}},
    setTimeout:fn=>{if(timeout)queueMicrotask(fn);return 1;},clearTimeout(){},
    fetch:async(_,opts)=>{
      if(timeout) { await Promise.resolve(); if(opts.signal.aborted)throw Error('Aborted'); }
      if(fail)throw Error('Network');
      return {ok:true,text:async()=>raw===undefined?JSON.stringify(data):raw};
    }};
  vm.createContext(context);vm.runInContext(source,context);
  await new Promise(resolve=>setImmediate(resolve));
  return {context,elements,storage,run:code=>vm.runInContext(code,context)};
}
test('online salva dados e reabertura offline usa o mesmo cache',async()=>{
  const online=await client();assert.ok(online.storage.has(KEY));
  const offline=await client({storage:online.storage,fail:true});
  assert.equal(offline.elements.get('posts').children.length,1);
  assert.match(offline.elements.get('status').textContent,/dados salvos/);
});
test('primeira abertura offline oferece recuperação sem tela vazia',async()=>{
  const c=await client({fail:true});assert.match(c.elements.get('status').textContent,/primeira consulta/);
  assert.equal(c.elements.get('materials').children.length,4);
});
test('cache corrompido é ignorado e resposta online o substitui',async()=>{
  const c=await client({storage:new Map([[KEY,'invalid']])});assert.doesNotThrow(()=>JSON.parse(c.storage.get(KEY)));
});
test('HTML de login não destrói cache anterior',async()=>{
  const storage=new Map([[KEY,JSON.stringify(feed())]]);const old=storage.get(KEY);
  const c=await client({storage,raw:'<html>Login</html>'});assert.equal(storage.get(KEY),old);
  assert.match(c.elements.get('status').textContent,/dados salvos/);
});
test('resposta com URL perigosa é recusada sem sobrescrever cache',async()=>{
  const storage=new Map([[KEY,JSON.stringify(feed())]]);const old=storage.get(KEY);
  const data=feed();data.posts[0].url='javascript:alert(1)';
  await client({storage,data});assert.equal(storage.get(KEY),old);
});
test('título com marcação é exibido como texto',async()=>{
  const data=feed();data.posts[0].title='<img src=x onerror=alert(1)>';
  const c=await client({data});assert.equal(c.elements.get('posts').children[0].children[0].textContent,data.posts[0].title);
});
test('quota esgotada não impede renderização e avisa falta de persistência',async()=>{
  const c=await client({quota:true});assert.equal(c.elements.get('posts').children.length,1);
  assert.match(c.elements.get('status').textContent,/Não foi possível salvar/);
});
test('timeout mantém cache e reabilita botão',async()=>{
  const c=await client({timeout:true,storage:new Map([[KEY,JSON.stringify(feed())]])});
  assert.match(c.elements.get('status').textContent,/dados salvos/);
  assert.equal(c.elements.get('refresh').disabled,false);
});
test('resposta antiga não regride o cache',async()=>{
  const storage=new Map([[KEY,JSON.stringify(feed())]]);const data=feed();data.generatedAt='2026-10-21T00:00:00Z';
  await client({storage,data});assert.equal(JSON.parse(storage.get(KEY)).generatedAt,feed().generatedAt);
});
test('lista vazia válida substitui posts removidos; miniatura não é obrigatória',async()=>{
  const data=feed();data.posts=[];const c=await client({data,storage:new Map([[KEY,JSON.stringify(feed())]])});
  assert.equal(c.elements.get('posts').children.length,0);assert.equal(c.elements.get('empty').hidden,false);
});
test('erro lógico ok:false preserva cache mesmo com HTTP 200',async()=>{
  const storage=new Map([[KEY,JSON.stringify(feed())]]);const old=storage.get(KEY);
  await client({storage,data:{ok:false,error:{code:'FEED_UNAVAILABLE'}}});assert.equal(storage.get(KEY),old);
});

const gasSource=fs.readFileSync(path.join(root,'gas/Code.gs'),'utf8');
function gas({responses=[],props={BLOGGER_API_KEY:'secret'},busy=false}={}) {
  const cache=new Map();const calls=[];let released=0;
  const context={console,Date,JSON,Math,encodeURIComponent,isNaN,
    CacheService:{getScriptCache:()=>({get:k=>cache.get(k)||null,put:(k,v)=>cache.set(k,v)})},
    LockService:{getScriptLock:()=>({tryLock:()=>!busy,releaseLock:()=>released++})},
    PropertiesService:{getScriptProperties:()=>({getProperty:k=>props[k],setProperty:(k,v)=>props[k]=v})},
    Utilities:{sleep(){},newBlob:s=>({getBytes:()=>Buffer.from(s)})},
    ContentService:{MimeType:{JSON:'application/json'},createTextOutput:text=>({text,setMimeType(){return this;}})},
    UrlFetchApp:{fetch:(url)=>{calls.push(url);const response=responses.shift();if(!response)throw Error('NETWORK secret');return {getResponseCode:()=>response.status,getContentText:()=>JSON.stringify(response.body)};}}
  };
  vm.createContext(context);vm.runInContext(gasSource,context);
  return {run:code=>vm.runInContext(code,context),cache,calls,props,released:()=>released};
}
const blogResponse={status:200,body:{id:'1234567890123456789'}};
const postsResponse={status:200,body:{items:[{id:'1',title:'Maria',url:'http://somosservos.blogspot.com/p.html',published:'2026-10-22T09:00:00Z'}]}};
test('GAS resolve blog, consulta posts com imagens e reutiliza cache',()=>{
  const g=gas({responses:[blogResponse,postsResponse]});
  const first=JSON.parse(g.run('doGet().text'));const second=JSON.parse(g.run('doGet().text'));
  assert.equal(first.ok,true);assert.equal(first.posts[0].thumbnail,null);
  assert.match(first.posts[0].url,/^https:/);assert.equal(second.generatedAt,first.generatedAt);
  assert.equal(g.calls.length,2);assert.match(g.calls[1],/fetchImages=true/);assert.match(g.calls[1],/fetchBodies=false/);
  assert.equal(g.props.BLOGGER_BLOG_ID,'1234567890123456789');assert.equal(g.released(),1);
});
test('GAS retenta 429 e retorna sucesso',()=>{
  const g=gas({responses:[blogResponse,{status:429},postsResponse]});
  assert.equal(JSON.parse(g.run('doGet().text')).ok,true);assert.equal(g.calls.length,3);
});
test('GAS usa stale em falha e mantém data original',()=>{
  const g=gas();g.cache.set('somos-servos:v1:posts:stale',JSON.stringify(feed()));
  const result=JSON.parse(g.run('doGet().text'));assert.equal(result.stale,true);
  assert.equal(result.generatedAt,feed().generatedAt);assert.equal(g.released(),1);
});
test('GAS erro público não vaza chave nem URL interna',()=>{
  const g=gas({props:{}});const result=JSON.parse(g.run('doGet().text'));
  assert.equal(result.ok,false);assert.doesNotMatch(JSON.stringify(result),/secret|googleapis|CONFIG_REQUIRED/);
});
test('GAS não chama API se outra execução detém lock',()=>{
  const g=gas({busy:true});assert.equal(JSON.parse(g.run('doGet().text')).ok,false);assert.equal(g.calls.length,0);
});
