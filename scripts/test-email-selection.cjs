'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('src/main/webapp/assets/js/email-selection.js','utf8');
class Element {
 constructor(tag='input'){this.tag=tag;this.value='';this.children=[];this.events={};this.dataset={};this.required=false;this.validation='';}
 append(...nodes){this.children.push(...nodes);} add(node){this.children.push(node);} replaceChildren(...nodes){this.children=nodes;}
 setAttribute(key,value){this[key]=value;} setCustomValidity(message){this.validation=message;}
 checkValidity(){return !this.validation&&(!this.required||!!this.value)&&(this.type!=='email'||!this.value||/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.value));}
 reportValidity(){return this.checkValidity();} focus(){} addEventListener(name,fn){(this.events[name]||=[]).push(fn);}
 dispatchEvent(event){this.emit(event.type,event);} emit(name,event={}){return Promise.all((this.events[name]||[]).map(fn=>fn(event)));}
}
function fixture({optional=false,application=false,initial='',address=''}={}){
 const root=new Element('div'),form=new Element('form'),hidden=new Element();hidden.id='identity';hidden.value=initial;hidden.form=form;
 root.dataset={context:'/test',emailSelection:application?'application':'enrollment',optional:String(optional),email:address};root.querySelector=()=>hidden;
 const pending=[];const fetch=url=>new Promise(resolve=>pending.push({url,resolve}));
 const document={querySelectorAll:()=>[root],createElement:tag=>new Element(tag),getElementById:()=>null};
 function Option(text,value){const node=new Element('option');node.textContent=text;node.value=value;return node;}
 vm.runInNewContext(source,{document,fetch,Option,URLSearchParams,Event});
 const email=root.children.find(n=>n.type==='email'),choice=root.children.find(n=>n.tag==='select'),search=root.children.find(n=>n.textContent==='Tra cứu'),message=root.children.find(n=>n.tag==='p');
 const answer=async(data,success=true)=>{const p=pending.shift();assert.ok(p);p.resolve({ok:success,redirected:false,headers:{get:()=> 'application/json'},json:async()=>({success,data,error:'Tra cứu thất bại'})});await new Promise(setImmediate);return p.url;};
 const submit=()=>{let prevented=false;form.emit('submit',{preventDefault(){prevented=true;},stopImmediatePropagation(){}});return prevented;};
 return {root,hidden,email,choice,search,message,pending,answer,submit};
}
(async()=>{
 const f=fixture();assert.ok(f.submit());f.email.value='a@example.com';await f.email.emit('input');f.search.emit('click');
 const url=await f.answer([{enrollmentId:12,internName:'<script>',email:'a@example.com',programName:'Java',enrollmentStatus:'COMPLETED'},{enrollmentId:13,internName:'A',email:'a@example.com',programName:'Python',enrollmentStatus:'ACTIVE'}]);
 assert.match(url,/action=enrollments/);assert.equal(f.hidden.value,'');assert.ok(f.submit());assert.equal(f.choice.children.length,3);assert.match(f.choice.children[1].textContent,/<script>/);
 f.choice.value='12';await f.choice.emit('change');assert.equal(f.hidden.value,'12');assert.equal(f.submit(),false);
 f.email.value='b@example.com';await f.email.emit('input');assert.equal(f.hidden.value,'');assert.ok(f.submit());f.search.emit('click');
 f.email.value='c@example.com';await f.email.emit('input');await f.answer([{enrollmentId:99}]);assert.equal(f.hidden.value,'');assert.equal(f.choice.children.length,1);
 f.search.emit('click');await f.answer([],false);assert.match(f.message.textContent,/thất bại/);assert.ok(f.submit());
 const optional=fixture({optional:true});assert.equal(optional.submit(),false);optional.email.value='a@example.com';await optional.email.emit('input');assert.ok(optional.submit());
 await optional.root.children.find(n=>n.textContent==='Xem tất cả').emit('click');assert.equal(optional.hidden.value,'');assert.equal(optional.submit(),false);
 const app=fixture({application:true,initial:'7'});assert.equal(app.hidden.value,'');const appUrl=await app.answer([{applicationId:7,internName:'Approved',email:'approved@example.com'}]);
 assert.match(appUrl,/application-options/);assert.match(appUrl,/applicationId=7/);assert.equal(app.email.value,'approved@example.com');assert.equal(app.hidden.value,'7');assert.equal(app.submit(),false);
 app.email.value='none@example.com';await app.email.emit('input');app.search.emit('click');await app.answer([]);assert.ok(app.submit());
 console.log('PASS: explicit historical/multiple choice, stale-ID/response clearing, visible failures, optional reset and approved-application links.');
})().catch(error=>{console.error(error);process.exitCode=1;});
