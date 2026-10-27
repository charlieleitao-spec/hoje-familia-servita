const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const helpers=html.split('// BEGIN DAILY HELPERS')[1].split('let resumeCandidate=')[0];
function context(saved=null){
 const c=vm.createContext({URL,localStorage:{getItem:()=>saved},fmt:new Intl.DateTimeFormat('pt-BR',{dateStyle:'long'}),parseDate:s=>/^\d{4}-\d{2}-\d{2}$/.test(s||'')&&!isNaN(new Date(s+'T12:00:00'))?new Date(s+'T12:00:00'):null,displayCelebrationTitle:x=>x.titulo});
 vm.runInContext(helpers,c);return c;
}
test('reflexões usam o dia civil e alternam os três temas',()=>{
 const c=context();
 assert.equal(vm.runInContext("dailyReflection(new Date(2026,9,27,0)).title===dailyReflection(new Date(2026,9,27,23)).title",c),true);
 assert.equal(vm.runInContext("new Set([27,28,29].map(d=>dailyReflection(new Date(2026,9,d)).title)).size",c),3);
 assert.equal(vm.runInContext("dailyReflection(new Date(2026,9,27)).prayer.length>20",c),true);
});
test('calendário trata ano bissexto, virada do ano e limites do seletor',()=>{
 const c=context();
 assert.equal(vm.runInContext('monthDates(2028,1).days',c),29);
 assert.equal(vm.runInContext('monthDates(2027,1).days',c),28);
 assert.equal(vm.runInContext('monthDates(2026,12).year',c),2027);
 assert.equal(vm.runInContext('monthDates(2026,-1).month',c),11);
 assert.equal(vm.runInContext('monthDates(100,-1)',c),null);
 assert.equal(vm.runInContext('monthDates(9999,12)',c),null);
});
test('partilha abre a data escolhida no endereço público',()=>{
 const c=context();
 assert.equal(vm.runInContext("dailyShareData(new Date(2026,7,23),{titulo:'São Filipe Benizi'}).url",c),'https://charlieleitao-spec.github.io/hoje-familia-servita/?data=2026-08-23');
 assert.match(vm.runInContext("dailyShareData(new Date(2026,7,23),{titulo:'São Filipe Benizi'}).text",c),/Filipe/);
});
test('formação acompanha a pessoa e usa a Biblioteca quando não há artigo específico',()=>{
 const c=context();
 assert.match(vm.runInContext("dailyFormation({titulo:'São Filipe Benizi'},null).url",c),/sao-filipe-benizi-osm/);
 assert.match(vm.runInContext("dailyFormation({titulo:'Sete Santos Fundadores'},null).url",c),/os-sete-santos-fundadores/);
 assert.match(vm.runInContext("dailyFormation({titulo:'Outro beato'},null).url",c),/biblioteca-e-formacao-servita/);
 assert.match(vm.runInContext("dailyFormation(null,dailyReflection(new Date(2026,9,27))).url",c),/#/);
});
test('retomada ignora armazenamento inválido, indisponível e não preserva dados excedentes',()=>{
 assert.equal(vm.runInContext('readLastReading()',context('{invalid')),null);
 assert.equal(vm.runInContext('readLastReading()',context('{"date":"ontem"}')),null);
 const c=context('{"date":"2026-08-23","hour":"laudes","extra":"não guardar"}');
 assert.equal(vm.runInContext('readLastReading().hour',c),'laudes');
 assert.equal(vm.runInContext('Object.keys(readLastReading()).length',c),2);
 c.localStorage.getItem=()=>{throw Error('negado')};assert.equal(vm.runInContext('readLastReading()',c),null);
});
