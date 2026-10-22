(function () {
    'use strict';
    const root = document.getElementById('fe3Sprint3');
    if (!root) return;
    const ctx = root.dataset.context || '';
    const byId = id => document.getElementById(id);
    const node = (tag, text, cls) => { const e = document.createElement(tag); if (text != null) e.textContent = String(text); if (cls) e.className = cls; return e; };
    const notice = (message, success) => { const box = byId('s3Notice'); box.textContent = message; box.className = 'alert alert-' + (success ? 'success' : 'danger'); };
    const stamp = value => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(/Z$|[+-]\d\d:\d\d$/.test(value) ? value : value + 'Z')) : 'Chưa có';
    async function json(path, params, body) {
        const options = { credentials: 'same-origin', headers: { Accept: 'application/json' } };
        if (body) { options.method = 'POST'; options.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8'; options.body = new URLSearchParams(body); }
        const response = await fetch(ctx + path + (params ? '?' + new URLSearchParams(params) : ''), options);
        if (response.redirected || !(response.headers.get('content-type') || '').includes('application/json')) throw new Error('Phiên đăng nhập đã hết hạn. Đăng nhập lại rồi thử lại.');
        const data = await response.json();
        if (!response.ok) throw new Error(typeof data.error === 'string' ? data.error : data.error && data.error.message || 'Không xử lý được yêu cầu (' + response.status + ').');
        return data;
    }
    const values = form => Object.fromEntries(new FormData(form).entries());
    const cleanFilter = form => Object.fromEntries(Object.entries(values(form)).filter(([, v]) => v !== ''));
    const error = e => notice(e.message || 'Không thể tải dữ liệu.');

    async function reports() {
        let page = 1, filters = {}, applied = null, sequence = 0;
        const buttons = ['exportExcel', 'exportPdf', 'reportPrevious', 'reportNext'];
        const disable = flag => buttons.forEach(id => { byId(id).disabled = flag; });
        function showHistory(row) {
            const section = byId('assessmentHistory'), body = byId('assessmentHistoryRows');
            body.replaceChildren(node('p', row.internName + ' · Ghi danh #' + row.enrollmentId));
            if (!row.assessments.length) body.appendChild(node('p', 'Chưa có đánh giá hai tiêu chí.'));
            row.assessments.forEach((a, i) => {
                const card = node('article', null, 'fe3-feedback');
                card.appendChild(node('strong', (i === 0 ? 'Mới nhất · ' : '') + a.mentorName + ' · ' + stamp(a.createdAt)));
                card.appendChild(node('p', 'Kỹ năng: ' + a.skillsScore + '/5 · Thái độ: ' + a.attitudeScore + '/5'));
                card.appendChild(node('p', a.comment, 'fe3-report-content')); body.appendChild(card);
            });
            if (row.legacyScore != null) body.appendChild(node('p', 'Điểm cũ: ' + row.legacyScore + '/100. Không quy đổi sang tiêu chí /5.'));
            section.hidden = false; section.focus();
        }
        async function reload() {
            const own = ++sequence, requested = { ...filters }, requestedPage = page;
            disable(true); applied = null;
            byId('s3Notice').textContent = ''; byId('s3Notice').className = 'alert d-none';
            const body = byId('endTermRows'); body.replaceChildren(); const loading = node('tr'); const cell = node('td', 'Đang tải…'); cell.colSpan = 7; loading.appendChild(cell); body.appendChild(loading);
            byId('assessmentHistory').hidden = true;
            try {
                const result = await json('/hr/be4/end-term', { ...requested, page: requestedPage, size: 20 });
                if (own !== sequence) return;
                body.replaceChildren();
                if (!result.items.length) { const row = node('tr'), c = node('td', 'Không có ghi danh phù hợp bộ lọc.'); c.colSpan = 7; row.appendChild(c); body.appendChild(row); }
                result.items.forEach(item => {
                    const row = node('tr');
                    const latest = item.assessments[0];
                    const texts = [item.internName + ' · #' + item.enrollmentId, item.programName + ' · #' + item.programId + ' · ' + item.status + '\n' + item.startDate + ' → ' + item.endDate,
                        (item.university || 'Chưa khai báo') + '\n' + (item.major || 'Chưa khai báo'), item.tasksDone + '/' + item.taskCount,
                        item.submittedReports + ' đã nộp · ' + item.reviewedReports + ' đã phản hồi', latest ? 'Kỹ năng ' + latest.skillsScore + '/5 · Thái độ ' + latest.attitudeScore + '/5\n' + latest.mentorName : 'Chưa đánh giá'];
                    texts.forEach(text => { const c = node('td', text); c.style.whiteSpace = 'pre-line'; row.appendChild(c); });
                    const action = node('td'), button = node('button', 'Xem lịch sử', 'btn btn-outline-primary btn-sm'); button.type = 'button'; button.addEventListener('click', () => showHistory(item)); action.appendChild(button); row.appendChild(action); body.appendChild(row);
                });
                applied = requested; disable(false); byId('reportPrevious').disabled = requestedPage === 1; byId('reportNext').disabled = !result.hasMore;
                byId('reportPage').textContent = 'Trang ' + requestedPage;
                byId('reportSnapshot').textContent = 'Dữ liệu lúc ' + stamp(result.generatedAt) + '. Bộ lọc: chương trình ' + (requested.programId || 'tất cả') + ', ghi danh ' + (requested.enrollmentId || 'tất cả') + ', trạng thái ' + (requested.status || 'tất cả') + '.';
            } catch (e) { if (own === sequence) { body.replaceChildren(); error(e); } }
        }
        byId('endTermFilter').addEventListener('submit', event => { event.preventDefault(); filters = cleanFilter(event.currentTarget); page = 1; reload(); });
        byId('reportPrevious').addEventListener('click', () => { if (page > 1) { page--; reload(); } });
        byId('reportNext').addEventListener('click', () => { page++; reload(); });
        async function download(format) {
            if (!applied) throw new Error('Hãy tải kết quả hợp lệ trước khi xuất.');
            const response = await fetch(ctx + '/hr/be4/end-term/export?' + new URLSearchParams({ ...applied, format }), { credentials: 'same-origin' });
            const mime = response.headers.get('content-type') || '';
            if (response.redirected) throw new Error('Phiên đăng nhập hết hạn.');
            if (!response.ok || !mime.includes(format === 'pdf' ? 'application/pdf' : 'spreadsheetml.sheet')) {
                const data = mime.includes('application/json') ? await response.json() : null;
                throw new Error(data && (typeof data.error === 'string' ? data.error : data.error && data.error.message) || 'Không thể tải file báo cáo.');
            }
            const url = URL.createObjectURL(await response.blob()), a = node('a'); a.href = url; a.download = 'bao-cao-cuoi-ky.' + format; a.click(); setTimeout(() => URL.revokeObjectURL(url), 30000);
            notice('Đã tạo file theo bộ lọc đang hiển thị. File lấy dữ liệu mới tại thời điểm xuất.', true);
        }
        ['Excel', 'Pdf'].forEach(kind => byId('export' + kind).addEventListener('click', async event => { const button = event.currentTarget; button.disabled = true; try { await download(kind === 'Excel' ? 'xlsx' : 'pdf'); } catch (e) { error(e); } finally { button.disabled = !applied; } }));
        await reload();
    }

    async function meetings() {
        const form = byId('meetingForm'); let participants = [], editing = null, token = null, page = 1;
        const local = iso => new Date(new Date(iso).getTime() + 7 * 3600000).toISOString().slice(0, 16);
        const instant = value => new Date(value + ':00+07:00').toISOString();
        function choices(selected = []) {
            const target = byId('meetingParticipants'); target.replaceChildren();
            const available = participants.filter(p => String(p.programId) === byId('meetingProgram').value);
            if (!available.length) target.textContent = 'Chưa có TTS ACTIVE trong chương trình này.';
            available.forEach(p => { const label = node('label', null, 'd-block mb-2'), input = node('input'); input.type = 'checkbox'; input.name = 'participant'; input.value = p.enrollmentId; input.checked = selected.includes(p.enrollmentId); input.className = 'form-check-input me-2'; label.appendChild(input); label.appendChild(node('span', p.internName + ' · Ghi danh #' + p.enrollmentId)); target.appendChild(label); });
        }
        function reset() { editing = null; form.reset(); byId('meetingProgram').disabled = false; byId('meetingFormTitle').textContent = 'Tạo cuộc họp'; choices(); }
        async function reload(append = false) {
            const data = await json('/meetings', { page, size: 20 }); token = data.csrfToken;
            const list = byId('meetingList'); if (!append) list.replaceChildren();
            if (!data.items.length && page === 1) list.appendChild(node('p', 'Chưa có cuộc họp trong phạm vi phụ trách.'));
            data.items.forEach(item => {
                const card = node('article', null, 'fe3-feedback'); card.appendChild(node('strong', item.title)); card.appendChild(node('p', stamp(item.startsAt) + ' – ' + stamp(item.endsAt) + ' · ' + item.location)); card.appendChild(node('p', item.status === 'CANCELLED' ? 'Đã hủy' : 'Đã lên lịch'));
                if (item.status === 'SCHEDULED' && String(item.organizerUserId) === root.dataset.userId) {
                    const edit = node('button', 'Chỉnh sửa', 'btn btn-outline-primary btn-sm me-2'); edit.type = 'button'; edit.addEventListener('click', async () => { try { const detail = await json('/meetings', { id: item.id }); token = detail.csrfToken; editing = detail.item; const current = editing; byId('meetingProgram').value = current.programId; byId('meetingProgram').disabled = true; byId('meetingTitle').value = current.title; byId('meetingLocation').value = current.location; byId('meetingDescription').value = current.description || ''; byId('meetingStart').value = local(current.startsAt); byId('meetingEnd').value = local(current.endsAt); choices(current.enrollmentIds); byId('meetingFormTitle').textContent = 'Cập nhật cuộc họp #' + current.id; byId('meetingTitle').focus(); } catch (e) { error(e); } });
                    const cancel = node('button', 'Hủy họp', 'btn btn-outline-danger btn-sm'); cancel.type = 'button'; cancel.addEventListener('click', async () => { if (!window.confirm('Hủy cuộc họp “' + item.title + '”? Lịch sử vẫn được giữ.')) return; cancel.disabled = true; try { const detail = await json('/meetings', { id: item.id }); await json('/meetings', null, { action: 'cancel', id: detail.item.id, revision: detail.item.revision, csrfToken: detail.csrfToken }); page = 1; await reload(); notice('Đã hủy cuộc họp.', true); } catch (e) { error(e); cancel.disabled = false; } }); card.appendChild(edit); card.appendChild(cancel);
                }
                list.appendChild(card);
            }); byId('meetingMore').hidden = data.items.length < 20;
        }
        participants = await json('/mentor/be4/meeting-participants');
        const select = byId('meetingProgram'); select.replaceChildren(); const placeholder = node('option', 'Chọn chương trình'); placeholder.value = ''; select.appendChild(placeholder);
        const programs = new Map(participants.map(p => [p.programId, p.programName])); programs.forEach((name, id) => { const option = node('option', name); option.value = id; select.appendChild(option); });
        if (!participants.length) { byId('meetingSave').disabled = true; notice('Chưa được phân TTS ACTIVE; vẫn có thể xem các cuộc họp được phép truy cập.'); }
        select.addEventListener('change', () => choices()); byId('meetingReset').addEventListener('click', reset); byId('meetingReload').addEventListener('click', () => { page = 1; reload().catch(error); }); byId('meetingMore').addEventListener('click', () => { page++; reload(true).catch(error); });
        form.addEventListener('submit', async event => {
            event.preventDefault(); const save = byId('meetingSave'); save.disabled = true;
            try {
                const v = values(form); v.programId = select.value;
                const selected = Array.from(form.querySelectorAll('input[name="participant"]:checked')).map(input => input.value);
                if (!selected.length) throw new Error('Chọn ít nhất một TTS đang phụ trách.');
                v.startsAt = instant(v.startsAt); v.endsAt = instant(v.endsAt); if (new Date(v.endsAt) <= new Date(v.startsAt)) throw new Error('Kết thúc phải sau bắt đầu.');
                if (new Date(v.startsAt) <= new Date()) throw new Error('Cuộc họp phải bắt đầu trong tương lai.');
                delete v.participant; v.enrollmentIds = selected.join(','); v.csrfToken = token; v.action = editing ? 'update' : 'create'; if (editing) { v.id = editing.id; v.revision = editing.revision; }
                await json('/meetings', null, v); reset(); page = 1; await reload(); notice('Đã lưu cuộc họp. Thông báo/email do dịch vụ BE1 xử lý.', true);
            } catch (e) { error(e); } finally { save.disabled = !participants.length; }
        });
        await reload();
    }
    (root.dataset.page === 'end-term' ? reports() : meetings()).catch(error);
}());
