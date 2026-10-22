'use strict';
const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/webapp/assets/js/fe3-sprint3.js'),'utf8');
class Element {
    constructor(tag='div'){this.tagName=tag;this.children=[];this.listeners={};this.dataset={};this.style={};this.disabled=false;this.textContent='';this.value='';this.hidden=false;this.fields={};}
    appendChild(e){this.children.push(e);return e;}
    replaceChildren(...items){this.children=items;this.textContent='';}
    addEventListener(name,fn){this.listeners[name]=fn;}
    focus(){}
    click(){if(this.listeners.click)return this.listeners.click({currentTarget:this,preventDefault(){}});}
    dispatch(name){return this.listeners[name]({currentTarget:this,preventDefault(){}});}
    reset(){}
    querySelectorAll(){return this.selected||[];}
}
const response=(data,status=200,mime='application/json',redirected=false)=>({ok:status<400,status,redirected,headers:{get(){return mime;}},json:async()=>data,blob:async()=>({})});
const tick=()=>new Promise(resolve=>setImmediate(resolve));
async function settle(){for(let i=0;i<8;i++)await tick();}
function app(fetch,page='end-term') {
    const elements=new Map();const get=id=>{if(!elements.has(id))elements.set(id,new Element());return elements.get(id);};
    get('fe3Sprint3').dataset={page,context:'/preview',userId:'2'};
    const downloads=[];
    const context={document:{getElementById:get,createElement:tag=>{const e=new Element(tag);if(tag==='a')e.click=()=>downloads.push(e.download);return e;}},
        window:{confirm:()=>true},fetch,URL:{createObjectURL:()=> 'blob:test',revokeObjectURL(){}},URLSearchParams,Intl,Date,Map,Set,Array,Object,String,Number,Error,
        FormData:class{constructor(form){this.form=form;}entries(){return Object.entries(this.form.fields);}},setTimeout:()=>0};
    vm.runInNewContext(source,context);return {get,downloads};
}
const empty={items:[],hasMore:false,generatedAt:'2026-10-09T17:00:00Z'};
const text=e=>e.textContent+' '+e.children.map(text).join(' ');
test('empty report is explicit and leaves valid filtered export available',async()=>{
    const ui=app(async()=>response(empty));await settle();
    assert.match(text(ui.get('endTermRows')),/Không có ghi danh/);assert.equal(ui.get('exportExcel').disabled,false);
    assert.equal(ui.get('reportNext').disabled,true);assert.match(ui.get('reportSnapshot').textContent,/10\/10\/26/);
});
test('failure or expired HTML is not an empty successful report and blocks export',async()=>{
    for(const result of [response({error:'Database unavailable'},503),response({},200,'text/html',true)]) {
        const ui=app(async()=>result);await settle();assert.equal(ui.get('exportExcel').disabled,true);
        assert.equal(ui.get('endTermRows').children.length,0);assert.match(ui.get('s3Notice').className,/danger/);
    }
});
test('export reuses applied filters, not unsubmitted form edits',async()=>{
    const calls=[];const ui=app(async(url)=>{calls.push(url);return url.includes('/export?')?response(null,200,'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'):response(empty);});await settle();
    ui.get('endTermFilter').fields={programId:'12',status:'COMPLETED'};ui.get('endTermFilter').dispatch('submit');await settle();
    ui.get('endTermFilter').fields={programId:'99'};await ui.get('exportExcel').click();
    const url=calls.find(u=>u.includes('/export?'));assert.match(url,/programId=12/);assert.match(url,/status=COMPLETED/);assert.doesNotMatch(url,/99/);assert.deepEqual(ui.downloads,['bao-cao-cuoi-ky.xlsx']);
});
test('slow obsolete response cannot replace latest filter data',async()=>{
    const pending=[];const ui=app(url=>new Promise(resolve=>pending.push({url,resolve})));
    ui.get('endTermFilter').fields={programId:'2'};ui.get('endTermFilter').dispatch('submit');
    pending[1].resolve(response(empty));await settle();pending[0].resolve(response({...empty,hasMore:true}));await settle();
    assert.match(ui.get('reportSnapshot').textContent,/chương trình 2/);assert.equal(ui.get('reportNext').disabled,true);
});
test('assessment history renders author, labelled criteria and untrusted text literally',async()=>{
    const item={internName:'<img onerror=bad()>',enrollmentId:1,programId:2,programName:'Java',status:'COMPLETED',startDate:'2026-09-01',endDate:'2026-10-01',taskCount:2,tasksDone:1,submittedReports:1,reviewedReports:1,university:'ICTU',major:'IT',legacyScore:80,
        assessments:[{mentorName:'Mentor A',skillsScore:4,attitudeScore:5,createdAt:'2026-10-09T17:00:00Z',comment:'<script>bad()</script>'}]};
    const ui=app(async()=>response({...empty,items:[item]}));await settle();
    const historyButton=ui.get('endTermRows').children[0].children[6].children[0];historyButton.click();
    const content=text(ui.get('assessmentHistoryRows'));assert.match(content,/Mentor A/);assert.match(content,/Kỹ năng: 4\/5/);assert.match(content,/<script>bad\(\)<\/script>/);assert.match(content,/80\/100/);
});
test('meeting uses current participant source and Vietnam time converted to UTC',async()=>{
    const calls=[];const ui=app(async(url,options)=>{calls.push({url,options});if(url.includes('meeting-participants'))return response([{programId:1,programName:'Java',enrollmentId:7,internName:'TTS X'}]);if(options.method==='POST')return response({id:8},201);return response({items:[],csrfToken:'meeting-token'});},'meetings');await settle();
    ui.get('meetingProgram').value='1';ui.get('meetingForm').fields={title:'Trao đổi',location:'Phòng A',description:'',startsAt:'2099-10-10T09:00',endsAt:'2099-10-10T10:00'};ui.get('meetingForm').selected=[{value:'7'}];
    await ui.get('meetingForm').dispatch('submit');const post=calls.find(c=>c.options.method==='POST');assert.equal(post.options.body.get('startsAt'),'2099-10-10T02:00:00.000Z');assert.equal(post.options.body.get('csrfToken'),'meeting-token');assert.equal(post.options.body.get('enrollmentIds'),'7');assert.equal(post.options.body.get('action'),'create');
});
