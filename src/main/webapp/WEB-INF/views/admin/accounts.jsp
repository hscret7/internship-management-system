<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Tài khoản nội bộ" />
<c:set var="activePage" value="admin-accounts" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">

    <div class="page-head mb-4">
        <h2 class="page-head__title">Tài khoản nội bộ</h2>
        <p class="page-head__sub">Tạo tài khoản cho HR, Mentor và thực tập sinh. Hệ thống tự sinh mật khẩu tạm thời và gửi qua email.</p>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div>
    </c:if>
    <%-- errorMessage da duoc header.jsp render san (khoi flash chung).
         Render lai o day se hien 2 lan. --%>

    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white py-3 border-bottom">
            <h5 class="card-title mb-0 fw-bold">Tạo tài khoản mới</h5>
        </div>
        <div class="card-body">
            <form method="post" action="${pageContext.request.contextPath}/admin/accounts" class="row g-3">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>

                <div class="col-md-6">
                    <label class="form-label" for="username">Tên đăng nhập <span class="text-danger">*</span></label>
                    <input class="form-control" type="text" id="username" name="username"
                           required maxlength="50" autocomplete="off"/>
                </div>

                <div class="col-md-6">
                    <label class="form-label" for="fullName">Họ và tên <span class="text-danger">*</span></label>
                    <input class="form-control" type="text" id="fullName" name="fullName"
                           required maxlength="100"/>
                </div>

                <div class="col-md-6">
                    <label class="form-label" for="email">Email <span class="text-danger">*</span></label>
                    <input class="form-control" type="email" id="email" name="email"
                           required maxlength="100"/>
                    <div class="form-text">Mật khẩu tạm thời sẽ được gửi tới địa chỉ này.</div>
                </div>

                <div class="col-md-6">
                    <label class="form-label" for="role">Vai trò <span class="text-danger">*</span></label>
                    <select class="form-select" id="role" name="role" required>
                        <option value="">— Chọn vai trò —</option>
                        <c:forEach var="r" items="${roles}">
                            <option value="${r.roleName}"><c:out value="${r.roleName}"/></option>
                        </c:forEach>
                    </select>
                </div>

                <div class="col-12">
                    <button type="submit" class="btn btn-brand">
                        <i class="bi bi-person-plus me-1"></i>Tạo tài khoản
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-header bg-white py-3 border-bottom">
            <h5 class="card-title mb-0 fw-bold">Danh sách tài khoản (${not empty users ? users.size() : 0})</h5>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">ID</th>
                            <th>Tên đăng nhập</th>
                            <th>Họ và tên</th>
                            <th>Email</th>
                            <th>Vai trò</th>
                            <th>Trạng thái</th>
                            <th class="pe-3">Ngày tạo</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty users}">
                                <c:forEach var="u" items="${users}">
                                    <tr>
                                        <td class="ps-3 fw-bold text-muted">#<c:out value="${u.userId}"/></td>
                                        <td class="fw-semibold"><c:out value="${u.username}"/></td>
                                        <td><c:out value="${u.fullName}"/></td>
                                        <td><c:out value="${u.email}"/></td>
                                        <td><span class="badge text-bg-light border"><c:out value="${u.role.roleName}"/></span></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${u.status == 'ACTIVE'}">
                                                    <span class="badge-status badge-status--ok">Hoạt động</span>
                                                </c:when>
                                                <c:when test="${u.status == 'LOCKED'}">
                                                    <span class="badge-status badge-status--bad">Đã khoá</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge-status badge-status--neutral"><c:out value="${u.status}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="pe-3"><c:out value="${fn:replace(u.createdAt, 'T', ' ')}"/></td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="7" class="text-center py-4 text-muted">
                                        <i class="bi bi-people fs-3 d-block mb-2"></i>
                                        Chưa có tài khoản nào.
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
