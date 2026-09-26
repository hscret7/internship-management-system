<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý đơn ứng tuyển</title>
</head>
<body>
<h1>Quản lý đơn ứng tuyển</h1>

<c:if test="${not empty successMessage}">
    <div style="color:green"><c:out value="${successMessage}"/></div>
</c:if>
<c:if test="${not empty errorMessage}">
    <div style="color:red"><c:out value="${errorMessage}"/></div>
</c:if>

<c:if test="${viewMode == 'detail' and not empty application}">
    <h2>Chi tiết đơn #<c:out value="${application.applicationId}"/></h2>
    <p>Thực tập sinh: <c:out value="${application.internName}"/> (<c:out value="${application.internEmail}"/>)</p>
    <p>Trạng thái: <c:out value="${application.status}"/></p>
    <p>Ngày nộp: <c:out value="${application.submittedAt}"/></p>
    <c:if test="${not empty application.reviewerName}">
        <p>Người duyệt: <c:out value="${application.reviewerName}"/></p>
        <p>Ngày duyệt: <c:out value="${application.reviewedAt}"/></p>
    </c:if>
    <c:if test="${not empty application.rejectReason}">
        <p>Lý do từ chối: <c:out value="${application.rejectReason}"/></p>
    </c:if>

    <c:if test="${application.status == 'PENDING'}">
        <form method="post" action="${pageContext.request.contextPath}/hr/applications" style="display:inline">
            <input type="hidden" name="csrfToken" value="${csrfToken}"/>
            <input type="hidden" name="applicationId" value="${application.applicationId}"/>
            <input type="hidden" name="action" value="approve"/>
            <button type="submit">Duyệt</button>
        </form>
        <form method="post" action="${pageContext.request.contextPath}/hr/applications" style="display:inline">
            <input type="hidden" name="csrfToken" value="${csrfToken}"/>
            <input type="hidden" name="applicationId" value="${application.applicationId}"/>
            <input type="hidden" name="action" value="reject"/>
            <label>Lý do từ chối: <input type="text" name="rejectReason" required/></label>
            <button type="submit">Từ chối</button>
        </form>
    </c:if>

    <p><a href="${pageContext.request.contextPath}/hr/applications">← Quay lại danh sách</a></p>
</c:if>

<c:if test="${viewMode == 'list' or empty viewMode}">
    <p>
        <a href="${pageContext.request.contextPath}/hr/applications">Chờ duyệt</a> |
        <a href="${pageContext.request.contextPath}/hr/applications?action=all">Tất cả</a>
    </p>

    <table border="1">
        <tr>
            <th>Mã đơn</th><th>Thực tập sinh</th><th>Trạng thái</th>
            <th>Ngày nộp</th><th>Thao tác</th>
        </tr>
        <c:forEach var="app" items="${applications}">
            <tr>
                <td><c:out value="${app.applicationId}"/></td>
                <td><c:out value="${app.internName}"/></td>
                <td><c:out value="${app.status}"/></td>
                <td><c:out value="${app.submittedAt}"/></td>
                <td>
                    <a href="${pageContext.request.contextPath}/hr/applications?action=detail&id=${app.applicationId}">Xem</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>
</body>
</html>
