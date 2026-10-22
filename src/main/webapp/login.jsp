<%--
    login.jsp - Trang đăng nhập. Owner: FE1

    ============================================================================
    HỢP ĐỒNG ĐÃ CHỐT VỚI BE1 (US39/4.1 - Đăng nhập) - đối chiếu LoginServlet
    ----------------------------------------------------------------------------
    [1] Servlet nhận POST tại  ${ctx}/auth/login
    [2] Tên tham số:           username, password
    [3] GET /auth/login:       đã đăng nhập -> redirect về ${ctx}/
                               chưa đăng nhập -> forward về /login.jsp
                               (chính là file này - trang này ĐÃ được nối thật,
                                không còn là trang mồ côi như trước)
    [4] Đăng nhập sai:         forward về /login.jsp kèm request attribute
                               "error" và attribute "username" để giữ lại tên
                               đã nhập -> ô username đọc ${username}
    [5] Đăng nhập đúng:        set session currentUser + currentRole rồi
                               redirect về ${ctx}/
    [6] Tài khoản bị khoá:     thông báo qua "error"
    ----------------------------------------------------------------------------
    LƯU Ý QUAN TRỌNG: LogoutServlet CHỈ override doPost, KHÔNG có doGet, nên
    link GET tới /auth/logout sẽ trả 405 Method Not Allowed. Vì vậy nút Đăng
    xuất ở cuối trang này (và ở header.jsp) bắt buộc phải là form POST.
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

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:ital,wght@0,300;0,400;0,500;0,600;0,700;1,400&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/app.css?v=20261019-khanh-base" rel="stylesheet">
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
            <%-- LogoutServlet chỉ có doPost -> phải là form POST, xem ghi chú đầu file --%>
            <form action="${ctx}/auth/logout" method="post" class="m-0">
                <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
                <button type="submit" class="btn btn-outline-brand w-100">
                    <i class="bi bi-box-arrow-right me-1"></i>Đăng xuất
                </button>
            </form>
        </c:if>

        <c:if test="${empty sessionScope.currentUser}">

            <%-- Thông báo sau khi đăng ký thành công, hoặc sau khi đăng xuất --%>
            <%-- Hai khối dưới đây in flash bằng EL trần, KHÔNG qua <c:out>, trong khi
                 header.jsp vẫn escape đúng hai kênh này. ContractServlet và
                 DocumentUploadServlet đổ e.getMessage() (có thể là thông báo lỗi SQL)
                 vào flashError, nên EL trần ở đây là lỗ hổng XSS/HTML injection.
                 Escape cho khớp header.jsp. --%>
            <c:if test="${not empty sessionScope.flashSuccess}">
                <div class="alert alert-success" role="alert">
                    <i class="bi bi-check-circle-fill me-1"></i><c:out value="${sessionScope.flashSuccess}" />
                </div>
                <c:remove var="flashSuccess" scope="session" />
            </c:if>

            <c:if test="${not empty sessionScope.flashError}">
                <div class="alert alert-danger" role="alert">
                    <i class="bi bi-exclamation-triangle-fill me-1"></i><c:out value="${sessionScope.flashError}" />
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
                <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">

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
                    <div class="position-relative">
                        <input type="password" id="password" name="password" class="form-control pe-5"
                               required autocomplete="current-password"
                               maxlength="100" placeholder="Nhập mật khẩu">
                        <button type="button" class="btn-toggle-password position-absolute end-0 top-50 translate-middle-y me-2 border-0 bg-transparent text-secondary p-1"
                                tabindex="-1" title="Hiện/ẩn mật khẩu" aria-label="Hiện/ẩn mật khẩu"
                                onclick="togglePasswordVisibility(this, event)"
                                style="cursor: pointer; z-index: 5; display: flex; align-items: center; justify-content: center;">
                            <span class="pwd-toggle-icon" style="display: flex;">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                                  <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M1.173 8a13 13 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5s3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5s-3.879-1.168-5.168-2.457A13 13 0 0 1 1.172 8z"/>
                                  <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5M4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0"/>
                                </svg>
                            </span>
                        </button>
                    </div>
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

<script src="${ctx}/assets/js/app.js?v=20261017-v3"></script>
</body>
</html>
