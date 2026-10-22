/* Bounded substring suggestions; choosing an address never chooses an enrollment implicitly. */
(() => {
    'use strict';
    let nextId = 0;
    function attach(input, {context = '', scope = 'enrollment', multiple = false, onSelect} = {}) {
        if (input.dataset.emailAutocompleteReady) return;
        input.dataset.emailAutocompleteReady = 'true';
        input.removeAttribute('list'); // use one consistent substring menu instead of browser-dependent datalists
        input.autocomplete = 'off';
        const menu = document.createElement('div');
        menu.id = 'email-suggestions-' + (++nextId);
        menu.className = 'list-group border rounded shadow-sm mt-1';
        menu.style.maxHeight = '240px';menu.style.overflowY = 'auto';menu.hidden = true;
        menu.setAttribute('role','listbox');menu.setAttribute('aria-label','Gợi ý email');
        input.setAttribute('role','combobox');input.setAttribute('aria-autocomplete','list');
        input.setAttribute('aria-controls',menu.id);input.setAttribute('aria-expanded','false');
        input.insertAdjacentElement('afterend',menu);
        let timer, version = 0, options = [], active = -1, focused = false;
        function close() {
            menu.hidden = true;input.setAttribute('aria-expanded','false');input.removeAttribute('aria-activedescendant');active = -1;
        }
        function invalidate() {clearTimeout(timer);version++;options=[];menu.replaceChildren();close();}
        function choose(index) {
            const selected = options[index];if (!selected) return;
            if (multiple) {
                const caret=input.selectionStart==null?input.value.length:input.selectionStart;
                const start=input.value.lastIndexOf(',',caret-1)+1;
                const comma=input.value.indexOf(',',caret);
                const end=comma<0?input.value.length:comma;
                input.value=input.value.slice(0,start)+(start?' ':'')+selected.email+input.value.slice(end);
            } else input.value = selected.email;
            input.dispatchEvent(new Event('input',{bubbles:true}));
            input.dispatchEvent(new Event('change',{bubbles:true}));
            invalidate();input.focus();
            if (onSelect) onSelect(selected);
        }
        function highlight(index) {
            active = index;
            Array.from(menu.children).forEach((button,i) => {
                button.classList.toggle('active',i===active);
                button.setAttribute('aria-selected',String(i===active));
            });
            input.setAttribute('aria-activedescendant',menu.children[active].id);
        }
        input.addEventListener('focus',()=>{focused=true;});
        input.addEventListener('blur',()=>{focused=false;invalidate();});
        input.addEventListener('input',()=>{
            focused=true;invalidate();
            const caret=input.selectionStart==null?input.value.length:input.selectionStart;
            const start=multiple?input.value.lastIndexOf(',',caret-1)+1:0;
            const comma=multiple?input.value.indexOf(',',caret):-1;
            const query=input.value.slice(start,comma<0?input.value.length:comma).trim();
            if(query.length<2 || query.length>254)return;
            const own=version;
            timer=setTimeout(async()=>{
                try {
                    const route=scope==='actor'?'/admin/email-suggestions':'/api/hr/email-suggestions';
                    const params=new URLSearchParams({q:query,scope});
                    const response=await fetch(context+route+'?'+params,{credentials:'same-origin',headers:{Accept:'application/json'}});
                    if(response.redirected || !response.ok || !(response.headers.get('content-type')||'').includes('application/json'))throw Error('Không tải được gợi ý email.');
                    const body=await response.json();
                    if(own!==version || !focused)return;
                    if(!body.success || !Array.isArray(body.data))throw Error('Không tải được gợi ý email.');
                    const seen=new Set();
                    options=body.data.filter(item=>typeof item.email==='string' && item.email.toLowerCase().includes(query.toLowerCase())
                        && !seen.has(item.email.toLowerCase()) && seen.add(item.email.toLowerCase())).slice(0,20);
                    menu.replaceChildren();active=-1;
                    options.forEach((item,index)=>{
                        const button=document.createElement('button');button.type='button';button.tabIndex=-1;
                        button.id=menu.id+'-'+index;button.className='list-group-item list-group-item-action text-start';
                        button.textContent=item.email+(item.fullName?' · '+item.fullName:'');
                        button.setAttribute('role','option');button.setAttribute('aria-selected','false');
                        button.addEventListener('mousedown',event=>event.preventDefault());
                        button.addEventListener('pointerdown',event=>event.preventDefault());
                        button.addEventListener('click',()=>choose(index));menu.appendChild(button);
                    });
                    if(!options.length) {
                        const empty=document.createElement('div');empty.className='list-group-item text-muted';
                        empty.textContent='Không có email phù hợp.';empty.setAttribute('role','status');menu.appendChild(empty);
                    }
                    menu.hidden=false;input.setAttribute('aria-expanded','true');
                } catch(error) {
                    if(own!==version || !focused)return;
                    menu.replaceChildren();const notice=document.createElement('div');notice.className='list-group-item text-muted';
                    notice.textContent='Chưa tải được gợi ý. Bạn vẫn có thể nhập email đầy đủ.';notice.setAttribute('role','status');
                    menu.appendChild(notice);menu.hidden=false;input.setAttribute('aria-expanded','true');
                }
            },250);
        });
        input.addEventListener('keydown',event=>{
            if(event.key==='Escape'){invalidate();return;}
            if(menu.hidden || !options.length)return;
            if(event.key==='ArrowDown' || event.key==='ArrowUp') {
                event.preventDefault();highlight((active+(event.key==='ArrowDown'?1:active<0?0:-1)+options.length)%options.length);
            } else if(event.key==='Enter' && active>=0) {
                event.preventDefault();event.stopImmediatePropagation();choose(active);
            } else if(event.key==='Tab')invalidate();
        },true);
        if(input.form)input.form.addEventListener('reset',invalidate);
    }
    window.EmailAutocomplete = {attach};
    function init() {
        document.querySelectorAll('[data-email-suggest]').forEach(input=>attach(input,{
            context:input.dataset.context||'',scope:input.dataset.emailSuggest||'enrollment',multiple:input.dataset.emailSuggestMultiple==='true'
        }));
    }
    if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
