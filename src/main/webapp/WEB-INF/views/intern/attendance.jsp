<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="title" value="Chấm công thực tập" />
</jsp:include>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="bi bi-clock-history me-2"></i> Chấm công (US21)</h2>
</div>

<c:if test="${not empty param.error}">
    <div class="alert alert-danger"><c:out value="${param.error}"/></div>
</c:if>
<c:if test="${not empty param.success}">
    <div class="alert alert-success">Thao tác thành công.</div>
</c:if>

<div class="card mb-4">
    <div class="card-body">
        <form action="${pageContext.request.contextPath}/intern/attendance" method="post" class="d-flex align-items-end gap-3">
            <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
            <div>
                <label class="form-label" for="attendanceEnrollment">Chương trình ghi danh</label>
                <select id="attendanceEnrollment" name="enrollmentId" class="form-select" required>
                    <option value="">Chọn chương trình để chấm công</option>
                    <c:forEach var="choice" items="${enrollmentChoices}">
                        <option value="${choice.enrollmentId}" ${choice.enrollmentId == enrollmentId ? 'selected' : ''}><c:out value="${choice.programName}"/> — <c:out value="${choice.enrollmentStatus}"/> (#${choice.enrollmentId})</option>
                    </c:forEach>
                </select>
                <c:if test="${empty enrollmentChoices}"><div class="form-text">Bạn chưa có ghi danh. Liên hệ HR để được hỗ trợ.</div></c:if>
            </div>
            <button type="submit" name="action" value="checkin" class="btn btn-primary"><i class="bi bi-box-arrow-in-right"></i> Check-in</button>
            <button type="submit" name="action" value="checkout" class="btn btn-warning"><i class="bi bi-box-arrow-right"></i> Check-out</button>
        </form>
    </div>
</div>

<div class="card">
    <div class="card-header bg-white">
        <h5 class="mb-0">Báo cáo chấm công (US22)</h5>
    </div>
    <div class="card-body">
        <form action="${pageContext.request.contextPath}/intern/attendance" method="get" class="row g-3 mb-4">
            <div class="col-md-12">
                <label class="form-label" for="reportEnrollment">Chương trình ghi danh</label>
                <select id="reportEnrollment" name="enrollmentId" class="form-select">
                    <option value="">Tất cả ghi danh của tôi</option>
                    <c:forEach var="choice" items="${enrollmentChoices}">
                        <option value="${choice.enrollmentId}" ${choice.enrollmentId == enrollmentId ? 'selected' : ''}><c:out value="${choice.programName}"/> — <c:out value="${choice.enrollmentStatus}"/> (#${choice.enrollmentId})</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-4">
                <label class="form-label">Từ ngày</label>
                <input type="date" name="from" class="form-control" value="<c:out value='${from}'/>">
            </div>
            <div class="col-md-4">
                <label class="form-label">Đến ngày</label>
                <input type="date" name="to" class="form-control" value="<c:out value='${to}'/>">
            </div>
            <div class="col-md-4 d-flex align-items-end">
                <button type="submit" class="btn btn-secondary w-100">Lọc</button>
            </div>
        </form>
        
        <table class="table table-bordered table-striped">
            <thead>
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
                        <tr><td colspan="6" class="text-center">Không có dữ liệu chấm công.</td></tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="row" items="${report}">
                            <tr>
                                <td><c:out value="${row.workDate}" /></td>
                                <td><c:out value="${row.startTime} - ${row.endTime}" /></td>
                                <td><c:out value="${row.checkInTime != null ? row.checkInTime : '-'}" /></td>
                                <td><c:out value="${row.checkOutTime != null ? row.checkOutTime : '-'}" /></td>
                                <td>
                                    <span class="badge ${row.status == 'PRESENT' ? 'bg-success' : row.status == 'LEAVE' ? 'bg-info' : row.status == 'MISSING_CHECKOUT' ? 'bg-warning text-dark' : 'bg-danger'}">
                                        <c:out value="${row.status}" />
                                    </span>
                                </td>
                                <td><fmt:formatNumber value="${row.workedHours}" maxFractionDigits="2" /></td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
