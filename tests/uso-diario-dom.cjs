const {JSDOM,VirtualConsole}=require('jsdom');
const fs=require('node:fs');const assert=require('node:assert/strict');
const root=require('node:path').join(__dirname,'../');
const html=fs.readFileSync(root+'index.html','utf8');const records=JSON.parse(fs.readFileSync(root+'servita.json','utf8'));
const errors=[];
async function open({saved=null,url='https://charlieleitao-spec.github.io/hoje-familia-servita/',failure=false,storageDenied=false}={}){
 const vc=new VirtualConsole();vc.on('jsdomError',e=>errors.push(e.message));
 const dom=new JSDOM(html,{url,runScripts:'dangerously',pretendToBeVisual:true,virtualConsole:vc,beforeParse(w){
 w.matchMedia=()=>({matches:true});w.scrollTo=()=>{};w.HTMLElement.prototype.scrollIntoView=()=>{};
 w.fetch=async()=>{if(failure)throw Error('rede');return {ok:true,json:async()=>records}};
 if(saved)w.localStorage.setItem('familia-servita-ultima-leitura-v1',JSON.stringify(saved));
 if(storageDenied)Object.defineProperty(w,'localStorage',{get(){throw Error('negado')}});
 }});await new Promise(r=>setImmediate(r));return dom;
}
(async()=>{
 let dom=await open({url:'https://charlieleitao-spec.github.io/hoje-familia-servita/?data=2026-10-27'}),w=dom.window,d=w.document;
 assert.equal(d.querySelector('#memoryLabel').textContent,'Espiritualidade servita');assert.match(d.querySelector('#memEditorialNote').textContent,/devocional/);assert.equal(d.querySelector('#title').textContent,'Liturgia do dia');
 d.querySelector('#calendarBtn').click();assert.equal(d.querySelector('#calendarCard').hidden,false);assert.equal(d.querySelectorAll('#monthDays button').length,31);
 d.querySelector('#nextMonth').click();assert.match(d.querySelector('#monthTitle').textContent,/novembro/);assert.equal(d.querySelectorAll('#monthDays button').length,30);
 d.querySelector('#monthDays button').click();assert.equal(d.querySelector('#picker').value,'2026-11-01');assert.equal(d.querySelector('#calendarCard').hidden,true);
 let shared;Object.defineProperty(w.navigator,'share',{value:async data=>{shared=data},configurable:true});await w.shareCurrentDay();assert.match(shared.url,/data=2026-11-01/);
 Object.defineProperty(w.navigator,'share',{value:undefined,configurable:true});await w.shareCurrentDay();assert.equal(d.querySelector('#shareCopy').hidden,false);assert.match(d.querySelector('#shareCopy').value,/data=2026-11-01/);
 Object.defineProperty(w.navigator,'clipboard',{value:{writeText:async s=>{shared=s}}});await w.shareCurrentDay();assert.equal(d.querySelector('#shareStatus').textContent,'Link desta data copiado.');
 Object.defineProperty(w.navigator,'share',{value:async()=>{const e=new Error();e.name='AbortError';throw e},configurable:true});await w.shareCurrentDay();assert.equal(d.querySelector('#shareCopy').hidden,true);
 dom.window.close();
 dom=await open({url:'https://charlieleitao-spec.github.io/hoje-familia-servita/?data=2026-08-23'});w=dom.window;d=w.document;
 assert.match(d.querySelector('#title').textContent,/Filipe/);assert.equal(d.querySelector('#memoryLabel').textContent,'Memória servita');assert.match(d.querySelector('#formationLink').href,/sao-filipe-benizi/);
 d.querySelector('#materialBtn').click();assert.equal(d.querySelector('#reader').hidden,false);let buttons=d.querySelectorAll('#hours button');assert.ok(buttons.length>1);buttons[buttons.length-1].click();
 const saved=JSON.parse(w.localStorage.getItem('familia-servita-ultima-leitura-v1'));assert.equal(saved.hour,buttons[buttons.length-1].dataset.hourKey);assert.equal(saved.date,'2026-08-23');
 dom.window.close();dom=await open({saved});w=dom.window;d=w.document;assert.equal(d.querySelector('#resumeCard').hidden,false);d.querySelector('#resumeBtn').click();assert.equal(d.querySelector('#picker').value,saved.date);assert.equal(d.querySelector('#hours button.active').dataset.hourKey,saved.hour);assert.equal(d.querySelector('#resumeCard').hidden,true);
 dom.window.close();dom=await open({saved:{date:'2026-08-23',hour:'missing'}});w=dom.window;d=w.document;d.querySelector('#resumeBtn').click();assert.ok(d.querySelector('#hours button.active'));d.querySelector('#dismissResume').click();assert.equal(w.localStorage.getItem('familia-servita-ultima-leitura-v1'),null);dom.window.close();
 dom=await open({failure:true});d=dom.window.document;assert.equal(d.querySelector('#memoryLabel').textContent,'Memória servita');assert.match(d.querySelector('#bio').textContent,/carregar/);assert.equal(d.querySelector('#shareDay').disabled,true);dom.window.close();
 dom=await open({storageDenied:true});d=dom.window.document;d.querySelector('#next').click();assert.equal(d.querySelector('#resumeCard').hidden,true);dom.window.close();
 dom=await open({url:'https://charlieleitao-spec.github.io/hoje-familia-servita/?data=2026-11-16'});w=dom.window;d=w.document;
 d.querySelector('#materialBtn').click();d.querySelector('[data-hour-key="laudes"]').click();
 const stanza=[...d.querySelectorAll('.hymn-stanza')].at(-1);
 assert.deepEqual([...stanza.children].map(e=>e.textContent),['Honra e louvor ao Deus trino,','como no céu aqui na terra demos:','erguendo a nossa voz no mesmo hino,','sem fim louvemos. Amém!']);
 assert.ok(d.querySelector('#content .lit-warning'),'Laudes 16/11 precisa sinalizar salmodia ausente');
 let checked=0;
 for(const item of Object.values(records))for(const hour of Object.values(item.material?.horas||{})){
  const text=hour.texto||'';
  const ants=(text.match(/^Ant\.\s*\d+/gmi)||[]).length;
  const titles=text.split('\n').filter(line=>/^(Salmo|Cântico)\s+/i.test(line));
  if(ants>=3&&titles.length&&titles.every(line=>/^Cântico evangélico/i.test(line))){
   d.querySelector('#content').replaceChildren();w.renderLiturgicalHour(hour.titulo,text);
   assert.ok(d.querySelector('#content .lit-warning'),item.titulo+' '+hour.titulo);checked++;
  }
 }
 assert.equal(checked,11,'Cobrir as onze horas afetadas da base real');
 const complete='SALMODIA\nAnt. 1 Primeira\nSalmo 62\nMeu Deus, sois vós.\nAnt. 2 Segunda\nAnt. 3 Terceira\nCântico evangélico (Benedictus)';
 d.querySelector('#content').replaceChildren();w.renderLiturgicalHour('Laudes',complete);
 assert.equal(d.querySelector('#content .lit-warning'),null,'Salmo presente não deve gerar aviso');
 dom.window.close();
 assert.deepEqual(errors,[]);console.log('PASS: uso diário; estrofe completa e ordenada de Laudes 16/11; aviso nas onze horas afetadas; salmodia com salmo presente sem falso aviso; sem erros de execução.');
})().catch(e=>{console.error(e);process.exit(1)});
