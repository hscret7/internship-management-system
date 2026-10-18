(async () => {
 const root=document.getElementById('scheduleApp'),api=root.dataset.api,$=id=>document.getElementById(id),alertBox=$('scheduleAlert');let token='';
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const iso=d=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit'}).format(d),today=new Date(),from=new Date(Date.now()-7*86400000);$('workDate').min=iso(today);
 const localDate=d=>d?new Date(d+'T00:00:00').toLocaleDateString('vi-VN'):'';
 function flash(text,type='info'){alertBox.innerHTML=`<div class="alert alert-${type} py-2" role="alert">${esc(text)}</div>`;}
 async function call(url,options={}){const r=await fetch(url,{credentials:'same-origin',...options});const ct=r.headers.get('content-type')||'';if(r.redirected||!ct.includes('application/json'))throw Error('Phiên đăng nhập hết hạn hoặc phản hồi không hợp lệ. Hãy tải lại trang.');const b=await r.json();if(b.csrfToken){token=b.csrfToken;$('csrfToken').value=token;}if(!r.ok||b.success===false&&!b.data)throw Error(b.error?.message||`Lỗi HTTP ${r.status}`);return{body:b,response:r};}
 const post=data=>call(api,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:data.toString()});
 function query(){const q=new URLSearchParams({from:$('filterFrom').value,to:$('filterTo').value});if($('filterProgram').value)q.set('programId',$('filterProgram').value);return q;}
 function render(rows){const target=$('scheduleRows');if(!rows.length){target.innerHTML='<tr><td colspan="6" class="text-center text-muted py-5"><strong>Chưa có ca làm trong khoảng ngày đã chọn.</strong><br>Đổi bộ lọc hoặc tạo ca mới.</td></tr>';return;}target.innerHTML=rows.map(s=>`<tr><td>${esc(localDate(s.workDate))}<br><span class="text-muted">${esc(s.startTime)}–${esc(s.endTime)}</span></td><td>${esc(s.internName)} <span class="text-muted">(#${esc(s.enrollmentId)})</span></td><td>${esc(s.programName)}</td><td>${esc(s.note||'—')}</td><td><span class="badge ${s.status==='CANCELLED'?'text-bg-secondary':'text-bg-success'}">${s.status==='CANCELLED'?'Đã hủy':'Đang hiệu lực'}</span>${s.approvedLeave?' <span class="badge text-bg-warning">Đã duyệt nghỉ</span>':''}</td><td>${s.status!=='CANCELLED'?`<button class="btn btn-sm btn-outline-danger" data-cancel="${Number(s.scheduleId)}">Hủy ca</button>`:''}</td></tr>`).join('');}


 async function load(){if(!$('filterForm').reportValidity())return;$('scheduleRows').innerHTML='<tr><td colspan="6" class="text-center py-4">Đang tải…</td></tr>';try{const{body}=await call(`${api}?${query()}`);render(body.data||[]);}catch(e){$('scheduleRows').innerHTML='<tr><td colspan="6" class="text-center text-danger py-4">Không tải được danh sách lịch.</td></tr>';flash(e.message,'danger');}}
 function validTime(){if($('endTime').value<=$('startTime').value){$('endTime').setCustomValidity('Giờ kết thúc phải sau giờ bắt đầu.');$('endTime').reportValidity();$('endTime').setCustomValidity('');return false;}return true;}
 let lookupSequence=0;
 function clearEnrollment(){lookupSequence++;$('enrollmentId').replaceChildren(new Option('Tìm bằng email rồi chọn ghi danh',''));}
 $('enrollmentEmail').addEventListener('input',clearEnrollment);
 $('lookupEnrollment').addEventListener('click',async()=>{
   clearEnrollment();const sequence=lookupSequence;
   if(!$('enrollmentEmail').value.trim()||!$('enrollmentEmail').reportValidity()){flash('Nhập email đầy đủ hợp lệ.','warning');return;}
   const button=$('lookupEnrollment');button.disabled=true;
   try{const{body}=await call(`${api}?${new URLSearchParams({action:'enrollments',email:$('enrollmentEmail').value.trim()})}`);
     if(sequence!==lookupSequence)return;
     const rows=body.data||[];for(const row of rows){const option=new Option(`${row.internName} — ${row.programName} (#${row.enrollmentId}, ${row.enrollmentStatus})`,String(row.enrollmentId));option.disabled=row.enrollmentStatus!=='ACTIVE'||row.programStatus!=='ACTIVE';$('enrollmentId').add(option);}
     flash(rows.length?'Chọn đúng chương trình ghi danh trước khi lưu.':'Không tìm thấy ghi danh cho email này.',rows.length?'info':'warning');
   }catch(e){if(sequence===lookupSequence)flash(e.message,'danger');}finally{button.disabled=false;}
 });
 $('addBulkEnrollment').addEventListener('click',()=>{if(!$('enrollmentId').reportValidity())return;const ids=new Set($('enrollmentIds').value.split(',').map(x=>x.trim()).filter(Boolean));ids.add($('enrollmentId').value);$('enrollmentIds').value=[...ids].join(', ');$('bulkForm').classList.remove('d-none');});
 $('scheduleForm').addEventListener('submit',async e=>{e.preventDefault();if(!e.currentTarget.reportValidity()||!validTime())return;try{await post(new URLSearchParams({action:'create',csrfToken:token,enrollmentId:$('enrollmentId').value,workDate:$('workDate').value,startTime:$('startTime').value,endTime:$('endTime').value,note:$('note').value}));flash('Đã lưu ca làm thành công.','success');await load();}catch(err){flash(err.message,'danger');}});
 $('bulkToggle').addEventListener('click',()=>$('bulkForm').classList.toggle('d-none'));
 $('bulkForm').addEventListener('submit',async e=>{e.preventDefault();if(!e.currentTarget.reportValidity()||!['workDate','startTime','endTime'].every(id=>$(id).reportValidity())||!validTime())return;const ids=[...new Set($('enrollmentIds').value.split(',').map(x=>x.trim()).filter(Boolean))];if(!ids.length||ids.length>100||ids.some(x=>!/^[1-9]\d*$/.test(x))){flash('Nhập 1–100 mã ghi danh dương, phân tách bằng dấu phẩy.','warning');return;}try{const{body}=await post(new URLSearchParams({action:'bulk-create',csrfToken:token,enrollmentIds:ids.join(','),workDate:$('workDate').value,startTime:$('startTime').value,endTime:$('endTime').value,note:$('note').value}));const d=body.data||{},failed=(d.results||[]).filter(x=>!x.success);flash(`${d.successCount||0} ca đã lưu; ${d.failureCount||0} ca lỗi.${failed.length?' Xung đột/không hợp lệ: '+failed.map(x=>x.enrollmentId+' ('+x.errorCode+')').join(', '):''}`,failed.length?'warning':'success');await load();}catch(err){flash(err.message,'danger');}});
 $('filterForm').addEventListener('submit',e=>{e.preventDefault();load();});
 $('scheduleRows').addEventListener('click',async e=>{const b=e.target.closest('[data-cancel]');if(!b||!confirm('Hủy ca này?'))return;try{await post(new URLSearchParams({action:'cancel',csrfToken:token,scheduleId:b.dataset.cancel}));flash('Đã hủy ca.','success');await load();}catch(err){flash(err.message,'danger');}});

  $('filterFrom').value=iso(from);$('filterTo').value=iso(today);
  $('filterTo').value=iso(new Date(today.getTime()+30*86400000));
  try{const r=await fetch(`${api}?from=${iso(today)}&to=${iso(today)}`,{credentials:'same-origin'});const b=await r.json();if(!r.ok||!b.csrfToken)throw Error(b.error?.message||'Không lấy được CSRF token.');token=b.csrfToken;$('csrfToken').value=token;}catch(e){flash(e.message,'danger');}
  load();
})();
