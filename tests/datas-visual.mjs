import {chromium} from 'playwright';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import {spawn} from 'node:child_process';
const output='validation-datas';await fs.mkdir(output,{recursive:true});
const server=spawn('python3',['-m','http.server','18775','--bind','127.0.0.1'],{stdio:'ignore'});
let browser;const results=[];
try{
 for(let n=0;n<80;n++){try{if((await fetch('http://127.0.0.1:18775/')).ok)break;}catch{}await new Promise(r=>setTimeout(r,50));}
 browser=await chromium.launch({executablePath:process.env.CHROME_BIN||undefined,headless:true,args:['--no-sandbox']});
 for(const width of [320,390,768]){
  const context=await browser.newContext({viewport:{width,height:844},hasTouch:true,isMobile:true});
  const page=await context.newPage();await page.goto('http://127.0.0.1:18775/?data=2026-11-16');
  await page.waitForFunction(()=>Object.keys(records).length>=30&&!loadError);
  await page.evaluate(()=>navigator.serviceWorker.ready);
  await page.getByRole('button',{name:'Guardar esta data',exact:true}).tap();
  assert.equal(await page.locator('#savedDatesList li').count(),1);
  assert.equal(await page.locator('#monthDays .calendar-saved').count(),1);
  const geometry=await page.locator('#savedDatesCard').evaluate(el=>{
   const r=el.getBoundingClientRect();return {overflow:document.documentElement.scrollWidth>innerWidth+1,buttons:[...el.querySelectorAll('button')].map(b=>{const v=b.getBoundingClientRect();return {height:v.height,left:v.left,right:v.right};}),card:{left:r.left,right:r.right}};
  });
  assert.equal(geometry.overflow,false);assert.ok(geometry.buttons.every(b=>b.height>=44&&b.left>=geometry.card.left-1&&b.right<=geometry.card.right+1));
  await page.locator('#savedDatesCard').scrollIntoViewIfNeeded();await page.screenshot({path:output+'/'+width+'.png',animations:'disabled'});
  await page.reload();await page.waitForFunction(()=>Object.keys(records).length>=30&&!loadError);
  await context.setOffline(true);await page.reload();await page.waitForFunction(()=>Object.keys(records).length>=30&&!loadError);
  await page.getByRole('button',{name:'Calendário',exact:true}).tap();assert.equal(await page.locator('#savedDatesList li').count(),1);
  await page.locator('#savedDatesList button').first().tap();assert.equal(await page.locator('#picker').inputValue(),'2026-11-16');assert.equal(await page.locator('#calendarCard').isVisible(),false);
  await page.getByRole('button',{name:'Remover esta data de Minhas datas',exact:true}).tap();assert.equal(await page.evaluate(()=>localStorage.getItem('familia-servita-datas-pessoais-v1')),'[]');
  await page.reload();await page.waitForFunction(()=>Object.keys(records).length>=30&&!loadError);assert.equal(await page.locator('#saveDay').getAttribute('aria-pressed'),'false');
  results.push({width,...geometry,offlineReload:true,offlineOpen:true,offlineRemoval:true});await context.close();
 }
 await fs.writeFile(output+'/results.json',JSON.stringify({browser:browser.version(),results},null,2));console.log('PASS: datas guardadas em 320/390/768 px; reabertura, navegação e remoção offline com Service Worker real.');
}catch(error){await fs.writeFile(output+'/failure.txt',String(error.stack||error));throw error;}
finally{await browser?.close();server.kill();}
