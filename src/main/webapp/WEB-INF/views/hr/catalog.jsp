<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head><meta charset="UTF-8"><title>Thiết lập thực tập</title></head>
<body>
<h1>Phòng ban, chương trình và mentor</h1>
<p><a href="${pageContext.request.contextPath}/hr/enrollments">Quản lý ghi danh</a></p>
<c:if test="${not empty successMessage}"><p><c:out value="${successMessage}"/></p></c:if>
<c:if test="${not empty errorMessage}"><p><c:out value="${errorMessage}"/></p></c:if>

<h2>Tạo phòng ban</h2>
<form method="post" action="${pageContext.request.contextPath}/hr/catalog">
    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
    <input type="hidden" name="action" value="create-department"/>
    <label>Tên <input name="name" maxlength="150" required/></label>
    <label>Mô tả <input name="description"/></label>
    <button type="submit">Tạo phòng ban</button>
</form>

<h2>Tạo chương trình</h2>
<form method="post" action="${pageContext.request.contextPath}/hr/catalog">
    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
    <input type="hidden" name="action" value="create-program"/>
    <label>Tên <input name="name" maxlength="200" required/></label>
    <label>Phòng ban <select name="departmentId" required>
        <c:forEach var="department" items="${departments}">
            <option value="${department.departmentId}"><c:out value="${department.departmentName}"/></option>
        </c:forEach>
    </select></label>
    <label>Bắt đầu <input type="date" name="startDate" required/></label>
    <label>Kết thúc <input type="date" name="endDate" required/></label>
    <label>Số TTS tối đa (0 = không giới hạn) <input type="number" min="0" name="maxInterns" value="0" required/></label>
    <label>Mô tả <input name="description"/></label>
    <button type="submit">Tạo chương trình PLANNED</button>
</form>

<h2>Chương trình</h2>
<table border="1">
    <tr><th>Tên</th><th>Phòng ban</th><th>Thời gian</th><th>Trạng thái</th><th>Thao tác</th></tr>
    <c:forEach var="program" items="${programs}">
        <tr>
            <td><c:out value="${program.programName}"/></td>
            <td><c:out value="${program.departmentName}"/></td>
            <td><c:out value="${program.startDate}"/> – <c:out value="${program.endDate}"/></td>
            <td><c:out value="${program.status}"/></td>
            <td><c:if test="${program.status == 'PLANNED'}">
                <form method="post" action="${pageContext.request.contextPath}/hr/catalog">
                    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                    <input type="hidden" name="action" value="activate-program"/>
                    <input type="hidden" name="programId" value="${program.programId}"/>
                    <button type="submit">Kích hoạt</button>
                </form>
            </c:if></td>
        </tr>
    </c:forEach>
</table>

<h2>Thêm mentor</h2>
<form method="post" action="${pageContext.request.contextPath}/hr/catalog">
    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
    <input type="hidden" name="action" value="create-mentor"/>
    <label>Tài khoản Mentor <select name="userId" required>
        <c:forEach var="user" items="${mentorUsers}">
            <option value="${user.userId}"><c:out value="${user.fullName}"/></option>
        </c:forEach>
    </select></label>
    <label>Phòng ban <select name="departmentId" required>
        <c:forEach var="department" items="${departments}">
            <option value="${department.departmentId}"><c:out value="${department.departmentName}"/></option>
        </c:forEach>
    </select></label>
    <label>Chuyên môn <input name="specialization"/></label>
    <label>Số TTS tối đa <input type="number" min="1" name="maxMentees" value="5" required/></label>
    <button type="submit">Thêm mentor</button>
</form>

<h2>Mentor</h2>
<table border="1">
    <tr><th>Tên</th><th>Phòng ban</th><th>Chuyên môn</th><th>Đang hướng dẫn</th></tr>
    <c:forEach var="mentor" items="${mentors}">
        <tr>
            <td><c:out value="${mentor.fullName}"/></td>
            <td><c:out value="${mentor.departmentName}"/></td>
            <td><c:out value="${mentor.specialization}"/></td>
            <td>${mentor.currentMenteeCount}/${mentor.maxMentees}</td>
        </tr>
    </c:forEach>
</table>
</body>
</html>
