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
    <div style="color:green">${successMessage}</div>
</c:if>
<c:if test="${not empty errorMessage}">
    <div style="color:red">${errorMessage}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/intern/applications">
    <button type="submit">Nộp đơn ứng tuyển mới</button>
</form>

<table border="1">
    <tr>
        <th>Mã đơn</th><th>Trạng thái</th><th>Ngày nộp</th>
        <th>Người duyệt</th><th>Ngày duyệt</th><th>Lý do từ chối</th>
    </tr>
    <c:forEach var="app" items="${applications}">
        <tr>
            <td>${app.applicationId}</td>
            <td>${app.status}</td>
            <td>${app.submittedAt}</td>
            <td>${app.reviewerName}</td>
            <td>${app.reviewedAt}</td>
            <td>${app.rejectReason}</td>
        </tr>
    </c:forEach>
</table>
</body>
</html>
