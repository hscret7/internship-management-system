(async () => {
  'use strict';
  const root = document.getElementById('internScheduleLeave'), $ = id => document.getElementById(id);
  if (!root) return;
  const schedule = root.dataset.mode === 'schedule';
  const api = root.dataset.context + '/api/intern/' + (schedule ? 'work-schedules' : 'leave-requests');
  const day = d => new Intl.DateTimeFormat('en-CA', {timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit'}).format(d);
  const now = new Date(); let token = '';
  $('from').value = day(new Date(now.getTime()-30*86400000)); $('to').value = day(new Date(now.getTime()+30*86400000));
  if ($('leaveFrom')) { $('leaveFrom').min = day(now); $('leaveTo').min = day(now); }
  const esc = v => String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const date = v => String(v||'').split('-').reverse().join('/');
  const flash = (v,type='danger') => { $('pageAlert').innerHTML = `<div class="alert alert-${type}">${esc(v)}</div>`; };
  async function call(url,options={}) {
    const response = await fetch(url,{credentials:'same-origin',...options});
    if(response.redirected || !(response.headers.get('content-type')||'').includes('application/json')) throw Error('Phiên đăng nhập hết hạn. Hãy tải lại trang.');
    const body = await response.json(); if(body.csrfToken) token=body.csrfToken;
    if(!response.ok || body.success===false) throw Error(body.error?.message||'Không thể xử lý yêu cầu.');
    return body;
  }
  async function load() {
    if(!$('dateFilter').reportValidity()) return;
    try {
      const body=await call(api+'?'+new URLSearchParams({from:$('from').value,to:$('to').value}));
      $('pageRows').innerHTML=(body.data||[]).map(r=>schedule
        ? `<tr><td>${esc(date(r.workDate))}</td><td>${esc(r.startTime)}–${esc(r.endTime)}</td><td>${esc(r.programName)}</td><td>${esc(r.note||'—')}</td><td>${r.status==='CANCELLED'?'Đã hủy':r.approvedLeave?'Đã duyệt nghỉ':'Có lịch làm'}</td></tr>`
        : `<tr><td>${esc(date(r.fromDate))}–${esc(date(r.toDate))}</td><td>${esc(r.programName)}</td><td>${esc(r.reason)}</td><td>${esc({PENDING:'Chờ HR duyệt',APPROVED:'Đã duyệt',REJECTED:'Từ chối'}[r.status]||r.status)}</td><td>${esc(r.rejectReason||'—')}</td></tr>`).join('') || '<tr><td colspan="5" class="text-center text-muted py-4">Không có dữ liệu trong khoảng ngày đã chọn.</td></tr>';
    }catch(e){$('pageRows').innerHTML='<tr><td colspan="5">Không tải được dữ liệu.</td></tr>';flash(e.message);}
  }
  $('dateFilter').addEventListener('submit',e=>{e.preventDefault();load();});
  $('leaveForm')?.addEventListener('submit',async e=>{
    e.preventDefault();if(!e.currentTarget.reportValidity())return;
    const button=e.currentTarget.querySelector('button');button.disabled=true;
    try {
      await call(api,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams({action:'create',csrfToken:token,enrollmentId:root.dataset.enrollment,fromDate:$('leaveFrom').value,toDate:$('leaveTo').value,reason:$('reason').value})});
      flash('Đã gửi yêu cầu. Đơn đang chờ HR xét duyệt.','success');await load();
    }catch(error){flash(error.message);}finally{button.disabled=false;}
  });
  await load();
})();
