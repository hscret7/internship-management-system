/* Identity picker: users search by email; the existing numeric keys remain internal. */
(() => {
    'use strict';
    document.querySelectorAll('[data-email-selection]').forEach(root => {
        const value = root.querySelector('[data-email-value]');
        const ctx = root.dataset.context || '';
        const application = root.dataset.emailSelection === 'application';
        const optional = root.dataset.optional === 'true';
        const form = value.form;
        value.value = value.value || '';
        const initial = value.value;
        let sequence = 0, entries = [];
        const email = document.createElement('input');
        email.type = 'email'; email.className = 'form-control'; email.maxLength = 254;
        email.autocomplete = 'off'; email.placeholder = 'Nhập Gmail hoặc email đầy đủ';
        email.id = value.id + '-email';
        email.value = root.dataset.email || '';
        if (root.dataset.emailName) email.name = root.dataset.emailName;
        const label = document.createElement('label'); label.className = 'form-label';
        label.htmlFor = email.id; label.textContent = 'Gmail / email thực tập sinh';
        const search = document.createElement('button'); search.type = 'button';
        search.className = 'btn btn-outline-primary mt-2'; search.textContent = 'Tra cứu';
        const choice = document.createElement('select'); choice.className = 'form-select mt-2';
        choice.id = value.id + '-choice'; choice.required = !optional;
        const choiceLabel = document.createElement('label');choiceLabel.htmlFor = choice.id;
        choiceLabel.className = 'form-label mt-2';choiceLabel.textContent = application ? 'Đơn ứng tuyển đã duyệt' : 'Chương trình ghi danh';
        const message = document.createElement('p');message.className = 'form-text';message.setAttribute('role','status');
        const notify = (text, error=false) => { message.textContent = text;message.className=error?'text-danger mt-2':'form-text'; };
        function invalidate(clear=true) {
            sequence++;entries=[];
            if(clear) value.value='';
            choice.replaceChildren(new Option(optional?'Tất cả / chưa chọn':'Chọn sau khi tra cứu',''));
            choice.required = !optional || !!email.value.trim();
            if(email.value.trim()) choice.setCustomValidity('Tra cứu email và chọn đúng hồ sơ trước khi tiếp tục.');
            else choice.setCustomValidity('');
        }
        invalidate(false);
        root.append(label,email,search,choiceLabel,choice,message);
        const fallback=document.createElement('noscript');fallback.textContent='Cần bật JavaScript để tra cứu email và chọn đúng hồ sơ.';root.append(fallback);
        if(optional) {
            const reset=document.createElement('button');reset.type='button';reset.className='btn btn-outline-secondary mt-2 ms-2';reset.textContent='Xem tất cả';
            reset.addEventListener('click',()=>{email.value='';invalidate();notify('Đã bỏ lọc theo thực tập sinh.');});root.append(reset);
        }
        email.addEventListener('input',()=>{invalidate();notify('Bấm Tra cứu rồi chọn đúng hồ sơ / chương trình.');});
        choice.addEventListener('change',()=>{
            value.value=choice.value;
            choice.setCustomValidity(choice.value||optional&&!email.value.trim()?'':'Chọn một mục sau khi tra cứu.');
            const selected=entries.find(e=>String(application?e.applicationId:e.enrollmentId)===choice.value);
            const program=root.dataset.programTarget&&document.getElementById(root.dataset.programTarget);
            if(program&&selected)program.value=selected.programId;
            value.dispatchEvent(new Event('change',{bubbles:true}));
        });
        async function lookup(selectedId) {
            const id = typeof selectedId==='string'?selectedId:'';
            if(!id && (!email.value.trim() || !email.reportValidity())) {notify('Nhập email đầy đủ hợp lệ.',true);return;}
            invalidate();const version=sequence;search.disabled=true;
            try {
                const params=new URLSearchParams({action:application?'application-options':'enrollments'});
                if(id&&application)params.set('applicationId',id);else params.set('email',email.value.trim());
                const response=await fetch(ctx+(application?'/hr/enrollments':'/api/hr/work-schedules')+'?'+params,{credentials:'same-origin',headers:{Accept:'application/json'}});
                if(response.redirected||!(response.headers.get('content-type')||'').includes('application/json'))throw Error('Phiên đăng nhập hết hạn hoặc phản hồi không hợp lệ.');
                const body=await response.json();
                if(!response.ok||!body.success)throw Error(typeof body.error==='string'?body.error:body.error?.message||'Không thể tra cứu.');
                if(version!==sequence)return;
                if(!Array.isArray(body.data))throw Error('Danh sách trả về không hợp lệ.');
                entries=body.data;
                for(const entry of entries) {
                    const key=application?entry.applicationId:entry.enrollmentId;
                    choice.add(new Option(entry.internName+' · '+entry.email+' · '+(application?'Đơn đã duyệt #'+key:entry.programName+' · '+entry.enrollmentStatus),String(key)));
                }
                if(id) {
                    const selected=entries.find(e=>String(application?e.applicationId:e.enrollmentId)===id);
                    if(selected){choice.value=id;value.value=id;email.value=selected.email;choice.setCustomValidity('');}
                }
                notify(entries.length?'Chọn đúng '+(application?'đơn đã duyệt':'chương trình')+' trước khi tiếp tục.':application?'Không có đơn đã duyệt đủ điều kiện ghi danh cho email này.':'Không tìm thấy ghi danh cho email này.',!entries.length);
            } catch(error) {if(version===sequence)notify(error.message,true);}
            finally {search.disabled=false;}
        }
        search.addEventListener('click',()=>lookup());
        email.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();lookup();}});
        if(form)form.addEventListener('submit',event=>{if(!choice.reportValidity()){event.preventDefault();event.stopImmediatePropagation();choice.focus();}}, true);
        if (typeof window !== 'undefined' && window.EmailAutocomplete) {
            window.EmailAutocomplete.attach(email, {context:ctx,scope:application?'application':'enrollment',onSelect:()=>lookup()});
        }
        if(initial && (application||email.value.trim()))lookup(initial);
        else if(initial)notify('Đang giữ bộ lọc đã chọn. Tra cứu email để đổi, hoặc bấm Xem tất cả.');
        else notify(application?'Chỉ hiện đơn APPROVED chưa ghi danh và hồ sơ chưa có chương trình đang hoạt động.':'Một email có thể có nhiều ghi danh; chọn đúng chương trình.');
    });
})();
