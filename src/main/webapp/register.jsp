<%--
    register.jsp - Trang đăng ký tài khoản Thực tập sinh. Owner: FE1 (US06 / 4.3)

    ============================================================================
    QUY TẮC BẮT BUỘC TỪ BẢN CHỐT (không được vi phạm):
    "Public registration luôn tạo role Intern ở phía server.
     Client không được tự gửi role tùy ý."
    => Trang này CỐ Ý KHÔNG có ô chọn role và KHÔNG có input ẩn tên "role".
       Nếu sau này có ai thêm vào, đó là lỗ hổng leo thang quyền.
    ============================================================================
    HỢP ĐỒNG CẦN CHỐT VỚI BE1 (4.1) VÀ BE3 (4.2)
    ----------------------------------------------------------------------------
    [GIẢ ĐỊNH 1] Servlet nhận POST tại  ${ctx}/auth/register
    [GIẢ ĐỊNH 2] Tên tham số tài khoản: username, email, password, confirmPassword
    [GIẢ ĐỊNH 3] Tên tham số hồ sơ:     fullName, phone, university, major, gpa
                 (trùng tên với intern/profile-form.jsp để BE1 dùng lại được
                  đúng DTO, không phải map thêm)
    [GIẢ ĐỊNH 4] Đăng ký lỗi: forward về trang này kèm request attribute
                 "errorMessage"; giá trị đã nhập được giữ lại qua ${param.*}
    [GIẢ ĐỊNH 5] Đăng ký thành công: BE1 tự quyết định 1 trong 2 cách
                 (a) set sessionScope.user rồi redirect vào /intern/profile
                 (b) set sessionScope.flashSuccess rồi redirect về /login.jsp
                 Trang này KHÔNG phụ thuộc vào việc chọn cách nào.
    ----------------------------------------------------------------------------
    BƯỚC "NỘP HỒ SƠ" (Application) KHÔNG nằm ở đây.
    Việc tạo và nộp đơn ứng tuyển là task 4.2 của BE3. Khi BE3 chốt endpoint,
    nút "Nộp hồ sơ ứng tuyển" sẽ được thêm vào trang hồ sơ, không thêm vào đây,
    vì lúc đó thí sinh đã có tài khoản và đã đăng nhập.
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
    <title>Đăng ký thực tập sinh - Internship Management System</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/app.css" rel="stylesheet">
</head>
<body>

<div class="auth-page">
    <div class="auth-card auth-card--wide">

        <div class="auth-card__brand">
            <i class="bi bi-mortarboard-fill"></i>
            <span>Internship Manager</span>
        </div>
        <p class="auth-card__sub">
            Tạo tài khoản thực tập sinh và khai báo hồ sơ ban đầu
        </p>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" role="alert">
                <i class="bi bi-exclamation-octagon-fill me-1"></i>${errorMessage}
            </div>
        </c:if>

        <form action="${ctx}/auth/register" method="post" data-check-password>

            <h2 class="h6 fw-bold mb-3">
                <i class="bi bi-person-badge me-1"></i>Thông tin tài khoản
            </h2>

            <div class="row g-3 mb-4">
                <div class="col-md-6">
                    <label class="form-label" for="username">Tên đăng nhập <span class="req">*</span></label>
                    <%-- [GIẢ ĐỊNH 2] --%>
                    <input type="text" id="username" name="username" class="form-control"
                           value="<c:out value='${param.username}' />"
                           required autofocus autocomplete="username"
                           minlength="4" maxlength="50"
                           pattern="[A-Za-z0-9._\-]{4,50}" placeholder="intern_nam">
                    <span class="form-text">4 đến 50 ký tự, chỉ dùng chữ, số, dấu chấm, gạch dưới.</span>
                </div>

                <div class="col-md-6">
                    <label class="form-label" for="email">Email <span class="req">*</span></label>
                    <input type="email" id="email" name="email" class="form-control"
                           value="<c:out value='${param.email}' />"
                           required autocomplete="email" maxlength="120"
                           placeholder="nam@student.edu.vn">
                    <span class="form-text">Dùng để nhận thông báo từ hệ thống.</span>
                </div>

                <div class="col-md-6">
                    <label class="form-label" for="password">Mật khẩu <span class="req">*</span></label>
                    <input type="password" id="password" name="password" class="form-control"
                           required autocomplete="new-password"
                           minlength="8" maxlength="100" placeholder="Tối thiểu 8 ký tự">
                </div>

                <div class="col-md-6">
                    <label class="form-label" for="confirmPassword">Nhập lại mật khẩu <span class="req">*</span></label>
                    <input type="password" id="confirmPassword" name="confirmPassword" class="form-control"
                           required autocomplete="new-password"
                           minlength="8" maxlength="100" placeholder="Nhập lại mật khẩu">
                </div>
            </div>

            <h2 class="h6 fw-bold mb-3">
                <i class="bi bi-person-vcard me-1"></i>Hồ sơ ban đầu
            </h2>

            <div class="row g-3 mb-3">
                <div class="col-md-8">
                    <label class="form-label" for="fullName">Họ và tên <span class="req">*</span></label>
                    <input type="text" id="fullName" name="fullName" class="form-control"
                           value="<c:out value='${param.fullName}' />"
                           required maxlength="100" placeholder="Nguyễn Văn Nam">
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="phone">Số điện thoại <span class="req">*</span></label>
                    <input type="tel" id="phone" name="phone" class="form-control"
                           value="<c:out value='${param.phone}' />"
                           required pattern="[0-9+ .()\-]{9,15}" placeholder="0912345678">
                </div>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-md-7">
                    <label class="form-label" for="university">Trường đại học / cao đẳng <span class="req">*</span></label>
                    <input type="text" id="university" name="university" class="form-control"
                           value="<c:out value='${param.university}' />"
                           required maxlength="150" placeholder="Đại học Bách Khoa Hà Nội">
                </div>
                <div class="col-md-5">
                    <label class="form-label" for="major">Chuyên ngành <span class="req">*</span></label>
                    <input type="text" id="major" name="major" class="form-control"
                           value="<c:out value='${param.major}' />"
                           required maxlength="100" placeholder="Công nghệ thông tin">
                </div>
                <div class="col-md-3">
                    <label class="form-label" for="gpa">GPA (hệ 4)</label>
                    <input type="number" id="gpa" name="gpa" class="form-control"
                           value="<c:out value='${param.gpa}' />"
                           step="0.01" min="0" max="4" placeholder="3.50">
                </div>
            </div>

            <%-- KHÔNG thêm ô chọn role ở đây. Server luôn gán role = INTERN. --%>

            <div class="d-flex flex-wrap gap-2 align-items-center">
                <button type="submit" class="btn btn-brand">
                    <i class="bi bi-person-plus me-1"></i>Đăng ký
                </button>
                <a href="${ctx}/auth/login" class="btn btn-outline-brand">Đã có tài khoản</a>
            </div>

        </form>

        <p class="auth-foot">
            Sau khi đăng ký thành công, bạn đăng nhập và vào
            <strong>Hồ sơ cá nhân</strong> để hoàn thiện thông tin, tải lên CV
            và nộp hồ sơ ứng tuyển.
        </p>

        <div class="auth-note">
            <i class="bi bi-info-circle me-1"></i>
            Tài khoản đăng ký tại đây luôn có quyền <strong>Thực tập sinh</strong>.
            Tài khoản HR, Mentor và Quản trị viên do quản trị viên cấp riêng.
        </div>

    </div>
</div>

<script src="${ctx}/assets/js/app.js"></script>
</body>
</html>
