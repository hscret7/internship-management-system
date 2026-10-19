'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync(require('node:path').join(__dirname,'../src/main/webapp/assets/js/app.js'),'utf8');
const documentEvents={},windowEvents={};
const buttons=['old','new','confirm'].map(id=>{
 const input={id,type:'password',value:'Riêng tư Đặng 123!'};
 const attrs={};const classes=new Set();
 const wrapper={querySelector:()=>input};
 return {input,attrs,closest:()=>wrapper,querySelector:()=>null,getAttribute:k=>attrs[k],
  setAttribute:(k,v)=>attrs[k]=v,classList:{add:(...v)=>v.forEach(x=>classes.add(x)),remove:(...v)=>v.forEach(x=>classes.delete(x))}};
});
const document={addEventListener:(name,fn)=>(documentEvents[name]??=[]).push(fn),querySelectorAll:selector=>selector==='.btn-toggle-password'?buttons:[]};
const window={addEventListener:(name,fn)=>(windowEvents[name]??=[]).push(fn)};
const sandbox={document,window};vm.runInNewContext(source,sandbox);
const start=source.indexOf('    var passwordTogglesInitialized');
const end=source.indexOf('    /* ------------------------------------------------ Hỗ trợ Modal',start);
vm.runInNewContext(source.slice(start,end)+'\ninitPasswordToggle();initPasswordToggle();',sandbox);
assert.equal(documentEvents.click.length,1,'initialization must not double bind');
for(const b of buttons){assert.equal(b.attrs['aria-controls'],b.input.id);assert.equal(b.attrs['aria-pressed'],'false');}
const original=buttons[0].input.value;
window.togglePasswordVisibility(buttons[0]);assert.equal(buttons[0].input.type,'text');assert.equal(buttons[0].attrs['aria-pressed'],'true');
assert.equal(buttons[1].input.type,'password');assert.equal(buttons[0].input.value,original);
window.togglePasswordVisibility(buttons[0]);assert.equal(buttons[0].input.type,'password','rapid second toggle works');
buttons.forEach(b=>window.togglePasswordVisibility(b));documentEvents.submit[0]();assert.ok(buttons.every(b=>b.input.type==='password'));
buttons.forEach(b=>window.togglePasswordVisibility(b));windowEvents.pageshow[0]();assert.ok(buttons.every(b=>b.input.type==='password'));
assert.ok(buttons.every(b=>!JSON.stringify(b.attrs).includes(original)));
console.log('PASS: Khánh password controls keep values, toggle independently, handle rapid clicks, ARIA and submit/history reset.');
