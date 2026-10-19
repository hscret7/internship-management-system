<%--
    header.jsp - Khung đầu trang dùng chung cho mọi role.
    Owner: FE1

    Cách dùng trong một trang JSP:
        <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <c:set var="pageTitle"  value="Hồ sơ cá nhân" />
        <c:set var="activePage" value="profile" />
        <%@ include file="/WEB-INF/views/common/header.jsp" %>
        ... nội dung trang ...
        <%@ include file="/WEB-INF/views/common/footer.jsp" %>

    Trang KHÔNG cần lặp lại <html>, <head>, sidebar hay topbar.

    ============================================================================
    HỢP ĐỒNG SESSION VỚI BE1 - KHÔNG ĐƯỢC ĐỔI TÊN
    ----------------------------------------------------------------------------
    LoginServlet đặt vào session:
        currentUser : com.internship.model.User
        currentRole : String - "ADMIN" | "HR" | "MENTOR" | "INTERN"

    User của BE1 KHÔNG có isAdmin()/isHR()/isIntern().
    Nên TUYỆT ĐỐI không viết ${sessionScope.currentUser.admin} -> ném
    PropertyNotFoundException -> HTTP 500.

    Lấy role: dùng currentRole (đã chốt với BE1 ngày 28/09/2026). KHÔNG dùng
    ${sessionScope.currentUser.role.roleName} - hiện cho cùng kết quả
    (currentRole = user.getRole().getRoleName()) nhưng không phải key đã chốt,
    và sẽ lệch nếu cách tính currentRole ở LoginServlet thay đổi.
        ${sessionScope.currentRole eq 'ADMIN'}

    Trước 28/09/2026 khối dưới đây đọc sessionScope.user nên không bao giờ
    render: không có tên người dùng, không có nhãn vai trò, và không có nút
    Đăng xuất nào trong toàn bộ giao diện.
    ============================================================================
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty pageTitle}">
    <c:set var="pageTitle" value="Hệ thống quản lý thực tập" />
</c:if>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${pageTitle} - Internship Management System</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:ital,wght@0,300;0,400;0,500;0,600;0,700;1,400&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/app.css?v=20261019-khanh-base" rel="stylesheet">
    <c:if test="${not empty pageStylesheet}">
        <link href="${ctx}/assets/css/${pageStylesheet}?v=20261019-khanh-base" rel="stylesheet">
    </c:if>
</head>
<body>

<div class="app-shell">

    <%@ include file="/WEB-INF/views/common/sidebar.jsp" %>

    <div class="app-backdrop" id="appBackdrop" hidden></div>

    <div class="app-main">

        <header class="app-topbar">
            <button type="button" class="app-menu-toggle" aria-label="Mở menu" aria-expanded="false">
                <i class="bi bi-list"></i>
            </button>

            <h1 class="app-topbar__title">${pageTitle}</h1>

            <div class="app-topbar__spacer"></div>

            <c:if test="${not empty sessionScope.currentUser}">

                <%--
                    ======================= CHUONG THONG BAO (US36) =======================
                    Owner: FE1. Dat o header dung chung nen CA 4 VAI TRO deu thay.
                    Hien thi: so chua doc + danh sach + danh dau da doc.

                    Du lieu lay tu GET ${ctx}/notifications?page=1&size=8 (hop dong
                    docs/BE1_US36_INAPP_CONTRACT.md). Luu y ve quyen: /notifications
                    KHONG nam duoi /admin|/hr|/mentor|/intern nen AuthorizationFilter
                    khong chan theo vai tro; NotificationServlet chi doi co session
                    currentUser. Vi vay chuong nay dung duoc cho ca 4 vai tro.

                    HAI DIEU DE LAM SAI, da tranh san o notifications-bell.js:
                      1) Phien het han -> AuthenticationFilter redirect ve trang HTML
                         dang nhap voi HTTP 200. Phai kiem tra Content-Type la JSON chu
                         KHONG chi kiem tra response.ok.
                      2) title/body do nguoi dung nhap (tieu de cuoc hop...) -> chi do
                         bang textContent, KHONG dung innerHTML.

                    Mac dinh inapp.notifications.enabled=false: luc do endpoint tra 503
                    -> chuong an badge va hien dong "chua bat", khong bao loi do.
                    Khong co endpoint "danh dau tat ca da doc" -> chi danh dau tung cai.
                --%>
                <div class="app-bell" id="appBell" data-context="${ctx}">
                    <button type="button" class="app-bell__btn" id="appBellBtn"
                            aria-haspopup="dialog" aria-expanded="false" aria-controls="appBellPanel"
                            aria-label="Thông báo">
                        <i class="bi bi-bell" aria-hidden="true"></i>
                        <span class="app-bell__badge" id="appBellBadge" hidden></span>
                    </button>

                    <div class="app-bell__panel" id="appBellPanel" role="dialog"
                         aria-label="Danh sách thông báo" hidden>
                        <div class="app-bell__head">
                            <span class="app-bell__title">Thông báo</span>
                            <span class="app-bell__tools">
                                <button type="button" class="app-bell__icon-btn" id="appBellReload"
                                        title="Tải lại" aria-label="Tải lại danh sách thông báo">
                                    <i class="bi bi-arrow-clockwise" aria-hidden="true"></i>
                                </button>
                                <button type="button" class="app-bell__icon-btn" id="appBellClose"
                                        title="Đóng" aria-label="Đóng danh sách thông báo">
                                    <i class="bi bi-x-lg" aria-hidden="true"></i>
                                </button>
                            </span>
                        </div>
                        <%-- Dong trang thai mac dinh nam trong HTML: nguoi dung van thay
                             phan hoi dung khi JS loi hoac bi chan. JS se thay the. --%>
                        <div class="app-bell__body" id="appBellBody" aria-live="polite">
                            <p class="app-bell__hint">Đang tải…</p>
                        </div>
                    </div>
                </div>

                <div class="app-user">
                    <span class="app-user__avatar" aria-hidden="true">
                        <c:choose>
                            <c:when test="${not empty sessionScope.currentUser.username}">
                                <c:out value="${fn:toUpperCase(fn:substring(sessionScope.currentUser.username, 0, 1))}" />
                            </c:when>
                            <c:otherwise><i class="bi bi-person"></i></c:otherwise>
                        </c:choose>
                    </span>
                    <span class="app-user__meta">
                        <%-- User của BE1 có getFullName(); ưu tiên họ tên, chưa có thì lấy username --%>
                        <span class="app-user__name"><c:out value="${empty sessionScope.currentUser.fullName ? sessionScope.currentUser.username : sessionScope.currentUser.fullName}" /></span>
                        <span class="app-user__role">
                            <c:choose>
                                <c:when test="${sessionScope.currentRole eq 'ADMIN'}">Quản trị viên</c:when>
                                <c:when test="${sessionScope.currentRole eq 'HR'}">Nhân sự (HR)</c:when>
                                <c:when test="${sessionScope.currentRole eq 'MENTOR'}">Mentor</c:when>
                                <c:when test="${sessionScope.currentRole eq 'INTERN'}">Thực tập sinh</c:when>
                                <c:otherwise><c:out value="${sessionScope.currentRole}" /></c:otherwise>
                            </c:choose>
                        </span>
                    </span>
                    <%--
                        LogoutServlet CHỈ override doPost, KHÔNG có doGet. Nên
                        <a href="${ctx}/auth/logout"> (GET) sẽ trả 405 Method Not
                        Allowed. Bắt buộc phải là form POST như dưới đây.
                        Không thêm doGet vào LogoutServlet (file của BE1).
                    --%>
                    <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-toggle="modal" data-bs-target="#changePasswordModal" title="Đổi mật khẩu">
                        <i class="bi bi-key"></i>
                        <span class="d-none d-md-inline ms-1">Đổi MK</span>
                    </button>
                    <form action="${ctx}/auth/logout" method="post" class="m-0">
                        <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
                        <button type="submit" class="btn btn-outline-brand btn-sm">
                            <i class="bi bi-box-arrow-right"></i>
                            <span class="d-none d-sm-inline ms-1">Đăng xuất</span>
                        </button>
                    </form>
                </div>
            </c:if>
        </header>

        <main class="app-content">

            <%--
                BA KENH THONG BAO - LUON PHAI QUA <c:out>.
                flashSuccess/flashError/errorMessage duoc BE dat bang
                setAttribute tu nhieu servlet, trong do co chuoi sinh ra tu
                du lieu DB (vi du ContractServlet dat "Loi CSDL: " +
                e.getMessage()). Neu in thang ${...} thi HTML trong chuoi do
                duoc trinh duyet dien giai -> stored XSS. Da ra soat 41 cho
                set: khong cho nao co y dua the HTML vao, nen escape khong
                lam mat dinh dang nao.
                KHONG doi lai thanh ${...} khi sua file nay.
            --%>

            <c:if test="${not empty sessionScope.flashSuccess}">
                <div class="alert alert-success d-flex align-items-start gap-2" role="alert">
                    <i class="bi bi-check-circle-fill mt-1"></i>
                    <div class="flex-grow-1"><c:out value="${sessionScope.flashSuccess}" /></div>
                    <button type="button" class="btn-close" data-dismiss="alert" aria-label="Đóng"></button>
                </div>
                <c:remove var="flashSuccess" scope="session" />
            </c:if>

            <c:if test="${not empty sessionScope.flashError}">
                <div class="alert alert-danger d-flex align-items-start gap-2" role="alert">
                    <i class="bi bi-exclamation-triangle-fill mt-1"></i>
                    <div class="flex-grow-1"><c:out value="${sessionScope.flashError}" /></div>
                    <button type="button" class="btn-close" data-dismiss="alert" aria-label="Đóng"></button>
                </div>
                <c:remove var="flashError" scope="session" />
            </c:if>

            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger d-flex align-items-start gap-2" role="alert">
                    <i class="bi bi-exclamation-octagon-fill mt-1"></i>
                    <div class="flex-grow-1"><c:out value="${errorMessage}" /></div>
                    <button type="button" class="btn-close" data-dismiss="alert" aria-label="Đóng"></button>
                </div>
            </c:if>
