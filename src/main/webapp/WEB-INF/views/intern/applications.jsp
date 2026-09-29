<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đơn ứng tuyển của tôi" />
<c:set var="activePage" value="documents" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">

    <div class="page-head mb-4">
        <h2 class="page-head__title">Đơn ứng tuyển của tôi</h2>
        <p class="page-head__sub">Theo dõi trạng thái các đơn bạn đã nộp cho nhà tuyển dụng.</p>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div>
    </c:if>

    <div class="card shadow-sm mb-4">
        <div class="card-body d-flex justify-content-between align-items-center flex-wrap gap-2">
            <div>
                <div class="fw-bold">Nộp đơn ứng tuyển mới</div>
                <small class="text-muted">Đơn sau khi nộp sẽ ở trạng thái chờ HR duyệt.</small>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/intern/applications" class="m-0">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <button type="submit" class="btn btn-primary">
                    <i class="bi bi-send me-1"></i>Nộp đơn ứng tuyển mới
                </button>
            </form>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-header bg-white py-3 border-bottom">
            <h5 class="card-title mb-0 fw-bold">Danh sách đơn (${not empty applications ? applications.size() : 0})</h5>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">Mã đơn</th>
                            <th>Trạng thái</th>
                            <th>Ngày nộp</th>
                            <th>Người duyệt</th>
                            <th>Ngày duyệt</th>
                            <th class="pe-3">Lý do từ chối</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty applications}">
                                <c:forEach var="app" items="${applications}">
                                    <tr>
                                        <td class="ps-3 fw-bold text-muted">#<c:out value="${app.applicationId}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${app.status == 'PENDING'}">
                                                    <span class="badge bg-warning text-dark"><c:out value="${app.status}"/></span>
                                                </c:when>
                                                <c:when test="${app.status == 'APPROVED'}">
                                                    <span class="badge bg-success"><c:out value="${app.status}"/></span>
                                                </c:when>
                                                <c:when test="${app.status == 'REJECTED'}">
                                                    <span class="badge bg-danger"><c:out value="${app.status}"/></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary"><c:out value="${app.status}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><c:out value="${app.submittedAt}"/></td>
                                        <td><c:out value="${app.reviewerName}"/></td>
                                        <td><c:out value="${app.reviewedAt}"/></td>
                                        <td class="pe-3"><c:out value="${app.rejectReason}"/></td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="6" class="text-center py-4 text-muted">
                                        <i class="bi bi-inbox fs-3 d-block mb-2"></i>
                                        Bạn chưa nộp đơn ứng tuyển nào.
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
