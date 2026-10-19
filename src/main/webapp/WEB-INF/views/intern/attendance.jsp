<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<c:set var="pageTitle" value="Chấm công thực tập" />
<c:set var="activePage" value="intern-attendance" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
        <div>
            <h2 class="h3 fw-bold mb-1"><i class="bi bi-clock-history me-2 text-primary"></i>Chấm công thực tập</h2>
            <p class="text-muted mb-0">Thực hiện điểm danh check-in và check-out theo ca làm việc hàng ngày.</p>
        </div>
        <div>
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2 fs-6">
                <i class="bi bi-calendar-check me-1"></i> Hôm nay: <span id="currentDateDisplay"></span>
            </span>
        </div>
    </div>

    <c:if test="${not empty param.error}">
        <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i><strong>Lỗi:</strong> <c:out value="${param.error}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty param.success}">
        <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-check-circle-fill me-2"></i><strong>Thành công:</strong> Chấm công đã được ghi nhận vào hệ thống!
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <!-- Thẻ chấm công nhanh liên kết tài khoản -->
    <div class="card border-0 shadow-sm mb-4">
        <div class="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
            <h5 class="card-title mb-0 fw-bold text-dark">
                <i class="bi bi-fingerprint text-primary me-2"></i>Thực hiện chấm công
            </h5>
            <span class="badge bg-success-subtle text-success border border-success-subtle">
                <i class="bi bi-link-45deg me-1"></i>Đã liên kết tài khoản
            </span>
        </div>
        <div class="card-body p-4">
            <c:if test="${not empty enrollmentDetails}">
                <div class="alert alert-info border-info-subtle mb-4">
                    <div class="d-flex align-items-center">
                        <i class="bi bi-person-badge fs-2 me-3 text-info"></i>
                        <div>
                            <div class="fw-bold fs-6"><c:out value="${enrollmentDetails.fullName}"/> (<c:out value="${enrollmentDetails.email}"/>)</div>
                            <div class="small text-muted">
                                Chương trình: <strong><c:out value="${enrollmentDetails.programName}"/></strong>
                                &bull; Mã ghi danh: <span class="badge bg-secondary">#<c:out value="${enrollmentDetails.enrollmentId}"/></span>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>

            <form action="${pageContext.request.contextPath}/intern/attendance" method="post" class="row g-3 align-items-end">
                <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
                <div class="col-12"><label class="form-label">Chương trình thực tập</label>
                <select name="enrollmentId" class="form-select" required><option value="">Chọn chương trình</option>
                <c:forEach var="choice" items="${enrollmentChoices}"><option value="${choice.enrollmentId}" ${choice.enrollmentId eq enrollmentId ? 'selected' : ''}><c:out value="${choice.programName}"/> — <c:out value="${choice.enrollmentStatus}"/></option></c:forEach></select></div>

                <div class="col-lg-6 col-md-12">
                    <label class="form-label fw-semibold">
                        <i class="bi bi-envelope-at me-1 text-primary"></i>Gmail / Email ghi danh chấm công
                    </label>
                    <div class="input-group">
                        <span class="input-group-text bg-light"><i class="bi bi-envelope"></i></span>
                        <input type="email" name="email" class="form-control" readonly
                               value="<c:out value='${internEmail != null ? internEmail : sessionScope.currentUser.email}'/>"
                               placeholder="nguyenvannam@student.edu.vn" required>
                    </div>
                    <div class="form-text text-muted">
                        <i class="bi bi-info-circle me-1"></i>Được tự động điền từ tài khoản đăng nhập hiện tại để tránh phải gõ lại mã ghi danh.
                    </div>
                </div>

                <div class="col-lg-6 col-md-12 d-flex flex-wrap gap-2">
                    <button type="submit" name="action" value="checkin" class="btn btn-primary px-4 py-2 flex-grow-1 shadow-sm">
                        <i class="bi bi-box-arrow-in-right me-1"></i> Check-in vào ca
                    </button>
                    <button type="submit" name="action" value="checkout" class="btn btn-warning px-4 py-2 flex-grow-1 shadow-sm text-dark">
                        <i class="bi bi-box-arrow-right me-1"></i> Check-out ra ca
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- Bảng lịch sử chấm công -->
    <div class="card border-0 shadow-sm">
        <div class="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
            <h5 class="card-title mb-0 fw-bold text-dark">
                <i class="bi bi-journal-check text-primary me-2"></i>Lịch sử & Báo cáo chấm công
            </h5>
        </div>
        <div class="card-body p-4">
            <form action="${pageContext.request.contextPath}/intern/attendance" method="get" class="row g-3 mb-4">
                <input type="hidden" name="enrollmentId" value="<c:out value='${enrollmentId}'/>">
                <div class="col-md-4">
                    <label class="form-label small fw-semibold">Từ ngày</label>
                    <input type="date" name="from" class="form-control" value="<c:out value='${from}'/>">
                </div>
                <div class="col-md-4">
                    <label class="form-label small fw-semibold">Đến ngày</label>
                    <input type="date" name="to" class="form-control" value="<c:out value='${to}'/>">
                </div>
                <div class="col-md-4 d-flex align-items-end">
                    <button type="submit" class="btn btn-outline-primary w-100">
                        <i class="bi bi-funnel me-1"></i>Lọc dữ liệu
                    </button>
                </div>
            </form>

            <div class="table-responsive">
                <table class="table table-hover table-bordered align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="text-center" style="width: 120px;">Ngày</th>
                            <th>Ca làm việc</th>
                            <th class="text-center">Giờ Check-in</th>
                            <th class="text-center">Giờ Check-out</th>
                            <th class="text-center" style="width: 150px;">Trạng thái</th>
                            <th class="text-center" style="width: 120px;">Giờ làm (h)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty report}">
                                <tr>
                                    <td colspan="6" class="text-center py-4 text-muted">
                                        <i class="bi bi-calendar-x fs-3 d-block mb-2"></i>
                                        Chưa có dữ liệu chấm công trong khoảng thời gian này.
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="row" items="${report}">
                                    <tr>
                                        <td class="text-center fw-semibold"><c:out value="${row.workDate}" /></td>
                                        <td><i class="bi bi-clock me-1 text-muted"></i><c:out value="${row.startTime} - ${row.endTime}" /></td>
                                        <td class="text-center">
                                            <c:choose>
                                                <c:when test="${row.checkInTime != null}">
                                                    <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">
                                                        <c:out value="${row.checkInTime}" />
                                                    </span>
                                                </c:when>
                                                <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-center">
                                            <c:choose>
                                                <c:when test="${row.checkOutTime != null}">
                                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1">
                                                        <c:out value="${row.checkOutTime}" />
                                                    </span>
                                                </c:when>
                                                <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-center">
                                            <c:choose>
                                                <c:when test="${row.status == 'PRESENT'}">
                                                    <span class="badge bg-success text-white px-2 py-1"><i class="bi bi-check-circle me-1"></i>Có mặt</span>
                                                </c:when>
                                                <c:when test="${row.status == 'LEAVE'}">
                                                    <span class="badge bg-info text-dark px-2 py-1"><i class="bi bi-calendar2-minus me-1"></i>Nghỉ phép</span>
                                                </c:when>
                                                <c:when test="${row.status == 'MISSING_CHECKOUT'}">
                                                    <span class="badge bg-warning text-dark px-2 py-1"><i class="bi bi-exclamation-circle me-1"></i>Quên Check-out</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-danger text-white px-2 py-1"><i class="bi bi-x-circle me-1"></i>Vắng mặt</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-center fw-bold">
                                            <fmt:formatNumber value="${row.workedHours}" maxFractionDigits="2" /> h
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
</div>

<script>
    document.addEventListener("DOMContentLoaded", function () {
        const today = new Date();
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        const formatted = today.toLocaleDateString('vi-VN', options);
        const elem = document.getElementById("currentDateDisplay");
        if (elem) elem.textContent = formatted;
    });
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
