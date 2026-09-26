<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Đơn ứng tuyển của tôi</title>
</head>
<body>
<h1>Đơn ứng tuyển của tôi</h1>

<c:if test="${not empty successMessage}">
    <div style="color:green"><c:out value="${successMessage}"/></div>
</c:if>
<c:if test="${not empty errorMessage}">
    <div style="color:red"><c:out value="${errorMessage}"/></div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/intern/applications">
    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
    <button type="submit">Nộp đơn ứng tuyển mới</button>
</form>

<table border="1">
    <tr>
        <th>Mã đơn</th><th>Trạng thái</th><th>Ngày nộp</th>
        <th>Người duyệt</th><th>Ngày duyệt</th><th>Lý do từ chối</th>
    </tr>
    <c:forEach var="app" items="${applications}">
        <tr>
            <td><c:out value="${app.applicationId}"/></td>
            <td><c:out value="${app.status}"/></td>
            <td><c:out value="${app.submittedAt}"/></td>
            <td><c:out value="${app.reviewerName}"/></td>
            <td><c:out value="${app.reviewedAt}"/></td>
            <td><c:out value="${app.rejectReason}"/></td>
        </tr>
    </c:forEach>
</table>
</body>
</html>
