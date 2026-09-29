<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Chương trình thực tập của tôi</title>
</head>
<body>
<h1>Chương trình thực tập của tôi</h1>

<c:if test="${not empty errorMessage}">
    <div style="color:red"><c:out value="${errorMessage}"/></div>
</c:if>

<c:choose>
    <c:when test="${not empty enrollment}">
        <h2>Đang tham gia: <c:out value="${enrollment.programName}"/></h2>
        <p>Trạng thái: <c:out value="${enrollment.status}"/></p>
        <p>Ngày ghi danh: <c:out value="${enrollment.enrolledAt}"/></p>

        <c:if test="${not empty currentMentor}">
            <h3>Mentor hiện tại: <c:out value="${currentMentor.mentorName}"/></h3>
            <p>Ngày phân công: <c:out value="${currentMentor.assignedAt}"/></p>
        </c:if>
        <c:if test="${empty currentMentor}">
            <p>Chưa được phân mentor.</p>
        </c:if>
    </c:when>
    <c:otherwise>
        <p>Bạn chưa được ghi danh vào chương trình nào.</p>
    </c:otherwise>
</c:choose>
</body>
</html>
