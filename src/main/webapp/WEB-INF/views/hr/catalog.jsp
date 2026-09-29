<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Chương trình thực tập" />
<c:set var="activePage" value="hr-catalog" />
<c:set var="pageStylesheet" value="fe3.css" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page fe3-be3">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-diagram-3"></i> CHƯƠNG TRÌNH</span>
            <h2>Thiết lập thực tập</h2>
            <p>Quản lý phòng ban, chương trình và mentor tại một nơi.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-diagram-3"></i></div>
    </div>
    <div class="fe3-be3__actions"><a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/hr/enrollments">Quản lý ghi danh <i class="bi bi-arrow-right"></i></a></div>
    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>

<div class="fe3-panel">

<h2>Tạo phòng ban</h2>
<form method="post" action="${pageContext.request.contextPath}/hr/catalog">
    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
    <input type="hidden" name="action" value="create-department"/>
    <label>Tên <input name="name" maxlength="150" required/></label>
    <label>Mô tả <input name="description"/></label>
    <button type="submit">Tạo phòng ban</button>
</form>

</div>
<div class="fe3-panel">
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

</div>
<div class="fe3-panel">
<h2>Chương trình</h2>
<div class="fe3-be3__table-scroll"><table>
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
</table></div>

</div>
<div class="fe3-panel">
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

</div>
<div class="fe3-panel">
<h2>Mentor</h2>
<div class="fe3-be3__table-scroll"><table>
    <tr><th>Tên</th><th>Phòng ban</th><th>Chuyên môn</th><th>Đang hướng dẫn</th></tr>
    <c:forEach var="mentor" items="${mentors}">
        <tr>
            <td><c:out value="${mentor.fullName}"/></td>
            <td><c:out value="${mentor.departmentName}"/></td>
            <td><c:out value="${mentor.specialization}"/></td>
            <td>${mentor.currentMenteeCount}/${mentor.maxMentees}</td>
        </tr>
    </c:forEach>
</table></div>
</div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
