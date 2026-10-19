<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- Noi dung duoc chuyen nguyen khoi tu src/main/webapp/hr/allowances.jsp - mot ban
     markup da viet xong nhung khong servlet nao forward toi (AllowanceServlet chi
     tra ve /WEB-INF/views/hr/allowances.jsp), nen khong bao gio chay. Ban o day la
     ban duy nhat con lai.

     Chi khac ban goc 2 cho:
       - activePage: 'allowances' -> 'hr-allowances' cho khop dieu kien highlight
         cua sidebar.jsp.
       - a.paidAt boc 'T' bang fn:replace, theo loi admin/accounts.jsp:109: JSTL
         fmt:formatDate chi nhan java.util.Date con paidAt la LocalDateTime. --%>

<c:set var="pageTitle" value="Quản lý phụ cấp" />
<c:set var="activePage" value="hr-allowances" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<style>
    .hr-allowance .panel{border:0;border-radius:1rem;box-shadow:0 .35rem 1.2rem #172b4d12}
    .hr-allowance .table td{vertical-align:middle}
    .money{white-space:nowrap}
    @media(max-width:575.98px){.hr-allowance .table{min-width:760px}.hr-allowance .btn{min-height:2.5rem}}
</style>

<div class="container-fluid py-4 hr-allowance" id="allowanceApp">

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
            <h2 class="h4 mb-1">Quản lý phụ cấp</h2>
            <p class="text-muted mb-0">Tạo phụ cấp theo kỳ và đánh dấu đã chi.</p>
        </div>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/hr/allowances">Làm mới</a>
    </div>

    <c:if test="${not empty param.error}">
        <div class="alert alert-danger"><c:out value="${param.error}"/></div>
    </c:if>
    <c:if test="${param.success eq 'true'}">
        <div class="alert alert-success">Đã cập nhật phụ cấp thành công.</div>
    </c:if>

    <div class="row g-4">

        <div class="col-12 col-xl-4">
            <section class="card panel h-100">
                <div class="card-body p-4">
                    <h3 class="h5">Nhập phụ cấp</h3>
                    <p class="small text-muted">Số tiền phải là số không âm.</p>

                    <form action="${pageContext.request.contextPath}/hr/allowances" method="post"
                          id="allowanceForm" novalidate>
                        <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
                        <input type="hidden" name="action" value="create">

                        <div class="mb-3" data-email-selection="enrollment" data-context="${ctx}" data-email-name="internEmail">
                            <input type="hidden" id="enrollmentId" name="enrollmentId" data-email-value>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="allowanceType">Loại phụ cấp *</label>
                            <select class="form-select" id="allowanceType" name="allowanceType" required>
                                <option value="MEAL">Ăn trưa</option>
                                <option value="TRAVEL">Đi lại</option>
                                <option value="SALARY">Lương TTS</option>
                                <option value="OTHER">Khác</option>
                            </select>
                        </div>

                        <div class="mb-3">
                            <label class="form-label" for="period">Kỳ áp dụng *</label>
                            <input class="form-control" id="period" name="period"
                                   pattern="[0-9]{4}-(0[1-9]|1[0-2])" placeholder="YYYY-MM" required>
                            <div class="form-text">Ví dụ: 2026-10</div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label" for="amount">Số tiền (VNĐ) *</label>
                            <input class="form-control" id="amount" name="amount"
                                   type="number" min="0" step="1000" required>
                            <div class="invalid-feedback">Số tiền không được âm.</div>
                        </div>

                        <button class="btn btn-primary" type="submit">
                            <i class="bi bi-plus-circle me-1"></i>Tạo phụ cấp
                        </button>
                    </form>
                </div>
            </section>
        </div>

        <div class="col-12 col-xl-8">
            <section class="card panel">
                <div class="card-body p-4">
                    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                        <h3 class="h5 mb-0">Danh sách phụ cấp</h3>
                        <form class="d-flex gap-2" method="get"
                              action="${pageContext.request.contextPath}/hr/allowances">
                            <label class="visually-hidden" for="filterEnrollment">Mã ghi danh</label>
                            <div data-email-selection="enrollment" data-context="${ctx}" data-optional="true"
                                 data-email-name="email" data-email="<c:out value='${param.email}'/>">
                                <input type="hidden" id="filterEnrollment" name="enrollmentId" data-email-value value="<c:out value='${param.enrollmentId}'/>">
                            </div>
                            <button class="btn btn-outline-primary" type="submit">Lọc</button>
                        </form>
                    </div>

                    <div class="table-responsive">
                        <table class="table table-hover align-middle">
                            <thead class="table-light">
                                <tr>
                                    <th>ID</th>
                                    <th>Thực tập sinh / chương trình</th>
                                    <th>Kỳ</th>
                                    <th>Loại</th>
                                    <th>Số tiền</th>
                                    <th>Trạng thái</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${not empty allowances}">
                                        <c:forEach var="a" items="${allowances}">
                                            <tr>
                                                <td>#<c:out value="${a.allowanceId}"/></td>
                                                <td><strong><c:out value="${a.internName}"/></strong><br><span class="text-primary"><c:out value="${a.internEmail}"/></span><br><small><c:out value="${a.programName}"/></small></td>
                                                <td><c:out value="${a.period}"/></td>
                                                <td><c:out value="${a.allowanceType}"/></td>
                                                <td class="money">
                                                    <fmt:formatNumber value="${a.amount}" type="number" maxFractionDigits="0"/> VNĐ
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${a.status eq 'PAID'}">
                                                            <span class="badge text-bg-success">Đã chi</span>
                                                            <c:if test="${not empty a.paidAt}">
                                                                <small class="d-block text-muted"><c:out value="${fn:replace(a.paidAt, 'T', ' ')}"/></small>
                                                            </c:if>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge text-bg-warning">Chưa chi</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <c:if test="${a.status ne 'PAID'}">
                                                        <form method="post"
                                                              action="${pageContext.request.contextPath}/hr/allowances"
                                                              onsubmit="return confirm('Xác nhận đánh dấu khoản phụ cấp này đã chi?');">
                                                            <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
                                                            <input type="hidden" name="action" value="update_status">
                                                            <input type="hidden" name="allowanceId" value="${a.allowanceId}">
                                                            <input type="hidden" name="status" value="PAID">
                                                            <button class="btn btn-sm btn-success" type="submit">Đã chi</button>
                                                        </form>
                                                    </c:if>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr>
                                            <td colspan="7" class="text-center text-muted py-5">
                                                <i class="bi bi-cash-stack fs-2 d-block mb-2"></i>
                                                Chưa có phụ cấp phù hợp.
                                            </td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                </div>
            </section>
        </div>

    </div>
</div>

<script>
    document.getElementById('allowanceForm').addEventListener('submit', function (e) {
        if (!e.currentTarget.reportValidity()) { e.preventDefault(); return; }
        const a = document.getElementById('amount');
        if (Number(a.value) < 0) {
            e.preventDefault();
            a.classList.add('is-invalid');
        }
    });
</script>

<script src="${ctx}/assets/js/email-selection.js?v=20261019-email-suggest" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
