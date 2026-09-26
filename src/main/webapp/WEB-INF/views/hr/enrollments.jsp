<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý ghi danh</title>
</head>
<body>
<h1>Quản lý ghi danh & phân mentor</h1>
<p><a href="${pageContext.request.contextPath}/hr/catalog">Thiết lập phòng ban, chương trình, mentor</a></p>

<c:if test="${not empty successMessage}">
    <div style="color:green"><c:out value="${successMessage}"/></div>
</c:if>
<c:if test="${not empty errorMessage}">
    <div style="color:red"><c:out value="${errorMessage}"/></div>
</c:if>

<%-- Detail view --%>
<c:if test="${viewMode == 'detail' and not empty enrollment}">
    <h2>Enrollment #<c:out value="${enrollment.enrollmentId}"/></h2>
    <p>Thực tập sinh: <c:out value="${enrollment.internName}"/></p>
    <p>Chương trình: <c:out value="${enrollment.programName}"/></p>
    <p>Trạng thái: <c:out value="${enrollment.status}"/></p>
    <p>Mentor hiện tại: <c:out value="${not empty enrollment.currentMentorName ? enrollment.currentMentorName : 'Chưa phân'}"/></p>

    <c:if test="${enrollment.status == 'ACTIVE'}">
        <c:choose>
            <c:when test="${empty enrollment.currentMentorId}">
                <a href="${pageContext.request.contextPath}/hr/enrollments?action=assign-form&enrollmentId=${enrollment.enrollmentId}">Phân mentor</a>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/hr/enrollments?action=change-form&enrollmentId=${enrollment.enrollmentId}">Đổi mentor</a>
            </c:otherwise>
        </c:choose>
    </c:if>

    <h3>Lịch sử mentor</h3>
    <table border="1">
        <tr><th>Mentor</th><th>Ngày gán</th><th>Ngày kết thúc</th><th>Lý do</th><th>Gán bởi</th></tr>
        <c:forEach var="ma" items="${mentorHistory}">
            <tr>
                <td><c:out value="${ma.mentorName}"/></td>
                <td><c:out value="${ma.assignedAt}"/></td>
                <td><c:out value="${ma.endedAt}"/></td>
                <td><c:out value="${ma.endReason}"/></td>
                <td><c:out value="${ma.assignedByName}"/></td>
            </tr>
        </c:forEach>
    </table>
    <p><a href="${pageContext.request.contextPath}/hr/enrollments">← Quay lại</a></p>
</c:if>

<%-- Enroll form --%>
<c:if test="${viewMode == 'enroll-form'}">
    <h2>Ghi danh vào chương trình</h2>
    <form method="post" action="${pageContext.request.contextPath}/hr/enrollments">
        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
        <input type="hidden" name="action" value="enroll"/>
        <label>Mã đơn (Application ID): <input type="number" name="applicationId" value="${applicationId}" required/></label><br/>
        <label>Chương trình:
            <select name="programId" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="prog" items="${programs}">
                    <option value="${prog.programId}"><c:out value="${prog.programName}"/> (<c:out value="${prog.departmentName}"/>)</option>
                </c:forEach>
            </select>
        </label><br/>
        <button type="submit">Ghi danh</button>
    </form>
    <p><a href="${pageContext.request.contextPath}/hr/enrollments">← Quay lại</a></p>
</c:if>

<%-- Assign mentor form --%>
<c:if test="${viewMode == 'assign-form' and not empty enrollment}">
    <h2>Phân mentor cho <c:out value="${enrollment.internName}"/></h2>
    <form method="post" action="${pageContext.request.contextPath}/hr/enrollments">
        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
        <input type="hidden" name="action" value="assign-mentor"/>
        <input type="hidden" name="enrollmentId" value="${enrollment.enrollmentId}"/>
        <label>Mentor:
            <select name="mentorId" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="m" items="${mentors}">
                    <option value="${m.mentorId}"><c:out value="${m.fullName}"/> (${m.currentMenteeCount}/${m.maxMentees})</option>
                </c:forEach>
            </select>
        </label><br/>
        <button type="submit">Phân mentor</button>
    </form>
    <p><a href="${pageContext.request.contextPath}/hr/enrollments">← Quay lại</a></p>
</c:if>

<%-- Change mentor form --%>
<c:if test="${viewMode == 'change-form' and not empty enrollment}">
    <h2>Đổi mentor cho <c:out value="${enrollment.internName}"/></h2>
    <form method="post" action="${pageContext.request.contextPath}/hr/enrollments">
        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
        <input type="hidden" name="action" value="change-mentor"/>
        <input type="hidden" name="enrollmentId" value="${enrollment.enrollmentId}"/>
        <label>Mentor mới:
            <select name="newMentorId" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="m" items="${mentors}">
                    <option value="${m.mentorId}"><c:out value="${m.fullName}"/> (${m.currentMenteeCount}/${m.maxMentees})</option>
                </c:forEach>
            </select>
        </label><br/>
        <label>Lý do đổi: <input type="text" name="endReason" required/></label><br/>
        <button type="submit">Đổi mentor</button>
    </form>
    <p><a href="${pageContext.request.contextPath}/hr/enrollments">← Quay lại</a></p>
</c:if>

<%-- List view (default) --%>
<c:if test="${viewMode == 'list' or empty viewMode}">
    <p>
        <a href="${pageContext.request.contextPath}/hr/enrollments?action=enroll-form">Ghi danh mới</a>
    </p>
    <table border="1">
        <tr>
            <th>ID</th><th>Thực tập sinh</th><th>Chương trình</th>
            <th>Trạng thái</th><th>Mentor</th><th>Ngày ghi danh</th><th>Thao tác</th>
        </tr>
        <c:forEach var="e" items="${enrollments}">
            <tr>
                <td><c:out value="${e.enrollmentId}"/></td>
                <td><c:out value="${e.internName}"/></td>
                <td><c:out value="${e.programName}"/></td>
                <td><c:out value="${e.status}"/></td>
                <td><c:out value="${not empty e.currentMentorName ? e.currentMentorName : '-'}"/></td>
                <td><c:out value="${e.enrolledAt}"/></td>
                <td>
                    <a href="${pageContext.request.contextPath}/hr/enrollments?action=detail&id=${e.enrollmentId}">Xem</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>
</body>
</html>
