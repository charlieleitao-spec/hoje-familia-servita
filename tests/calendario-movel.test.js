// Teste independente do cálculo proposto para Santa Maria junto à Cruz.
// Executar: node tests/calendario-movel.test.js
const assert=require('node:assert/strict');
function easterSunday(year){
 const a=year%19,b=Math.floor(year/100),c=year%100,d=Math.floor(b/4),e=b%4,f=Math.floor((b+8)/25),g=Math.floor((b-f+1)/3),h=(19*a+b-d-g+15)%30,i=Math.floor(c/4),k=c%4,l=(32+2*e+2*i-h-k+7)%7,m=Math.floor((a+11*h+22*l)/451),n=h+l-7*m+114;
 return new Date(year,Math.floor(n/31)-1,n%31+1,12);
}
function mariaJuntoACruz(year){const d=easterSunday(year);d.setDate(d.getDate()-9);return [d.getFullYear(),d.getMonth()+1,d.getDate()].join('-')}
for(const [year,expected] of [[2025,'2025-4-11'],[2026,'2026-3-27'],[2027,'2027-3-19'],[2028,'2028-4-7'],[2029,'2029-3-23'],[2030,'2030-4-12'],[2031,'2031-4-4']]){
 assert.equal(mariaJuntoACruz(year),expected,'Ano '+year);
}
assert.equal(mariaJuntoACruz(2027),'2027-3-19','Conflito litúrgico com São José: exige regra de precedência, não resolvida por este teste');
console.log('7 datas corretas; precedência litúrgica de 2027 continua pendente de validação.');
