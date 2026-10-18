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
    <section class="card card-body mb-3" data-attendance-tools data-context="${ctx}"
             data-form="f" data-enrollment="fEnroll" data-program="fProgram" aria-label="Tra cứu ghi danh và xuất báo cáo">
        <div class="row g-3 align-items-end">
            <div class="col-md-5"><label class="form-label" for="reportLookupEmail">Email thực tập sinh</label>
                <input id="reportLookupEmail" data-lookup-email class="form-control" type="email" maxlength="254" placeholder="Nhập email đầy đủ" autocomplete="off"></div>
            <div class="col-md-2"><button type="button" data-lookup-button class="btn btn-outline-primary">Tra cứu</button></div>
            <div class="col-md-5"><label class="form-label" for="reportLookupChoices">Ghi danh phù hợp</label>
                <select id="reportLookupChoices" data-lookup-choices class="form-select" disabled><option value="">Chọn ghi danh sau khi tra cứu</option></select></div>
        </div>
        <p class="form-text">Một email có thể có nhiều ghi danh. Chọn đúng chương trình để điền mã vào bộ lọc; có thể nhập mã trực tiếp.</p>
        <div class="d-flex flex-wrap gap-2"><button type="button" data-attendance-export="xlsx" class="btn btn-outline-primary">Xuất Excel</button>
            <button type="button" data-attendance-export="pdf" class="btn btn-outline-primary">Xuất PDF</button></div>
        <p class="form-text mb-0">Xuất theo bộ lọc đang nhập, tối đa 366 ngày tính cả hai đầu. File gồm mọi trạng thái và một dòng cho mỗi ca làm. PDF cần cấu hình font tiếng Việt trên máy chủ.</p>
        <div data-report-message role="status" aria-live="polite" hidden></div>
    </section>


    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
            <h2 class="h4 mb-1">Báo cáo chuyên cần</h2>
            <p class="text-muted mb-0">Theo dõi đi làm, nghỉ phép, vắng và thiếu check-out. Mỗi dòng là một ca làm; không cộng giờ giữa các ca có cùng lần chấm công.</p>
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
            <div class="col-sm-6 col-lg-2">
                <label class="form-label" for="fEnroll">Mã ghi danh</label>
                <input class="form-control" id="fEnroll" name="enrollmentId" type="number" min="1">
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
                        <th>Ca làm</th>
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

        function paint() {
            const want = statusSelect.value;
            const shown = want ? loaded.filter((x) => x.status === want) : loaded;

            if (!shown.length) {
                rows.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-5">'
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
                return '<tr>'
                    + '<td>' + esc(x.workDate) + '</td>'
                    + '<td><b>' + esc(x.internName) + '</b>'
                        + '<div class="small text-muted">Ghi danh #' + esc(x.enrollmentId) + '</div></td>'
                    + '<td>' + esc(x.programName) + '</td>'
                    + '<td>' + esc(x.startTime) + ' – ' + esc(x.endTime) + '</td>'
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
                if (!Array.isArray(body)) throw new Error('Dữ liệu báo cáo trả về không hợp lệ.');
                loaded = body;
                err.classList.add('d-none');
                paint();
            } catch (e) {
                fail(e.message || 'Không tải được báo cáo.');
            }
        }

        form.addEventListener('submit', (e) => {
            e.preventDefault();
            load();
        });
        statusSelect.addEventListener('change', paint);

        // Mac dinh 30 ngay gan nhat, giong mac dinh cua AttendanceService.
        const iso = (d) => new Date(d - d.getTimezoneOffset() * 6e4).toISOString().slice(0, 10);
        const now = new Date();
        document.getElementById('fFrom').value = iso(new Date(now.getTime() - 30 * 864e5));
        document.getElementById('fTo').value = iso(now);

        load();
    })();
</script>

<script src="${ctx}/assets/js/attendance-report-tools.js" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
