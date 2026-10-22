<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Phân quyền"/>
<c:set var="activePage" value="admin-roles"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<div class="container-fluid py-4">
    <h2 class="h4">Phân quyền tài khoản</h2>
    <p class="text-muted">Chọn vai trò và lưu cho từng tài khoản. Người dùng cần đăng nhập lại sau khi quyền thay đổi.</p>
    <div class="alert alert-info">ADMIN: quản trị hệ thống · HR: quản lý thực tập · MENTOR: hướng dẫn · INTERN: thực tập sinh. Phân công mentor và ghi danh được quản lý riêng.</div>
    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
    <div class="card"><div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
            <thead><tr><th>Tài khoản</th><th>Họ tên</th><th>Trạng thái</th><th>Vai trò</th></tr></thead>
            <tbody><c:forEach var="u" items="${users}"><tr>
                <td><c:out value="${u.username}"/> (#<c:out value="${u.userId}"/>)</td>
                <td><c:out value="${u.fullName}"/></td><td><c:out value="${u.status}"/></td>
                <td><c:choose>
                    <c:when test="${u.userId eq sessionScope.currentUser.userId}"><span class="badge text-bg-primary">ADMIN</span> <small>Tài khoản đang đăng nhập</small></c:when>
                    <c:otherwise><form method="post" action="${pageContext.request.contextPath}/admin/roles" class="d-flex gap-2">
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="userId" value="${u.userId}"/>
                        <input type="hidden" name="expectedRole" value="<c:out value='${u.role.roleName}'/>"/>
                        <select name="role" class="form-select" aria-label="Vai trò tài khoản #${u.userId}" required>
                            <c:forEach var="r" items="${roleNames}"><option value="${r}" ${r eq u.role.roleName ? 'selected' : ''}>${r}</option></c:forEach>
                        </select><button class="btn btn-brand" type="submit">Lưu quyền</button>
                    </form></c:otherwise>
                </c:choose></td>
            </tr></c:forEach></tbody>
        </table>
    </div></div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
