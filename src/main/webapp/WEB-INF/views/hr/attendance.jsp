<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<c:set var="pageTitle" value="Báo cáo chấm công (HR)" />
<c:set var="activePage" value="hr-attendance" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="h4 mb-1"><i class="bi bi-clock-history me-2"></i>Báo cáo chấm công (US22)</h2>
        <p class="text-muted mb-0">Theo dõi chuyên cần, đi làm, nghỉ phép và xuất file báo cáo (US34).</p>
    </div>
    <div class="d-flex gap-2">
        <a href="${pageContext.request.contextPath}/hr/be4/attendance/export?format=xlsx&from=${from}&to=${to}${not empty enrollmentId ? '&enrollmentId='.concat(enrollmentId) : ''}${not empty programId ? '&programId='.concat(programId) : ''}" class="btn btn-outline-success">
            <i class="bi bi-file-earmark-excel me-1"></i>Xuất Excel
        </a>
        <a href="${pageContext.request.contextPath}/hr/be4/attendance/export?format=pdf&from=${from}&to=${to}${not empty enrollmentId ? '&enrollmentId='.concat(enrollmentId) : ''}${not empty programId ? '&programId='.concat(programId) : ''}" class="btn btn-outline-danger">
            <i class="bi bi-file-earmark-pdf me-1"></i>Xuất PDF
        </a>
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
    <div class="card-body">
        <form action="${pageContext.request.contextPath}/hr/attendance" method="get" class="row g-3">
            <div class="col-md-3">
                <label class="form-label" for="filterEnrollment">Mã Ghi Danh (Enrollment ID)</label>
                <input type="number" id="filterEnrollment" name="enrollmentId" class="form-control" min="1" placeholder="Tất cả TTS" value="<c:out value='${enrollmentId != null ? enrollmentId : param.enrollmentId}'/>">
            </div>
            <div class="col-md-3">
                <label class="form-label" for="filterProgram">Mã Chương Trình (Program ID)</label>
                <input type="number" id="filterProgram" name="programId" class="form-control" min="1" placeholder="Tất cả chương trình" value="<c:out value='${programId != null ? programId : param.programId}'/>">
            </div>
            <div class="col-md-2">
                <label class="form-label" for="filterFrom">Từ ngày</label>
                <input type="date" id="filterFrom" name="from" class="form-control" required value="<c:out value='${from != null ? from : param.from}'/>">
            </div>
            <div class="col-md-2">
                <label class="form-label" for="filterTo">Đến ngày</label>
                <input type="date" id="filterTo" name="to" class="form-control" required value="<c:out value='${to != null ? to : param.to}'/>">
            </div>
            <div class="col-md-2 d-flex align-items-end">
                <button type="submit" class="btn btn-primary w-100"><i class="bi bi-search me-1"></i>Lọc dữ liệu</button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow-sm border-0">
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
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
                            <tr><td colspan="8" class="text-center text-muted py-5"><i class="bi bi-calendar-x fs-2 d-block mb-2"></i>Không có dữ liệu chấm công phù hợp.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="row" items="${report}">
                                <tr>
                                    <td>
                                        <strong><c:out value="${row.internName}" /></strong>
                                        <div class="small text-muted">#<c:out value="${row.enrollmentId}" /></div>
                                    </td>
                                    <td><c:out value="${row.programName}" /></td>
                                    <td><c:out value="${row.workDate}" /></td>
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

<script type="application/json" id="attendance-data" data-attendance-json>
<c:out value="${attendanceJson}" escapeXml="false" />
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
