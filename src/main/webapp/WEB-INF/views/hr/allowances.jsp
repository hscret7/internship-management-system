<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<c:set var="pageTitle" value="Quản lý phụ cấp" />
<c:set var="activePage" value="hr-allowances" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="h4 mb-1"><i class="bi bi-cash-coin me-2"></i>Quản lý phụ cấp (US25)</h2>
        <p class="text-muted mb-0">Tạo mới khoản phụ cấp cho thực tập sinh và cập nhật trạng thái chi trả.</p>
    </div>
</div>

<c:if test="${not empty error or not empty param.error}">
    <div class="alert alert-danger" role="alert">
        <c:out value="${not empty error ? error : param.error}"/>
    </div>
</c:if>
<c:if test="${param.success eq 'true'}">
    <div class="alert alert-success" role="alert">
        Thao tác thành công.
    </div>
</c:if>

<div class="card mb-4 shadow-sm border-0">
    <div class="card-header bg-white py-3">
        <h5 class="card-title mb-0 fs-6 fw-bold">Tạo mới khoản phụ cấp</h5>
    </div>
    <div class="card-body">
        <form action="${pageContext.request.contextPath}/hr/allowances" method="post" class="row g-3">
            <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
            <input type="hidden" name="action" value="create">
            <div class="col-md-3">
                <label class="form-label" for="newEnrollment">Mã Ghi Danh (Enrollment ID) *</label>
                <input type="number" id="newEnrollment" name="enrollmentId" class="form-control" required min="1" placeholder="VD: 1">
            </div>
            <div class="col-md-3">
                <label class="form-label" for="newType">Loại phụ cấp *</label>
                <select id="newType" name="allowanceType" class="form-select" required>
                    <option value="MEAL">Ăn trưa (MEAL)</option>
                    <option value="TRAVEL">Đi lại (TRAVEL)</option>
                    <option value="SALARY">Lương TTS (SALARY)</option>
                    <option value="OTHER">Khác (OTHER)</option>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="newPeriod">Kỳ áp dụng *</label>
                <input type="text" id="newPeriod" name="period" class="form-control" required pattern="[0-9]{4}-(0[1-9]|1[0-2])" placeholder="YYYY-MM">
                <div class="form-text">VD: 2026-10</div>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="newAmount">Số tiền (VNĐ) *</label>
                <input type="number" id="newAmount" name="amount" class="form-control" required min="0" step="1000" placeholder="0">
            </div>
            <div class="col-md-2 d-flex align-items-end">
                <button type="submit" class="btn btn-primary w-100">
                    <i class="bi bi-plus-circle me-1"></i>Tạo mới
                </button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow-sm border-0">
    <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
        <h5 class="card-title mb-0 fs-6 fw-bold">Danh sách phụ cấp</h5>
        <form action="${pageContext.request.contextPath}/hr/allowances" method="get" class="d-flex gap-2">
            <input type="number" name="enrollmentId" class="form-control form-control-sm" style="max-width: 220px;" placeholder="Lọc theo Enrollment ID" value="<c:out value='${param.enrollmentId}'/>">
            <button class="btn btn-outline-secondary btn-sm" type="submit"><i class="bi bi-funnel"></i> Lọc</button>
        </form>
    </div>
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>ID</th>
                        <th>TTS</th>
                        <th>Kỳ</th>
                        <th>Loại</th>
                        <th>Số tiền (VNĐ)</th>
                        <th>Trạng thái</th>
                        <th>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${empty allowances}">
                            <tr><td colspan="7" class="text-center text-muted py-5"><i class="bi bi-cash-stack fs-2 d-block mb-2"></i>Không có dữ liệu phụ cấp.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="a" items="${allowances}">
                                <tr>
                                    <td>#<c:out value="${a.allowanceId}"/></td>
                                    <td>
                                        <strong><c:out value="${not empty a.internName ? a.internName : 'TTS'}" /></strong>
                                        <div class="small text-muted">#<c:out value="${a.enrollmentId}" /></div>
                                    </td>
                                    <td><c:out value="${a.period}" /></td>
                                    <td><c:out value="${a.allowanceType}" /></td>
                                    <td><fmt:formatNumber value="${a.amount}" type="number" maxFractionDigits="0" /> VNĐ</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${a.status eq 'PAID'}">
                                                <span class="badge text-bg-success">Đã chi</span>
                                                <c:if test="${not empty a.paidAt}">
                                                    <small class="d-block text-muted"><c:out value="${a.paidAt}" /></small>
                                                </c:if>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge text-bg-warning">Chờ thanh toán</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:if test="${a.status ne 'PAID'}">
                                            <form action="${pageContext.request.contextPath}/hr/allowances" method="post" class="d-inline" onsubmit="return confirm('Xác nhận đánh dấu khoản phụ cấp này đã chi?');">
                                                <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
                                                <input type="hidden" name="action" value="update_status">
                                                <input type="hidden" name="allowanceId" value="${a.allowanceId}">
                                                <input type="hidden" name="status" value="PAID">
                                                <button type="submit" class="btn btn-sm btn-success">
                                                    <i class="bi bi-check-circle me-1"></i>Đã chi
                                                </button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
