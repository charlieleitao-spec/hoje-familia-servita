#!/usr/bin/env node
// Verifica a integridade estrutural da Biblioteca Servita antes da publicação.
const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve(__dirname,'..');
const web=fs.existsSync(path.join(root,'www','biblioteca-servita.html'))?path.join(root,'www'):root;
const catalogPath=path.join(web,'data','biblioteca-servita-catalogo.json');
const catalog=JSON.parse(fs.readFileSync(catalogPath,'utf8'));
const ids=new Set();
if(catalog.documents.length!==5)throw Error('Esperados cinco documentos');
for(const d of catalog.documents){
  if(!d.id||!d.title||ids.has(d.id))throw Error('Documento inválido ou duplicado');
  ids.add(d.id);
  if(d.offline_asset){
    if(/^(https?:)?\/\//.test(d.offline_asset)||d.offline_asset.includes('..'))throw Error('Caminho offline inseguro: '+d.id);
    const asset=path.resolve(web,d.offline_asset);
    if(!asset.startsWith(web+path.sep)||!fs.existsSync(asset))throw Error('PDF offline ausente: '+d.id);
    if(fs.readFileSync(asset).subarray(0,5).toString()!=='%PDF-')throw Error('Arquivo não é PDF: '+d.id);
  }
}
const sw=fs.readFileSync(path.join(web,'sw.js'),'utf8');
for(const asset of ['biblioteca-servita.html','data/biblioteca-servita-catalogo.json']){
 if(!sw.includes(asset))throw Error('Falta no cache: '+asset);
}
console.log('Biblioteca Servita: catálogo, arquivos locais declarados e cache OK.');
