/* hr-meetings.js - externalized from hr/meetings.jsp to avoid JSP EL parsing ${...} as 500 error.
 * Fixes: accurate local -> UTC conversion + escape for title.
 */
(() => {
  const c = (document.currentScript && document.currentScript.dataset.context) || '';
  const f = document.getElementById('form');
  const rows = document.getElementById('rows');
  const msg = document.getElementById('msg');
  let token = '';
  let participantDirectory = [];
  let directoryReady;
  async function loadDirectory() {
    const response = await fetch(c + '/api/hr/work-schedules?action=interns', {credentials:'same-origin', headers:{Accept:'application/json'}});
    if (!response.ok || response.redirected || !(response.headers.get('content-type') || '').includes('application/json')) throw Error('Không tải được danh sách email TTS.');
    const body = await response.json();
    if (!body.success || !Array.isArray(body.data)) throw Error('Không tải được danh sách email TTS.');
    participantDirectory = body.data;
  }

  const field = (n) => f.elements[n];
  const say = (t, isErr) => {
    msg.textContent = t;
    msg.className = 'alert alert-' + (isErr ? 'danger' : 'success');
  };

  // Escape HTML to prevent XSS when rendering title (and other strings).
  const esc = (v) => String(v == null ? '' : v)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

  // Form wall-clock time and display are always Vietnam, independent of browser timezone.
  const iso = v => v ? new Date(v + '+07:00').toISOString() : '';
  const localInput = v => {
    if (!v || Number.isNaN(new Date(v).getTime())) return '';
    const parts = Object.fromEntries(new Intl.DateTimeFormat('en-CA',{
      timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'
    }).formatToParts(new Date(v)).map(p=>[p.type,p.value]));
    return parts.year+'-'+parts.month+'-'+parts.day+'T'+parts.hour+':'+parts.minute;
  };

  async function load() {
    let r = await fetch(c + '/meetings?size=100');
    let d = await r.json();
    token = d.csrfToken || token;
    rows.innerHTML = (d.items || []).map((m) => `<tr><td>${esc(m.title)}</td><td>${new Date(m.startsAt).toLocaleString('vi-VN',{timeZone:'Asia/Ho_Chi_Minh'})} \u2013 ${new Date(m.endsAt).toLocaleTimeString('vi-VN',{timeZone:'Asia/Ho_Chi_Minh'})}</td><td>#${m.programId}</td><td><span class="badge ${m.status === 'CANCELLED' ? 'text-bg-secondary' : 'text-bg-success'}">${m.status}</span></td><td><span class="badge text-bg-info">${m.status === 'CANCELLED' ? 'Kh\u00f4ng g\u1eedi' : 'Theo c\u1ea5u h\u00ecnh server'}</span></td><td>${m.status === 'CANCELLED' ? '' : `<button class="btn btn-sm btn-outline-primary" data-id="${m.id}">S\u1eeda</button>`}</td></tr>`).join('') || '<tr><td colspan="6" class="text-center text-muted">Ch\u01b0a c\u00f3 l\u1ecbch h\u1ecdp.</td></tr>';
  }

  async function detail(id) {
    await directoryReady;
    let response = await fetch(c + '/meetings?id=' + encodeURIComponent(id));
    if (!response.ok) throw Error('Không tải được cuộc họp.');
    let d = await response.json();
    let m = d.item;
    token = d.csrfToken || token;
    if (m.status === 'CANCELLED') {
      say('Cu\u1ed9c h\u1ecdp \u0111\u00e3 b\u1ecb h\u1ee7y, kh\u00f4ng th\u1ec3 ch\u1ec9nh s\u1eeda.', true);
      return;
    }
    const emails = m.enrollmentIds.map(id => {
      const entry = participantDirectory.find(it => it.enrollmentId === id && it.programId === m.programId);
      if (!entry || !entry.email) throw Error('Một người tham gia không còn ghi danh hoạt động. Hãy tải lại và kiểm tra danh sách TTS.');
      return entry.email;
    });
    Object.keys(m).forEach((k) => {
      if (k === 'startsAt' || k === 'endsAt' || k === 'enrollmentIds') return;
      if (field(k)) field(k).value = Array.isArray(m[k]) ? m[k].join(',') : m[k];
    });
    field('enrollmentIds').value = emails.join(', ');
    field('startsAt').value = localInput(m.startsAt);
    field('endsAt').value = localInput(m.endsAt);
    field('programId').readOnly = true;
    document.getElementById('cancel').classList.toggle('d-none', m.status === 'CANCELLED');
  }

  async function post(data) {
    let r = await fetch(c + '/meetings', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ ...data, csrfToken: token }) });
    let d = await r.json();
    if (!r.ok) throw Error(d.error || 'Kh\u00f4ng th\u1ec3 l\u01b0u');
  }

  f.onsubmit = async (e) => {
    e.preventDefault();
    const saveBtn = document.getElementById('save');
    try {
      let x = Object.fromEntries(new FormData(f));
      x.action = x.id ? 'update' : 'create';
      const emails = x.enrollmentIds.split(',').map(value => value.trim());
      if (!emails.length || emails.some(value => !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value))) throw Error('Nhập email đầy đủ của người tham gia, phân tách bằng dấu phẩy.');
      x.enrollmentIds = emails.join(',');
      if (!x.startsAt || !x.endsAt) throw Error('Vui l\u00f2ng nh\u1eadp \u0111\u1ea7y \u0111\u1ee7 th\u1eddi gian b\u1eaft \u0111\u1ea7u v\u00e0 k\u1ebft th\u00fac.');
      const start = new Date(iso(x.startsAt)), end = new Date(iso(x.endsAt));
      if (isNaN(start.getTime()) || isNaN(end.getTime())) throw Error('Ng\u00e0y gi\u1edd kh\u00f4ng h\u1ee3p l\u1ec7. Vui l\u00f2ng ki\u1ec3m tra l\u1ea1i.');
      if (end <= start) throw Error('Th\u1eddi gian k\u1ebft th\u00fac ph\u1ea3i sau th\u1eddi gian b\u1eaft \u0111\u1ea7u.');
      x.startsAt = iso(x.startsAt);
      x.endsAt = iso(x.endsAt);
      if (saveBtn) { saveBtn.disabled = true; saveBtn.textContent = '\u0110ang l\u01b0u\u2026'; }
      await post(x);
      say('\u0110\u00e3 l\u01b0u l\u1ecbch h\u1ecdp. Email \u0111\u01b0\u1ee3c x\u1eed l\u00fd b\u1edfi worker server, kh\u00f4ng l\u1ed9 SMTP client.', false);
      f.reset();
      field('id').value = '';
      field('revision').value = '';
      field('programId').readOnly = false;
      document.getElementById('cancel').classList.add('d-none');
      load();
    } catch (e) { say(e.message, true); }
    finally { if (saveBtn) { saveBtn.disabled = false; saveBtn.textContent = 'L\u01b0u'; } }
  };

  rows.onclick = (e) => { let b = e.target.closest('[data-id]'); if (b) detail(b.dataset.id).catch(error => say(error.message, true)); };
  document.getElementById('new').onclick = () => { f.reset(); field('id').value = ''; field('revision').value = ''; field('programId').readOnly = false; document.getElementById('cancel').classList.add('d-none'); };
  document.getElementById('cancel').onclick = async () => {
    if (!confirm('H\u1ee7y cu\u1ed9c h\u1ecdp n\u00e0y?')) return;
    try { await post({ action: 'cancel', id: field('id').value, revision: field('revision').value }); say('\u0110\u00e3 h\u1ee7y cu\u1ed9c h\u1ecdp.', false); load(); }
    catch (e) { say(e.message, true); }
  };

  directoryReady = loadDirectory();
  directoryReady.catch(error => say(error.message, true));
  load().catch(error => say(error.message, true));
})();
