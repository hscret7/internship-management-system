(async () => {
  const root=document.getElementById('scheduleApp'),api=root.dataset.api,$=id=>document.getElementById(id),alertBox=$('scheduleAlert');let token='';
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const iso=d=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit'}).format(d),today=new Date(),from=new Date(Date.now()-7*86400000);
  if ($('workDate')) $('workDate').min=iso(today);
  const localDate=d=>d?new Date(d+'T00:00:00').toLocaleDateString('vi-VN'):'';
  function flash(text,type='info'){alertBox.innerHTML=`<div class="alert alert-${type} py-2" role="alert">${esc(text)}</div>`;}

  async function call(url,options={}){
    const r=await fetch(url,{credentials:'same-origin',...options});
    const ct=r.headers.get('content-type')||'';
    if(r.redirected||!ct.includes('application/json'))throw Error('Phiên đăng nhập hết hạn hoặc phản hồi không hợp lệ. Hãy tải lại trang.');
    const b=await r.json();
    if(b.csrfToken){token=b.csrfToken;if($('csrfToken'))$('csrfToken').value=token;}
    if(!r.ok||b.success===false&&!b.data)throw Error(b.error?.message||`Lỗi HTTP ${r.status}`);
    return{body:b,response:r};
  }
  const post=data=>call(api,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:data.toString()});
  function query(){
    const q=new URLSearchParams({from:$('filterFrom').value,to:$('filterTo').value});
    if($('filterProgram').value)q.set('programId',$('filterProgram').value);
    return q;
  }

  let internsList = [];
  const emailMap = Object.create(null);
  const enrollmentMap = Object.create(null);

  async function loadInterns() {
    try {
      const { body } = await call(`${api}?action=interns`);
      internsList = body.data || [];
      const dataList = $('internEmailList');
      if (dataList) dataList.innerHTML = '';
      internsList.forEach(it => {
        if (it.email) {
          emailMap[it.email.toLowerCase()] = it;
        }
        if (it.enrollmentId) {
          enrollmentMap[it.enrollmentId] = it;
        }
        if (dataList && it.email) {
          const opt = document.createElement('option');
          opt.value = it.email;
          opt.textContent = `${it.fullName || 'Thực tập sinh'} - ${it.programName || 'Chưa ghi danh'}`;
          dataList.appendChild(opt);
        }
      });
    } catch (err) {
      flash('Không tải được gợi ý Gmail. Bạn vẫn có thể nhập email đầy đủ; máy chủ sẽ kiểm tra ghi danh.','warning');
    }
  }

  function getAvatarLetters(name) {
    if (!name) return 'TTS';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  function updateInternCard(inputVal) {
    const card = $('internInfoCard');
    const hiddenId = $('enrollmentId');
    const emailInput = $('internEmail');
    if (!card || !emailInput) return;

    const val = (inputVal || emailInput.value || '').trim();
    if (!val) {
      card.classList.add('d-none');
      if (hiddenId) hiddenId.value = '';
      emailInput.setCustomValidity('');
      return;
    }

    const matched = emailMap[val.toLowerCase()] || internsList.find(it =>
      (it.email && it.email.toLowerCase() === val.toLowerCase())
    );

    if (matched) {
      card.classList.remove('d-none');
      $('cardAvatar').textContent = getAvatarLetters(matched.fullName);
      $('cardName').textContent = matched.fullName || 'Thực tập sinh';
      $('cardEmail').textContent = matched.email || val;
      $('cardEdu').textContent = (matched.university || 'Chưa cập nhật trường') + (matched.major ? ' • ' + matched.major : '');

      if (matched.enrollmentId) {
        if (hiddenId) hiddenId.value = matched.enrollmentId;
        $('cardProgram').textContent = matched.programName || 'Chương trình thực tập';
        $('cardStatusBadge').innerHTML = `<span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Đang thực tập (Mã ghi danh: #${matched.enrollmentId})</span>`;
        emailInput.setCustomValidity('');
      } else {
        if (hiddenId) hiddenId.value = '';
        $('cardProgram').textContent = 'Chưa phân bổ chương trình thực tập';
        $('cardStatusBadge').innerHTML = `<span class="badge bg-warning text-dark"><i class="bi bi-exclamation-triangle me-1"></i>Chưa ghi danh vào chương trình nào</span><small class="d-block text-danger mt-1">Cần vào 'Ghi danh thực tập' trước khi tạo ca làm việc.</small>`;
        emailInput.setCustomValidity('Tài khoản này chưa có đợt thực tập hợp lệ.');
      }
    } else if (val.includes('@')) {
      card.classList.remove('d-none');
      $('cardAvatar').textContent = 'NEW';
      $('cardName').textContent = 'Tài khoản: ' + val;
      $('cardEmail').textContent = val;
      $('cardEdu').textContent = 'Tài khoản mới';
      $('cardProgram').textContent = 'Kiểm tra thông tin ghi danh...';
      $('cardStatusBadge').innerHTML = `<span class="badge bg-info text-dark"><i class="bi bi-info-circle me-1"></i>Gmail mới</span>`;
      if (hiddenId) hiddenId.value = '';
      emailInput.setCustomValidity('');
    } else {
      card.classList.add('d-none');
      if (hiddenId) hiddenId.value = '';
      emailInput.setCustomValidity('');
    }
  }

  if ($('internEmail')) {
    $('internEmail').addEventListener('input', e => updateInternCard(e.target.value));
    $('internEmail').addEventListener('change', e => updateInternCard(e.target.value));
  }
  if ($('btnClearEmail')) {
    $('btnClearEmail').addEventListener('click', () => {
      $('internEmail').value = '';
      updateInternCard('');
      $('internEmail').focus();
    });
  }

  function render(rows){
    const target=$('scheduleRows');
    if(!rows.length){
      target.innerHTML='<tr><td colspan="6" class="text-center text-muted py-5"><strong>Chưa có ca làm trong khoảng ngày đã chọn.</strong><br>Đổi bộ lọc hoặc tạo ca mới.</td></tr>';
      return;
    }
    target.innerHTML=rows.map(s => {
      const email = s.internEmail || (enrollmentMap[s.enrollmentId] ? enrollmentMap[s.enrollmentId].email : (emailMap[s.internName?.toLowerCase()]?.email || ''));
      const emailHtml = email ? `<br><small class="text-primary font-monospace"><i class="bi bi-envelope me-1"></i>${esc(email)}</small>` : `<br><small class="text-muted">#${esc(s.enrollmentId)}</small>`;
      return `<tr>
        <td>${esc(localDate(s.workDate))}<br><span class="text-muted small">${esc(s.startTime)}–${esc(s.endTime)}</span></td>
        <td><strong>${esc(s.internName)}</strong>${emailHtml}</td>
        <td>${esc(s.programName)}</td>
        <td>${esc(s.note||'—')}</td>
        <td><span class="badge ${s.status==='CANCELLED'?'text-bg-secondary':'text-bg-success'}">${s.status==='CANCELLED'?'Đã hủy':'Đang hiệu lực'}</span>${s.approvedLeave?' <span class="badge text-bg-warning">Đã duyệt nghỉ</span>':''}</td>
        <td>${s.status!=='CANCELLED'?`<button class="btn btn-sm btn-outline-danger" data-cancel="${Number(s.scheduleId)}">Hủy ca</button>`:''}</td>
      </tr>`;
    }).join('');
  }

  async function load(){
    if(!$('filterForm').reportValidity())return;
    $('scheduleRows').innerHTML='<tr><td colspan="6" class="text-center py-4">Đang tải…</td></tr>';
    try {
      const{body}=await call(`${api}?${query()}`);
      render(body.data||[]);
    } catch(e) {
      $('scheduleRows').innerHTML='<tr><td colspan="6" class="text-center text-danger py-4">Không tải được danh sách lịch.</td></tr>';
      flash(e.message,'danger');
    }
  }

  function validTime(){
    if($('endTime').value<=$('startTime').value){
      $('endTime').setCustomValidity('Giờ kết thúc phải sau giờ bắt đầu.');
      $('endTime').reportValidity();
      $('endTime').setCustomValidity('');
      return false;
    }
    return true;
  }

  $('scheduleForm').addEventListener('submit',async e=>{
    e.preventDefault();
    if(!e.currentTarget.reportValidity()||!validTime())return;
    try {
      const p = new URLSearchParams({
        action: 'create',
        csrfToken: token,
        internEmail: $('internEmail').value,
        enrollmentId: $('enrollmentId').value || '',
        workDate: $('workDate').value,
        startTime: $('startTime').value,
        endTime: $('endTime').value,
        note: $('note').value
      });
      await post(p);
      flash('Đã lưu ca làm việc cho ' + $('internEmail').value + ' thành công.','success');
      $('note').value = '';
      await load();
    } catch(err) {
      flash(err.message,'danger');
    }
  });

  $('bulkToggle').addEventListener('click',()=>$('bulkForm').classList.toggle('d-none'));

  $('bulkForm').addEventListener('submit',async e=>{
    e.preventDefault();
    if(!e.currentTarget.reportValidity()||!['workDate','startTime','endTime'].every(id=>$(id).reportValidity())||!validTime())return;
    const rawTokens = $('enrollmentIds').value.split(',').map(x=>x.trim()).filter(Boolean);
    if(!rawTokens.length||rawTokens.length>100){
      flash('Nhập từ 1 đến 100 Gmail (hoặc mã ghi danh), phân tách bằng dấu phẩy.','warning');
      return;
    }
    try {
      const{body}=await post(new URLSearchParams({
        action:'bulk-create',
        csrfToken:token,
        enrollmentIds: rawTokens.join(','),
        workDate:$('workDate').value,
        startTime:$('startTime').value,
        endTime:$('endTime').value,
        note:$('note').value
      }));
      const d=body.data||{},failed=(d.results||[]).filter(x=>!x.success);
      flash(`${d.successCount||0} ca đã lưu; ${d.failureCount||0} ca lỗi.${failed.length?' Xung đột/không hợp lệ: '+failed.map(x=>x.enrollmentId+' ('+x.errorCode+')').join(', '):''}`,failed.length?'warning':'success');
      await load();
    } catch(err){
      flash(err.message,'danger');
    }
  });

  $('filterForm').addEventListener('submit',e=>{e.preventDefault();load();});

  $('scheduleRows').addEventListener('click',async e=>{
    const b=e.target.closest('[data-cancel]');
    if(!b||!confirm('Hủy ca này?'))return;
    try {
      await post(new URLSearchParams({action:'cancel',csrfToken:token,scheduleId:b.dataset.cancel}));
      flash('Đã hủy ca.','success');
      await load();
    } catch(err){
      flash(err.message,'danger');
    }
  });

  $('filterFrom').value=iso(from);$('filterTo').value=iso(today);
  $('filterTo').value=iso(new Date(today.getTime()+30*86400000));

  try {
    const r=await fetch(`${api}?from=${iso(today)}&to=${iso(today)}`,{credentials:'same-origin'});
    const b=await r.json();
    if(!r.ok||!b.csrfToken)throw Error(b.error?.message||'Không lấy được CSRF token.');
    token=b.csrfToken;
    $('csrfToken').value=token;
  } catch(e) {
    flash(e.message,'danger');
  }

  await loadInterns();
  load();
})();
