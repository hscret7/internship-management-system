<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<c:set var="pageTitle" value="Chấm công thực tập" />
<c:set var="activePage" value="intern-attendance" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="h4 mb-1"><i class="bi bi-clock-history me-2"></i>Chấm công thực tập (US21)</h2>
        <p class="text-muted mb-0">Thực hiện check-in, check-out hàng ngày và theo dõi lịch sử chuyên cần (US22).</p>
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
        <h5 class="card-title mb-0 fs-6 fw-bold">Điểm danh ngày hôm nay</h5>
    </div>
    <div class="card-body">
        <form action="${pageContext.request.contextPath}/intern/attendance" method="post" class="row g-3 align-items-end">
            <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
            <div class="col-md-4 col-sm-6">
                <label class="form-label" for="actionEnrollment">Mã Ghi Danh (Enrollment ID) *</label>
                <input type="number" id="actionEnrollment" name="enrollmentId" class="form-control" value="<c:out value='${enrollmentId != null ? enrollmentId : param.enrollmentId}'/>" required min="1">
            </div>
            <div class="col-md-8 col-sm-6 d-flex gap-2">
                <button type="submit" name="action" value="checkin" class="btn btn-primary">
                    <i class="bi bi-box-arrow-in-right me-1"></i>Check-in
                </button>
                <button type="submit" name="action" value="checkout" class="btn btn-warning">
                    <i class="bi bi-box-arrow-right me-1"></i>Check-out
                </button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow-sm border-0">
    <div class="card-header bg-white py-3">
        <h5 class="card-title mb-0 fs-6 fw-bold">Lịch sử chuyên cần (US22)</h5>
    </div>
    <div class="card-body">
        <form action="${pageContext.request.contextPath}/intern/attendance" method="get" class="row g-3 mb-4">
            <input type="hidden" name="enrollmentId" value="<c:out value='${enrollmentId != null ? enrollmentId : param.enrollmentId}'/>">
            <div class="col-md-5 col-sm-6">
                <label class="form-label" for="filterFrom">Từ ngày</label>
                <input type="date" id="filterFrom" name="from" class="form-control" value="<c:out value='${from != null ? from : param.from}'/>">
            </div>
            <div class="col-md-5 col-sm-6">
                <label class="form-label" for="filterTo">Đến ngày</label>
                <input type="date" id="filterTo" name="to" class="form-control" value="<c:out value='${to != null ? to : param.to}'/>">
            </div>
            <div class="col-md-2 col-sm-12 d-flex align-items-end">
                <button type="submit" class="btn btn-secondary w-100">
                    <i class="bi bi-funnel me-1"></i>Lọc
                </button>
            </div>
        </form>
        
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
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
                            <tr><td colspan="6" class="text-center text-muted py-5"><i class="bi bi-calendar-x fs-2 d-block mb-2"></i>Không có dữ liệu chấm công.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="row" items="${report}">
                                <tr>
                                    <td><strong><c:out value="${row.workDate}" /></strong></td>
                                    <td><c:out value="${row.startTime}" /> – <c:out value="${row.endTime}" /></td>
                                    <td><c:out value="${not empty row.checkInTime ? row.checkInTime : '—'}" /></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${row.status eq 'MISSING_CHECKOUT'}">
                                                <span class="text-danger fw-bold">Chưa check-out</span>
                                            </c:when>
                                            <c:when test="${not empty row.checkOutTime}">
                                                <c:out value="${row.checkOutTime}" />
                                            </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${row.status eq 'PRESENT'}"><span class="badge text-bg-success">Đi làm</span></c:when>
                                            <c:when test="${row.status eq 'MISSING_CHECKOUT'}"><span class="badge text-bg-danger">Thiếu check-out</span></c:when>
                                            <c:when test="${row.status eq 'LEAVE'}"><span class="badge text-bg-info">Nghỉ phép</span></c:when>
                                            <c:when test="${row.status eq 'ABSENT'}"><span class="badge text-bg-warning">Vắng mặt</span></c:when>
                                            <c:otherwise><span class="badge text-bg-secondary"><c:out value="${row.status}" /></span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><fmt:formatNumber value="${row.workedHours}" maxFractionDigits="2" minFractionDigits="1" /></td>
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
