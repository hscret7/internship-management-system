'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/assets/js/fe3.js'), 'utf8');

// Isolated DOM stub: executes the real UI script without a browser or external request.
async function render(entries) {
    function element() {
        return {textContent:'',className:'',children:[],appendChild(child){this.children.push(child);},
            replaceChildren(...children){this.children=children;},addEventListener(){},scrollIntoView(){}};
    }
    const ids = new Map();
    for (const id of ['fe3Notice','evaluationSearch','evaluationRows','metricEvaluationCount',
            'metricEvaluatedInterns','metricAverageScore']) ids.set(id, element());
    ids.get('evaluationSearch').value = '';
    ids.set('fe3App',{dataset:{page:'hr-evaluations',context:'/unit-test'}});
    let requests = 0;
    vm.runInNewContext(source, {
        document:{getElementById(id){return ids.get(id);},createElement:element},
        URLSearchParams,
        fetch:async url=>{assert.equal(url,'/unit-test/hr/be4/evaluations');requests++;
            return {ok:true,redirected:false,headers:{get:()=> 'application/json;charset=UTF-8'},json:async()=>entries};}
    });
    await new Promise(resolve=>setImmediate(resolve));
    assert.equal(requests,1);
    assert.equal(ids.get('fe3Notice').textContent,'');
    return ids;
}

test('different people sharing a name are counted separately, repeat enrollments only once', async()=>{
    const ids=await render([
        {enrollmentId:1,internProfileId:11,internName:'Nguyễn Văn An',score:80},
        {enrollmentId:2,internProfileId:12,internName:'Nguyễn Văn An',score:90},
        {enrollmentId:3,internProfileId:11,internName:'Nguyễn Văn An',score:100}]);
    assert.equal(ids.get('metricEvaluationCount').textContent,3);
    assert.equal(ids.get('metricEvaluatedInterns').textContent,2);
    assert.equal(ids.get('metricAverageScore').textContent,'90.0');
});
test('renaming the same profile does not create a new person', async()=>{
    const ids=await render([{internProfileId:11,internName:'Tên cũ',score:80},
        {internProfileId:11,internName:'Tên mới',score:90}]);
    assert.equal(ids.get('metricEvaluatedInterns').textContent,1);
});
test('older API without stable identities must show unavailable, not a fabricated count', async()=>{
    const ids=await render([{internName:'Tên trùng',score:80},{internName:'Tên trùng',score:90}]);
    assert.equal(ids.get('metricEvaluatedInterns').textContent,'—');
});
test('empty list reports zero people and no average', async()=>{
    const ids=await render([]);
    assert.equal(ids.get('metricEvaluatedInterns').textContent,0);
    assert.equal(ids.get('metricAverageScore').textContent,'—');
});
