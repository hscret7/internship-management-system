/* HR report controls: lookup is exact-email and always requires explicit enrollment selection. */
(() => {
    'use strict';
    const root = document.querySelector('[data-attendance-tools]');
    if (!root) return;
    const ctx = root.dataset.context || '';
    const form = document.getElementById(root.dataset.form);
    const enrollment = document.getElementById(root.dataset.enrollment);
    const program = document.getElementById(root.dataset.program);
    const email = root.querySelector('[data-lookup-email]');
    const choices = root.querySelector('[data-lookup-choices]');
    const lookup = root.querySelector('[data-lookup-button]');
    const message = root.querySelector('[data-report-message]');
    let matches = [];
    let lookupVersion = 0;
    function apiError(body, fallback) {
        if (typeof body?.error?.message === 'string') return body.error.message;
        if (typeof body?.error === 'string') return body.error;
        if (typeof body?.message === 'string') return body.message;
        return fallback;
    }
    function notify(text, error) {
        message.textContent = text;
        message.className = error ? 'alert alert-danger mt-2' : 'alert alert-info mt-2';
        message.hidden = false;
    }
    function invalidateLookup() {
        lookupVersion++;
        matches = [];
        choices.replaceChildren(new Option('Chọn ghi danh sau khi tra cứu', ''));
        choices.disabled = true;
    }
    email.addEventListener('input', invalidateLookup);
    lookup.addEventListener('click', async () => {
        invalidateLookup();
        if (!email.value.trim() || !email.checkValidity()) {
            notify('Nhập đúng địa chỉ email để tra cứu.', true);
            email.reportValidity();
            return;
        }
        const version = lookupVersion;
        lookup.disabled = true;
        try {
            const query = new URLSearchParams({ action: 'enrollments', email: email.value.trim() });
            const response = await fetch(ctx + '/api/hr/work-schedules?' + query, {
                credentials: 'same-origin', headers: { Accept: 'application/json' }
            });
            if (response.redirected || !(response.headers.get('content-type') || '').includes('application/json')) {
                throw new Error('Không đọc được dữ liệu tra cứu. Hãy kiểm tra phiên đăng nhập.');
            }
            const body = await response.json();
            if (!response.ok || !body.success) throw new Error(apiError(body, 'Tra cứu không thành công.'));
            if (version !== lookupVersion) return;
            if (!Array.isArray(body.data)) throw new Error('Dữ liệu ghi danh trả về không hợp lệ.');
            matches = body.data;
            matches.forEach(entry => {
                choices.add(new Option((entry.internName || '') + ' · ' + entry.email + ' · '
                    + entry.programName + ' · #' + entry.enrollmentId + ' · ' + entry.enrollmentStatus,
                    String(entry.enrollmentId)));
            });
            choices.disabled = !matches.length;
            notify(matches.length ? 'Tìm thấy ' + matches.length + ' ghi danh. Chọn đúng chương trình bên dưới; bộ lọc chưa tự thay đổi.'
                : 'Không tìm thấy ghi danh cho email này. Bộ lọc hiện tại được giữ nguyên.', false);
        } catch (error) {
            if (version === lookupVersion) notify(error.message, true);
        } finally { lookup.disabled = false; }
    });
    choices.addEventListener('change', () => {
        const selected = matches.find(entry => String(entry.enrollmentId) === choices.value);
        if (!selected) return;
        enrollment.value = selected.enrollmentId;
        program.value = selected.programId;
        notify('Đã chọn ghi danh #' + selected.enrollmentId + '. Bấm Xem/Lọc để áp dụng.', false);
    });
    root.querySelectorAll('[data-attendance-export]').forEach(button => button.addEventListener('click', async () => {
        button.disabled = true;
        try {
            const filters = new FormData(form);
            const from = filters.get('from'), to = filters.get('to');
            if (!from || !to) throw new Error('Chọn đủ Từ ngày và Đến ngày trước khi xuất file.');
            const days = (Date.parse(to) - Date.parse(from)) / 86400000;
            if (!Number.isFinite(days) || days < 0 || days >= 366) throw new Error('Khoảng xuất file phải từ 1 đến 366 ngày (tính cả hai đầu).');
            if (!form.reportValidity()) return;
            const status = filters.get('status');
            if (status) throw new Error('Xuất file bao gồm mọi trạng thái. Chọn “Tất cả” ở bộ lọc trạng thái trước khi xuất.');
            const format = button.dataset.attendanceExport;
            const query = new URLSearchParams({ from, to, format });
            ['programId', 'enrollmentId'].forEach(name => {
                const value = String(filters.get(name) || '').trim();
                if (value) {
                    if (!/^[1-9]\d*$/.test(value)) throw new Error('Mã chương trình và ghi danh phải là số nguyên dương.');
                    query.set(name, value);
                }
            });
            const response = await fetch(ctx + '/hr/be4/attendance/export?' + query, { credentials: 'same-origin' });
            const type = response.headers.get('content-type') || '';
            if (!response.ok || response.redirected || type.includes('json') || type.includes('text/html')) {
                let detail = 'Không thể xuất báo cáo (HTTP ' + response.status + '). Kiểm tra phiên đăng nhập hoặc cấu hình PDF.';
                if (type.includes('json')) {
                    const body = await response.json();
                    detail = apiError(body, detail);
                }
                throw new Error(detail);
            }
            const expected = format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
            if (!type.includes(expected)) throw new Error('Máy chủ trả về định dạng file không đúng.');
            const url = URL.createObjectURL(await response.blob());
            const link = document.createElement('a');
            link.href = url; link.download = 'bao-cao-chuyen-can.' + format;
            document.body.appendChild(link); link.click(); link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 30000);
            notify('Đã tạo file theo bộ lọc ngày, chương trình và ghi danh hiện tại. Kiểm tra mục tải xuống của trình duyệt.', false);
        } catch (error) { notify(error.message || 'Không xuất được báo cáo.', true); }
        finally { button.disabled = false; }
    }));
})();
