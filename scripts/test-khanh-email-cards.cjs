'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('src/main/webapp/assets/js/work-schedules.js','utf8');
function element(){const classes=new Set(['d-none']);return{value:'',dataset:{},children:[],events:{},innerHTML:'',textContent:'',classList:{add:x=>classes.add(x),remove:x=>classes.delete(x),contains:x=>classes.has(x),toggle:x=>classes.has(x)?classes.delete(x):classes.add(x)},addEventListener(name,fn){this.events[name]=fn;},appendChild(child){this.children.push(child);},focus(){},setCustomValidity(message){this.validation=message;},reportValidity(){return !this.validation;}};}
const elements={};const get=id=>elements[id]??=element();get('scheduleApp').dataset.api='/api/hr/work-schedules';
const document={getElementById:get,createElement:()=>element()};
const directory=[{email:'intern@example.com',fullName:'Intern A',enrollmentId:12,programName:'Java',university:'Uni',major:'IT'},{email:'waiting@example.com',fullName:'Waiting',enrollmentId:null}];
const fetch=async url=>({ok:true,redirected:false,headers:{get:()=> 'application/json'},json:async()=>({success:true,csrfToken:'token',data:url.includes('action=interns')?directory:[]})});
(async()=>{
 await vm.runInNewContext(source,{document,fetch,URLSearchParams,Intl,Date,console,confirm:()=>true});
 assert.equal(get('internEmailList').children.length,2);
 get('internEmail').value='intern@example.com';get('internEmail').events.input({target:get('internEmail')});
 assert.equal(get('enrollmentId').value,12);assert.equal(get('cardName').textContent,'Intern A');
 get('internEmail').value='unknown@example.com';get('internEmail').events.input({target:get('internEmail')});assert.equal(get('enrollmentId').value,'');
 get('internEmail').value='Waiting';get('internEmail').events.input({target:get('internEmail')});assert.equal(get('enrollmentId').value,'');
 get('internEmail').value='waiting@example.com';get('internEmail').events.change({target:get('internEmail')});assert.equal(get('enrollmentId').value,'');assert.ok(get('internEmail').validation);
 get('btnClearEmail').events.click();assert.equal(get('internEmail').value,'');assert.equal(get('enrollmentId').value,'');
 console.log('PASS: Khánh Gmail cards/datalist, email identity, stale-ID clearing, unenrolled state and reset.');
})().catch(error=>{console.error(error);process.exitCode=1;});
