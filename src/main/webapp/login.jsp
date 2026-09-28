<%--
    login.jsp - Trang đăng nhập. Owner: FE1

    ============================================================================
    HỢP ĐỒNG CẦN CHỐT VỚI BE1 (4.1 - Đăng nhập)
    Trang này viết theo giả định dưới đây. Khi BE1 chốt khác thì chỉ sửa đúng
    các dòng được đánh dấu [GIẢ ĐỊNH], KHÔNG phải viết lại trang.
    ----------------------------------------------------------------------------
    [GIẢ ĐỊNH 1] Servlet nhận POST tại  ${ctx}/auth/login
    [GIẢ ĐỊNH 2] Tên tham số:          username, password
    [GIẢ ĐỊNH 3] Đăng nhập sai:        forward về chính trang này kèm request
                                       attribute "errorMessage" (header/flash
                                       cũng hiển thị được sessionScope.flashError)
    [GIẢ ĐỊNH 4] Đăng nhập đúng:       set sessionScope.user rồi redirect
    [GIẢ ĐỊNH 5] Đăng xuất:            ${ctx}/auth/logout
    [GIẢ ĐỊNH 6] Tài khoản bị khoá:    thông báo qua "errorMessage"
    ----------------------------------------------------------------------------
    Nếu BE1 dùng tên tham số khác (vd: user, pass) thì đổi thuộc tính name của
    2 ô input bên dưới. Không cần sửa gì khác.
    ============================================================================
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="ctx" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập - Internship Management System</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/app.css" rel="stylesheet">
</head>
<body>

<div class="auth-page">
    <div class="auth-card">

        <div class="auth-card__brand">
            <i class="bi bi-mortarboard-fill"></i>
            <span>Internship Manager</span>
        </div>
        <p class="auth-card__sub">Hệ thống quản lý thực tập</p>

        <%-- Đã đăng nhập rồi thì không cần nhập lại --%>
        <c:if test="${not empty sessionScope.currentUser}">
            <div class="alert alert-info" role="alert">
                <i class="bi bi-info-circle-fill me-1"></i>
                Bạn đang đăng nhập với tài khoản
                <strong><c:out value="${sessionScope.currentUser.username}" /></strong>.
            </div>
            <a href="${ctx}/" class="btn btn-brand w-100 mb-3">
                <i class="bi bi-arrow-right-circle me-1"></i>Vào hệ thống
            </a>
            <a href="${ctx}/auth/logout" class="btn btn-outline-brand w-100">
                <i class="bi bi-box-arrow-right me-1"></i>Đăng xuất
            </a>
        </c:if>

        <c:if test="${empty sessionScope.currentUser}">

            <%-- Thông báo sau khi đăng ký thành công, hoặc sau khi đăng xuất --%>
            <c:if test="${not empty sessionScope.flashSuccess}">
                <div class="alert alert-success" role="alert">
                    <i class="bi bi-check-circle-fill me-1"></i>${sessionScope.flashSuccess}
                </div>
                <c:remove var="flashSuccess" scope="session" />
            </c:if>

            <c:if test="${not empty sessionScope.flashError}">
                <div class="alert alert-danger" role="alert">
                    <i class="bi bi-exclamation-triangle-fill me-1"></i>${sessionScope.flashError}
                </div>
                <c:remove var="flashError" scope="session" />
            </c:if>

            <%-- Lỗi đăng nhập do servlet trả về --%>
            <c:if test="${not empty error}">
                <div class="alert alert-danger" role="alert">
                    <i class="bi bi-exclamation-octagon-fill me-1"></i>
                    <c:out value="${error}" />
                </div>
            </c:if>

            <form action="${ctx}/auth/login" method="post">

                <div class="mb-3">
                    <label class="form-label" for="username">Tên đăng nhập <span class="req">*</span></label>
                    <%-- [GIẢ ĐỊNH 2] name="username" --%>
                    <input type="text" id="username" name="username" class="form-control"
                           value="<c:out value='${username}' />"
                           required autofocus autocomplete="username"
                           maxlength="50" placeholder="intern_nam">
                </div>

                <div class="mb-3">
                    <label class="form-label" for="password">Mật khẩu <span class="req">*</span></label>
                    <%-- [GIẢ ĐỊNH 2] name="password" --%>
                    <input type="password" id="password" name="password" class="form-control"
                           required autocomplete="current-password"
                           maxlength="100" placeholder="Nhập mật khẩu">
                </div>

                <button type="submit" class="btn btn-brand w-100">
                    <i class="bi bi-box-arrow-in-right me-1"></i>Đăng nhập
                </button>
            </form>

            <p class="auth-foot">
                Chưa có tài khoản?
                <a href="${ctx}/auth/register">Đăng ký thực tập sinh</a>
            </p>

            <div class="auth-note">
                <i class="bi bi-shield-lock me-1"></i>
                Tài khoản nội bộ (HR, Mentor, Quản trị viên) do quản trị viên cấp,
                không đăng ký được từ trang này.
            </div>

        </c:if>

    </div>
</div>

</body>
</html>
