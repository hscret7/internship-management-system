const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const base = path.resolve(__dirname, '../../main/webapp');
const js = fs.readFileSync(path.join(base, 'assets/js/hr-meetings.js'), 'utf8');
const functions = js.slice(js.indexOf('  const iso ='), js.indexOf('  async function load()'));
for (const tz of ['UTC', 'Asia/Ho_Chi_Minh']) {
  process.env.TZ = tz;
  const context = {Intl, Date}; vm.createContext(context);
  vm.runInContext(functions + '\nthis.formatters={iso,localInput};', context);
  assert.equal(context.formatters.iso('2026-10-13T09:00'), '2026-10-13T02:00:00.000Z');
  assert.equal(context.formatters.localInput('2026-10-13T02:00:00Z'), '2026-10-13T09:00');
}
for (const file of fs.readdirSync(path.join(base, 'assets/js')).filter(f => f.endsWith('.js'))) {
  new vm.Script(fs.readFileSync(path.join(base, 'assets/js', file), 'utf8'), {filename:file});
}
const leave = fs.readFileSync(path.join(base, 'assets/js/leave-requests.js'), 'utf8');
assert.match(leave, /render\(body\.data\|\|\[\]\)/);
const bulk = fs.readFileSync(path.join(base, 'assets/js/work-schedules.js'), 'utf8');
assert.ok(!bulk.includes("if(!$('scheduleForm').reportValidity()||!e.currentTarget.reportValidity()"));
assert.match(bulk, /today\.getTime\(\)\+30\*86400000/);
assert.match(leave, /today\.getTime\(\)\+60\*86400000/);
const detail = fs.readFileSync(path.join(base,'hr/intern-detail.jsp'),'utf8');
for (const form of detail.matchAll(/<form\b[^>]*method="post"[^>]*>([\s\S]*?)<\/form>/gi)) {
  assert.match(form[1], /name="csrfToken"/);
}
assert.match(detail, /name="applicationId" value="\$\{approvedApplication\.applicationId\}"/);
console.log('Frontend syntax, Vietnam meeting timezone, CSRF and integration regressions passed.');
