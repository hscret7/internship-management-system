'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('src/main/webapp/assets/js/email-autocomplete.js','utf8');
class Element {
 constructor(tag='input'){this.tag=tag;this.value='';this.dataset={};this.attrs={};this.style={};this.events={};this.children=[];this.classList={toggle(){}};}
 setAttribute(k,v){this.attrs[k]=v;}removeAttribute(k){delete this.attrs[k];}
 insertAdjacentElement(where,node){this.menu=node;}replaceChildren(...nodes){this.children=nodes;}appendChild(node){this.children.push(node);}
 addEventListener(name,fn){(this.events[name]||=[]).push(fn);}emit(name,event={}){for(const fn of this.events[name]||[])fn(event);}
 dispatchEvent(event){this.emit(event.type,event);}focus(){this.emit('focus');}
}
function fixture(options={}){
 const input=new Element(),window={},timers=new Map(),pending=[];let id=0,selected;
 const document={readyState:'complete',createElement:tag=>new Element(tag),querySelectorAll:()=>[]};
 const fetch=url=>new Promise(resolve=>pending.push({url,resolve}));
 vm.runInNewContext(source,{window,document,fetch,URLSearchParams,Event,setTimeout:(fn,delay)=>{assert.equal(delay,250);timers.set(++id,fn);return id;},clearTimeout:key=>timers.delete(key)});
 window.EmailAutocomplete.attach(input,{context:'/test',onSelect:item=>selected=item,...options});
 const type=value=>{input.value=value;input.emit('input');};
 const tick=()=>{const work=[...timers.values()];timers.clear();work.forEach(fn=>fn());};
 const answer=async(data,ok=true)=>{const request=pending.shift();assert.ok(request);request.resolve({ok,redirected:false,headers:{get:()=> 'application/json'},json:async()=>({success:ok,data})});await new Promise(setImmediate);return request.url;};
 return {input,type,tick,answer,timers,pending,get selected(){return selected;}};
}
(async()=>{
 const f=fixture();f.type('a');assert.equal(f.timers.size,0);f.type('am');f.type('ample');assert.equal(f.timers.size,1);f.tick();
 const url=await f.answer([{email:'user@example.com',fullName:'<script>'},{email:'USER@EXAMPLE.COM',fullName:'Duplicate'},{email:'other@domain.com'}]);
 assert.match(url,/q=ample/);assert.match(url,/api\/hr\/email-suggestions/);assert.equal(f.input.menu.children.length,1);assert.equal(f.input.menu.hidden,false);
 assert.equal(f.input.menu.children[0].textContent,'user@example.com · <script>');
 let prevented=false;f.input.emit('keydown',{key:'ArrowDown',preventDefault(){}});f.input.emit('keydown',{key:'Enter',preventDefault(){prevented=true;},stopImmediatePropagation(){}});
 assert.ok(prevented);assert.equal(f.input.value,'user@example.com');assert.equal(f.selected.email,'user@example.com');assert.equal(f.input.menu.hidden,true);assert.equal(f.timers.size,0);
 f.type('qa');f.tick();f.type('new');await f.answer([{email:'qa@example.com'}]);assert.equal(f.input.menu.hidden,true,'old results cannot reappear');
 f.tick();await f.answer([{email:'new@example.com'}]);assert.equal(f.input.menu.children[0].textContent,'new@example.com');
 f.input.emit('keydown',{key:'Escape'});assert.equal(f.input.menu.hidden,true);
 f.type('missing');f.tick();await f.answer([]);assert.match(f.input.menu.children[0].textContent,/Không có/);
 f.type('error');f.tick();await f.answer([],false);assert.match(f.input.menu.children[0].textContent,/vẫn có thể nhập/);
 f.type('late');f.tick();f.input.emit('blur');await f.answer([{email:'late@example.com'}]);assert.equal(f.input.menu.hidden,true);
 const actor=fixture({scope:'actor'});actor.type('ad');actor.tick();assert.match(await actor.answer([]),/admin\/email-suggestions/);
 const multi=fixture({multiple:true});multi.type('one@example.com, exam');multi.tick();assert.match(await multi.answer([{email:'two@example.com'}]),/q=exam/);
 multi.input.menu.children[0].emit('click');assert.equal(multi.input.value,'one@example.com, two@example.com');
 console.log('PASS: substring/case-insensitive suggestions, debounce, deduplication, keyboard/mouse selection, stale-response and blur guards, literal labels, failures and multiple-email input.');
})().catch(error=>{console.error(error);process.exitCode=1;});
