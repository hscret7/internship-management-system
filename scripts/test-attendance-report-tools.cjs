const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/attendance-report-tools.js', 'utf8');
function fixture() {
    function element() { return { value: '', disabled: false, dataset: {}, listeners: {},
        addEventListener(name, fn) { this.listeners[name] = fn; },
        replaceChildren(...items) { this.options = items; }, add(item) { this.options.push(item); },
        checkValidity() { return true; }, reportValidity() { return true; } }; }
    const email = element(), choices = element(), lookup = element(), message = element();
    const enrollment = element(), program = element(), form = element();
    const button = element(); button.dataset.attendanceExport = 'xlsx';
    const controls = { '[data-lookup-email]': email, '[data-lookup-choices]': choices,
        '[data-lookup-button]': lookup, '[data-report-message]': message };
    const root = { dataset: { context: '/app', form: 'form', enrollment: 'enrollment', program: 'program' },
        querySelector: key => controls[key], querySelectorAll: () => [button] };
    const elements = { form, enrollment, program };
    const calls = [], filters = { from: '2026-10-01', to: '2026-10-10', status: '' };
    let respond = () => ({ redirected: false, ok: true, status: 200,
        headers: { get: () => 'application/json' }, json: async () => ({ success: true, data: [] }) });
    vm.runInNewContext(source, {
        document: { querySelector: () => root, getElementById: key => elements[key] },
        Option: function (text, value) { this.text = text; this.value = value; }, URLSearchParams,
        FormData: class { get(key) { return ({ ...filters, enrollmentId: enrollment.value, programId: program.value })[key]; } },
        fetch: async (...args) => { calls.push(args); return respond(); }, setTimeout, URL: {}, console
    });
    return { email, choices, lookup, message, enrollment, program, button, filters, calls, response(fn) { respond = fn; } };
}
(async () => {
    const f = fixture();
    f.enrollment.value = '7'; f.program.value = '2'; f.email.value = 'intern@example.com';
    f.response(() => ({ ok: true, redirected: false, headers: { get: () => 'application/json' },
        json: async () => ({ success: true, data: [
            { enrollmentId: 10, programId: 3, email: f.email.value, programName: 'A', enrollmentStatus: 'ACTIVE' },
            { enrollmentId: 11, programId: 4, email: f.email.value, programName: 'B', enrollmentStatus: 'COMPLETED' }
        ] }) }));
    await f.lookup.listeners.click();
    assert.equal(f.enrollment.value, '7', 'Lookup must not auto-select first enrollment');
    assert.equal(f.choices.options.length, 3);
    assert.ok(f.calls[0][0].includes('action=enrollments'));
    f.choices.value = '11'; f.choices.listeners.change();
    assert.equal(f.enrollment.value, 11); assert.equal(f.program.value, 4);
    f.email.listeners.input(); assert.equal(f.choices.disabled, true);
    f.choices.value = '10'; f.choices.listeners.change(); assert.equal(f.enrollment.value, 11);
    f.response(() => ({ ok: false, redirected: false, status: 403, headers: { get: () => 'application/json' }, json: async () => ({ error: 'Không có quyền' }) }));
    await f.lookup.listeners.click(); assert.equal(f.message.textContent, 'Không có quyền'); assert.equal(f.enrollment.value, 11);
    f.response(() => ({ ok: false, redirected: false, status: 503, headers: { get: () => 'application/json' },
        json: async () => ({ error: { code: 'SERVICE_UNAVAILABLE', message: 'Không thể tra cứu ghi danh lúc này' } }) }));
    await f.lookup.listeners.click();
    assert.equal(f.message.textContent, 'Không thể tra cứu ghi danh lúc này', 'Nested schedule errors must remain readable');
    const e = fixture(); e.filters.status = 'ABSENT'; await e.button.listeners.click();
    assert.equal(e.calls.length, 0); assert.match(e.message.textContent, /Tất cả/);
    e.filters.status = ''; e.filters.from = '2025-01-01'; e.filters.to = '2026-01-01';
    e.response(() => ({ ok: false, status: 503, redirected: false, headers: { get: () => 'application/json' }, json: async () => ({ error: 'Thiếu font tiếng Việt' }) }));
    await e.button.listeners.click(); assert.equal(e.calls.length, 1, '366 inclusive days accepted'); assert.equal(e.message.textContent, 'Thiếu font tiếng Việt');
    e.response(() => ({ ok: false, status: 503, redirected: false, headers: { get: () => 'application/json' },
        json: async () => ({ error: { code: 'EXPORT_FAILED', message: 'Chưa thể tạo báo cáo' } }) }));
    await e.button.listeners.click();
    assert.equal(e.message.textContent, 'Chưa thể tạo báo cáo', 'Nested export errors must remain readable');
    e.filters.to = '2026-01-02'; await e.button.listeners.click(); assert.equal(e.calls.length, 2, '367 inclusive days rejected');
    e.filters.from = '2026-01-03'; e.filters.to = '2026-01-02'; await e.button.listeners.click(); assert.equal(e.calls.length, 2);
    assert.equal(e.button.disabled, false);
    console.log('PASS: enrollment choice, stale lookup, permissions, export date/filter validation and visible server errors');
})().catch(error => { console.error(error); process.exitCode = 1; });
