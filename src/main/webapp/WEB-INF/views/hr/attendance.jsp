<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>

<c:set var="pageTitle" value="Báo cáo chấm công (HR)" />
<c:set var="activePage" value="hr-attendance" />

<%-- Include TINH, khong dung <jsp:include>: header.jsp doc pageTitle/activePage
     bang <c:set> o PAGE SCOPE. Include dong tao PageContext moi nen khong thay hai
     bien nay, tieu de roi ve mac dinh "He thong quan ly thuc tap" va sidebar khong
     highlight duoc muc nao. footer.jsp mo ta dung cai bay nay o dau file.

     Truong java.time (workDate/startTime/checkInTime...) khong qua duoc
     fmt:formatDate vi JSTL chi nhan java.util.Date, nen hien bang <c:out>; rieng
     LocalDateTime boc 'T' bang fn:replace theo dung loi admin/accounts.jsp:109.

     contentType phai ghi Y HET chuoi cua header.jsp ("text/html; charset=UTF-8",
     co dau cach sau dau cham phay). Include tinh gop ca hai vao mot trang nen Jasper
     so chuoi truc tiep; lech mot ky tu la bao "illegal to have multiple occurrences
     of 'contentType' with different values" va ca trang do 500. Ban goc cua file nay
     khong dinh loi nay chi vi no dung <jsp:include> (dong). --%>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid py-4">

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
        <h2 class="mb-0"><i class="bi bi-clock-history me-2"></i> Báo cáo chấm công</h2>
        <%-- Loi vao trang bao cao chuyen can. Truoc day trang do chi vao duoc bang
             cach go tay URL: khong sidebar, khong trang nao link toi. --%>
        <a class="btn btn-outline-secondary" href="${ctx}/hr/attendance-report.jsp">
            <i class="bi bi-table me-1"></i>Xem báo cáo chuyên cần
        </a>
    </div>

    <c:if test="${not empty param.error}">
        <div class="alert alert-danger"><c:out value="${param.error}"/></div>
    </c:if>
    <c:if test="${not empty warning}">
        <div class="alert alert-warning alert-dismissible fade show mb-4" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i><c:out value="${warning}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <div class="card mb-4">
        <div class="card-body">
            <form action="${ctx}/hr/attendance" method="get" class="row g-3"
                  id="attendanceFilter" novalidate>
                <div class="col-md-6" data-email-selection="enrollment" data-context="${ctx}" data-optional="true"
                     data-email-name="email" data-email="<c:out value='${internEmail != null ? internEmail : param.email}'/>">
                    <input type="hidden" id="fEnrollment" name="enrollmentId" data-email-value
                           value="<c:out value='${enrollmentId != null ? enrollmentId : param.enrollmentId}'/>">
                </div>
                <div class="col-md-2">
                    <label class="form-label" for="fFrom">Từ ngày</label>
                    <input type="date" id="fFrom" name="from" class="form-control"
                           value="<c:out value='${param.from}'/>">
                </div>
                <div class="col-md-2">
                    <label class="form-label" for="fTo">Đến ngày</label>
                    <input type="date" id="fTo" name="to" class="form-control"
                           value="<c:out value='${param.to}'/>">
                    <div class="invalid-feedback" id="fToFeedback"></div>
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary w-100"><i class="bi bi-search"></i> Xem</button>
                </div>
            </form>

            <%-- AttendanceService.getReport nem IllegalArgumentException cho 2 truong
                 hop nhap lieu sai (from > to, va khoang > 367 ngay). AttendanceServlet
                 (dong 102) bat chung voi SQLException roi forward sang /error/500.jsp,
                 nen truoc day nguoi dung chi nhan duoc trang "500 - Loi he thong" -
                 mot loi nhap lieu bi bao nham thanh loi he thong. Chan tai day de
                 khong bao gio gui request sai di; phia BE van con nguyen luat. --%>
            <div class="alert alert-warning d-none mt-3 mb-0" id="rangeWarning" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i>
                <span id="rangeWarningText"></span>
            </div>
        </div>
    </div>

    <div class="card">
        <div class="card-body">
            <div class="table-responsive">
                <table class="table table-bordered table-striped align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th>TTS</th>
                            <th>Chương trình</th>
                            <th>Ngày</th>
                            <th>Ca làm</th>
                            <th>Check-in</th>
                            <th>Check-out</th>
                            <th>Trạng thái</th>
                            <th>Giờ làm (h)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty report}">
                                <tr>
                                    <td colspan="8" class="text-center text-muted py-4">
                                        Không có dữ liệu chấm công.
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="row" items="${report}">
                                    <tr>
                                        <td>
                                            <c:out value="${row.internName}"/> (E:#<c:out value="${row.enrollmentId}"/>)
                                        </td>
                                        <td><c:out value="${row.programName}"/></td>
                                        <td><c:out value="${row.workDate}"/></td>
                                        <td><c:out value="${row.startTime}"/> - <c:out value="${row.endTime}"/></td>
                                        <td><c:out value="${fn:replace(row.checkInTime, 'T', ' ')}"/></td>
                                        <td><c:out value="${fn:replace(row.checkOutTime, 'T', ' ')}"/></td>
                                        <%-- Nhan tieng Viet, gia tri gui cho BE van la
                                             PRESENT/ABSENT/LEAVE/MISSING_CHECKOUT. Dung
                                             cung bo tu voi luoi loc cua bao cao chuyen can. --%>
                                        <td>
                                            <c:choose>
                                                <c:when test="${row.status eq 'PRESENT'}">
                                                    <span class="badge text-bg-success">Đi làm</span>
                                                </c:when>
                                                <c:when test="${row.status eq 'LEAVE'}">
                                                    <span class="badge text-bg-info">Nghỉ phép</span>
                                                </c:when>
                                                <c:when test="${row.status eq 'ABSENT'}">
                                                    <span class="badge text-bg-warning">Vắng mặt</span>
                                                </c:when>
                                                <c:when test="${row.status eq 'MISSING_CHECKOUT'}">
                                                    <span class="badge text-bg-danger">Thiếu check-out</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge text-bg-secondary"><c:out value="${row.status}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><fmt:formatNumber value="${row.workedHours}" maxFractionDigits="2"/></td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

</div>

<%-- Duong lay du lieu thu 3 trong handoff FE2: doc thang tu the nay khi tai trang HTML.
     Noi dung do AttendanceServlet dung san (cung chuoi no tra khi xin ?format=json nen
     hai duong luon khop nhau). Servlet da escape moi '<' thanh < nen chuoi JSON
     khong the dong the script som - vi vay o day in tho, KHONG escape lai.
     EL khong tu escape, va dieu do la co y o day. --%>
<script id="attendance-data" type="application/json" data-attendance-json>${empty attendanceJson ? '[]' : attendanceJson}</script>

<script src="${ctx}/assets/js/email-selection.js?v=20261019-email-suggest" defer></script>
<script>
    (() => {
        const form = document.getElementById('attendanceFilter'),
            from = document.getElementById('fFrom'),
            to = document.getElementById('fTo'),
            warn = document.getElementById('rangeWarning'),
            warnText = document.getElementById('rangeWarningText');

        // Khong dung <input min/max>: tran la KHOANG chu khong phai mot moc ngay,
        // va moc 'to' phu thuoc 'from' nen phai tinh luc submit.
        const MAX_DAYS = 367;
        const DAY_MS = 86400000;

        function fail(el, msg) {
            warnText.textContent = msg;
            warn.classList.remove('d-none');
            el.classList.add('is-invalid');
            el.focus();
        }

        function clear() {
            warn.classList.add('d-none');
            from.classList.remove('is-invalid');
            to.classList.remove('is-invalid');
        }

        form.addEventListener('submit', (e) => {
            clear();
            if (!from.value || !to.value) return;   // BE tu ap mac dinh 30 ngay

            // Ca hai deu la 'YYYY-MM-DD' nen Date.parse ra nua dem UTC; hieu so
            // chia DAY_MS la so ngay duong lich, khong lech vi mui gio.
            const a = Date.parse(from.value), b = Date.parse(to.value);
            if (a > b) {
                e.preventDefault();
                fail(from, 'Từ ngày không được sau Đến ngày.');
                return;
            }
            const days = Math.round((b - a) / DAY_MS);
            if (days > MAX_DAYS) {
                e.preventDefault();
                fail(to, 'Khoảng tra cứu là ' + days + ' ngày, vượt mức ' + MAX_DAYS
                    + ' ngày. Vui lòng chia nhỏ khoảng thời gian.');
            }
        });

        [from, to].forEach(el => el.addEventListener('input', clear));
    })();
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
