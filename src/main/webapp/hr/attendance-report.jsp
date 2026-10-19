<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- Ban goc cua file nay (commit e9f3b15) la BA phien ban cua cung mot trang dan lien
     nhau, khong ban nao duoc xoa:
       - dong 1-6   : stub "API ... chua duoc cung cap"
       - dong 9-11  : stub thu hai, gan y het
       - dong 13-15 : trang day du (form + fetch + parse HTML)
     Da gop lai thanh mot ban duy nhat, lay ban day du lam goc.

     Khac ban goc:
       - Ban goc fetch /hr/attendance roi PARSE HTML di tim the [data-attendance-json],
         va nem loi neu khong thay. Nay xin thang JSON bang ?format=json (duong thu nhat
         trong handoff FE2) nen khong phai parse HTML nua.
       - Bo loc "Trang thai" cua ban goc la UI chet: AttendanceServlet KHONG doc tham so
         status. Nay loc ngay o client tren du lieu da tai ve.
       - Them esc() cho moi gia tri do BE tra ve truoc khi noi vao HTML.
       - Them dong trang thai rong va thong bao loi that su. --%>

<c:set var="pageTitle" value="Báo cáo chuyên cần" />
<%-- Dung chung key voi trang bang /hr/attendance: sidebar khong co muc rieng cho
     bao cao chuyen can, va hai trang nay la hai cach xem cua cung mot so lieu.
     Dat key rieng 'attendance-report' thi khong muc nao sang. --%>
<c:set var="activePage" value="hr-attendance" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid py-4">

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
            <h2 class="h4 mb-1">Báo cáo chuyên cần</h2>
            <p class="text-muted mb-0">Theo dõi đi làm, nghỉ phép, vắng và thiếu check-out.</p>
        </div>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/hr/attendance">
            Xem dạng bảng
        </a>
    </div>

    <form id="f" class="card card-body mb-4">
        <div class="row g-3 align-items-end">
            <div class="col-sm-6 col-lg-2">
                <label class="form-label" for="fFrom">Từ ngày</label>
                <input class="form-control" id="fFrom" name="from" type="date" required>
            </div>
            <div class="col-sm-6 col-lg-2">
                <label class="form-label" for="fTo">Đến ngày</label>
                <input class="form-control" id="fTo" name="to" type="date" required>
            </div>
            <div class="col-sm-6 col-lg-2">
                <label class="form-label" for="fProgram">Mã chương trình</label>
                <input class="form-control" id="fProgram" name="programId" type="number" min="1">
            </div>
            <div class="col-sm-6 col-lg-3" data-email-selection="enrollment" data-context="${ctx}" data-optional="true" data-email-name="email" data-program-target="fProgram">
                <input id="fEnroll" name="enrollmentId" type="hidden" data-email-value>
            </div>
            <div class="col-sm-6 col-lg-2">
                <label class="form-label" for="fStatus">Trạng thái</label>
                <select class="form-select" id="fStatus" name="status">
                    <option value="">Tất cả</option>
                    <option value="PRESENT">Đi làm</option>
                    <option value="MISSING_CHECKOUT">Thiếu check-out</option>
                    <option value="LEAVE">Nghỉ phép</option>
                    <option value="ABSENT">Vắng mặt</option>
                </select>
            </div>
            <div class="col-sm-6 col-lg-2">
                <button class="btn btn-primary w-100" type="submit">
                    <i class="bi bi-search me-1"></i>Lọc
                </button>
            </div>
        </div>
        <div class="form-text mt-2">
            Khoảng tra cứu tối đa 367 ngày mỗi lần.
        </div>
        <div class="d-flex gap-2 mt-3"><button type="button" class="btn btn-outline-success" data-export-format="xlsx">Xuất Excel</button><button type="button" class="btn btn-outline-danger" data-export-format="pdf">Xuất PDF</button></div>
    </form>

    <div id="err" class="alert alert-danger d-none" role="alert"></div>

    <div class="card">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>Ngày</th>
                        <th>Thực tập sinh</th>
                        <th>Chương trình</th>
                        <th>Check-in</th>
                        <th>Check-out</th>
                        <th>Giờ làm</th>
                        <th>Trạng thái</th>
                    </tr>
                </thead>
                <tbody id="rows"></tbody>
            </table>
        </div>
    </div>

</div>

<script src="${ctx}/assets/js/email-selection.js?v=20261019-email-suggest" defer></script>
<script>
    (() => {
        const ctx = '${pageContext.request.contextPath}';
        const form = document.getElementById('f');
        const rows = document.getElementById('rows');
        const err = document.getElementById('err');
        const statusSelect = document.getElementById('fStatus');

        const STATUS = {
            PRESENT: ['Đi làm', 'text-bg-success'],
            MISSING_CHECKOUT: ['Thiếu check-out', 'text-bg-danger'],
            LEAVE: ['Nghỉ phép', 'text-bg-info'],
            ABSENT: ['Vắng mặt', 'text-bg-warning']
        };

        // Moi gia tri do BE tra ve deu phai qua day truoc khi noi vao HTML.
        const esc = (v) => String(v ?? '—')
            .replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

        let loaded = [];
        let internEmailMap = {};

        function paint() {
            const want = statusSelect.value;
            const shown = want ? loaded.filter((x) => x.status === want) : loaded;

            if (!shown.length) {
                rows.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-5">'
                    + (loaded.length ? 'Không có dòng nào khớp bộ lọc trạng thái.'
                                     : 'Không có dữ liệu chấm công trong khoảng đã chọn.')
                    + '</td></tr>';
                return;
            }

            rows.innerHTML = shown.map((x) => {
                const [label, cls] = STATUS[x.status] || [x.status, 'text-bg-secondary'];
                const out = x.status === 'MISSING_CHECKOUT'
                    ? '<b class="text-danger">Chưa check-out</b>'
                    : esc(x.checkOutTime);
                const email = internEmailMap[x.enrollmentId];
                const subText = email
                    ? '<i class="bi bi-envelope me-1"></i>' + esc(email)
                    : ('Ghi danh #' + esc(x.enrollmentId));
                return '<tr>'
                    + '<td>' + esc(x.workDate) + '</td>'
                    + '<td><b>' + esc(x.internName) + '</b>'
                        + '<div class="small text-muted">' + subText + '</div></td>'
                    + '<td>' + esc(x.programName) + '</td>'
                    + '<td>' + esc(x.checkInTime) + '</td>'
                    + '<td>' + out + '</td>'
                    + '<td>' + Number(x.workedHours || 0).toFixed(2) + '</td>'
                    + '<td><span class="badge ' + cls + '">' + esc(label) + '</span></td>'
                    + '</tr>';
            }).join('');
        }

        function fail(message) {
            loaded = [];
            rows.innerHTML = '';
            err.textContent = message;
            err.classList.remove('d-none');
        }

        async function load() {
            const params = new URLSearchParams(new FormData(form));
            const identity=(params.get('enrollmentId')||'').trim();
            if(identity.includes('@')) {params.delete('enrollmentId');params.set('email',identity);}
            params.delete('status');        // BE khong loc theo trang thai, loc o client
            params.set('format', 'json');   // duong thu nhat trong handoff FE2

            try {
                const res = await fetch(ctx + '/hr/attendance?' + params, {
                    credentials: 'same-origin',
                    headers: { 'Accept': 'application/json' }
                });
                const type = res.headers.get('content-type') || '';
                if (res.redirected || type.indexOf('application/json') === -1) {
                    // Chua chac da het phien (co the la trang loi HTML), nhung khong doc duoc JSON.
                    throw new Error('Máy chủ không trả về JSON (mã ' + res.status
                        + '). Phiên đăng nhập có thể đã hết hạn, hoặc khoảng ngày không hợp lệ.');
                }
                const body = await res.json();
                if (!res.ok) {
                    throw new Error(body && body.error ? body.error : ('Lỗi HTTP ' + res.status));
                }
                loaded = Array.isArray(body) ? body : [];
                err.classList.add('d-none');
                paint();
            } catch (e) {
                fail(e.message || 'Không tải được báo cáo.');
            }
        }

        form.addEventListener('submit', (e) => {
            e.preventDefault();
            if (form.reportValidity()) load();
        });
        statusSelect.addEventListener('change', paint);
        document.querySelectorAll('[data-export-format]').forEach(button=>button.addEventListener('click',async()=>{
            button.disabled=true;
            try {
                if(!form.reportValidity())return;
                const params=new URLSearchParams(new FormData(form));
                const days=(Date.parse(params.get('to'))-Date.parse(params.get('from')))/86400000;
                if(!Number.isFinite(days)||days<0||days>=366)throw Error('Khoảng xuất file phải từ 1 đến 366 ngày.');
                if(params.get('status'))throw Error('Chọn trạng thái Tất cả trước khi xuất file.');
                params.delete('status');
                const identity=(params.get('enrollmentId')||'').trim();
                if(identity.includes('@')) {
                    const lookup=await fetch(ctx+'/api/hr/work-schedules?action=enrollments&email='+encodeURIComponent(identity),{credentials:'same-origin'});
                    if(!lookup.ok||lookup.redirected||!(lookup.headers.get('content-type')||'').includes('application/json'))throw Error('Không thể xác định ghi danh theo email.');
                    const body=await lookup.json();if(!body.success||!Array.isArray(body.data))throw Error('Không thể xác định ghi danh theo email.');
                    const program=params.get('programId');
                    const choices=body.data.filter(entry=>!program||String(entry.programId)===program);
                    if(choices.length!==1)throw Error('Email có nhiều hoặc chưa có ghi danh phù hợp. Chọn mã ghi danh/chương trình cụ thể trước khi xuất.');
                    params.set('enrollmentId',String(choices[0].enrollmentId));
                }
                ['enrollmentId','programId'].forEach(name=>{const value=params.get(name);if(value&&!/^[1-9]\d*$/.test(value))throw Error('Mã chương trình và ghi danh phải là số nguyên dương.');});
                const format=button.dataset.exportFormat;params.set('format',format);
                const response=await fetch(ctx+'/hr/be4/attendance/export?'+params,{credentials:'same-origin'});
                const type=response.headers.get('content-type')||'';
                const expected=format==='pdf'?'application/pdf':'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
                if(!response.ok||response.redirected||!type.includes(expected))throw Error('Không thể xuất báo cáo. Kiểm tra phiên đăng nhập, dữ liệu và cấu hình PDF.');
                const url=URL.createObjectURL(await response.blob());const link=document.createElement('a');
                link.href=url;link.download='bao-cao-chuyen-can.'+format;document.body.appendChild(link);link.click();link.remove();
                setTimeout(()=>URL.revokeObjectURL(url),30000);err.classList.add('d-none');
            } catch(error) {err.textContent=error.message;err.classList.remove('d-none');}
            finally {button.disabled=false;}
        }));


        // Mac dinh 30 ngay gan nhat, giong mac dinh cua AttendanceService.
        const iso = (d) => new Date(d - d.getTimezoneOffset() * 6e4).toISOString().slice(0, 10);
        const now = new Date();
        document.getElementById('fFrom').value = iso(new Date(now.getTime() - 30 * 864e5));
        document.getElementById('fTo').value = iso(now);

        // Load danh sach email thuc tap sinh
        fetch(ctx + '/api/hr/work-schedules?action=interns')
            .then((r) => r.json())
            .then((data) => {
                if (data && data.success && Array.isArray(data.data)) {
                    data.data.forEach(it => {
                        if (it.enrollmentId && it.email) internEmailMap[it.enrollmentId] = it.email;
                    });
                    paint();
                }
            })
            .catch(() => {});

        load();
    })();
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
