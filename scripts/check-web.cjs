const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
for(const name of ['index.html','causas-beatos.html','missas-proprias.html','biblioteca-servita.html']){
 const html=fs.readFileSync(name,'utf8');
 for(const [,script] of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi))new vm.Script(script,{filename:name});
}
const manifest=JSON.parse(fs.readFileSync('manifest.webmanifest','utf8'));
assert.equal(manifest.id,'./app-v2.9.13','Preservar identidade das instalações PWA existentes');
const data=JSON.parse(fs.readFileSync('servita.json','utf8'));
const santoral=JSON.parse(fs.readFileSync('santoral.json','utf8'));
assert.ok(Object.keys(data).length>=30);
assert.ok(Array.isArray(santoral)&&santoral.length>=30);
assert.ok(santoral.some(x=>x.categoria==='servo_de_deus'&&x.prayer));
const sw=fs.readFileSync('sw.js','utf8');
new vm.Script(sw,{filename:'sw.js'});
const source=sw.slice(0,sw.indexOf("self.addEventListener"));
const assets=vm.runInNewContext(source+';ASSETS');
for(const asset of assets)assert.ok(fs.existsSync(asset.split('?')[0]),'Recurso offline ausente: '+asset);
console.log('JavaScript, JSON, identidade PWA e recursos offline válidos.');
