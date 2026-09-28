<%--
    sidebar.jsp - Menu điều hướng theo role. Owner: FE1

    Quy ước:
    - Trang đặt <c:set var="activePage" value="..."/> TRƯỚC khi include header.
    - Mục chưa có route thật (BE/FE khác chưa làm xong) render bằng
      <span class="app-nav__link is-soon">, KHÔNG dùng <a> để tránh link chết 404.
      Khi route sẵn sàng thì đổi span thành a và bỏ class is-soon.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<aside class="app-sidebar" id="appSidebar">

    <a class="app-brand" href="${ctx}/">
        <i class="bi bi-mortarboard-fill"></i>
        <span>Internship Manager</span>
    </a>

    <nav class="app-nav" aria-label="Menu chính">

        <%-- ============================ INTERN ============================ --%>
        <c:if test="${sessionScope.user.intern}">

            <div class="app-nav__group">Thực tập của tôi</div>

            <a class="app-nav__link ${activePage eq 'profile' ? 'is-active' : ''}"
               href="${ctx}/intern/profile">
                <i class="bi bi-person-vcard"></i><span>Hồ sơ cá nhân</span>
            </a>

            <a class="app-nav__link ${activePage eq 'documents' ? 'is-active' : ''}"
               href="${ctx}/intern/profile#documents">
                <i class="bi bi-folder2-open"></i><span>CV và đơn ứng tuyển</span>
            </a>

            <a class="app-nav__link ${activePage eq 'contract' ? 'is-active' : ''}"
               href="${ctx}/contract/view">
                <i class="bi bi-file-earmark-text"></i><span>Hợp đồng thực tập</span>
            </a>

            <div class="app-nav__group">Công việc</div>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-list-check"></i><span>Nhiệm vụ của tôi</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-journal-text"></i><span>Báo cáo tuần</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <div class="app-nav__group">Thời gian và quyền lợi</div>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-clock-history"></i><span>Chấm công</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-calendar-x"></i><span>Nghỉ phép</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-life-preserver"></i><span>Yêu cầu hỗ trợ</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

        </c:if>

        <%-- ============================== HR ============================== --%>
        <c:if test="${sessionScope.user.HR and not sessionScope.user.admin}">

            <div class="app-nav__group">Tuyển dụng</div>

            <a class="app-nav__link ${activePage eq 'interns' ? 'is-active' : ''}"
               href="${ctx}/hr/interns">
                <i class="bi bi-people"></i><span>Hồ sơ thực tập sinh</span>
            </a>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-inbox"></i><span>Duyệt hồ sơ</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <div class="app-nav__group">Chương trình</div>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-diagram-3"></i><span>Chương trình thực tập</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-person-badge"></i><span>Mentor</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <div class="app-nav__group">Thời gian và quyền lợi</div>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-clock-history"></i><span>Chấm công</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-cash-coin"></i><span>Phụ cấp</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <div class="app-nav__group">Báo cáo</div>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-bar-chart"></i><span>Thống kê</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

        </c:if>

        <%-- ============================ MENTOR ============================ --%>
        <c:if test="${sessionScope.user.roleName eq 'MENTOR'}">

            <div class="app-nav__group">Hướng dẫn</div>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-people"></i><span>TTS phụ trách</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-list-check"></i><span>Giao nhiệm vụ</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <div class="app-nav__group">Theo dõi</div>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-journal-text"></i><span>Báo cáo tuần</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-clipboard-check"></i><span>Đánh giá</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

        </c:if>

        <%-- ============================= ADMIN ============================ --%>
        <c:if test="${sessionScope.user.admin}">

            <div class="app-nav__group">Quản trị hệ thống</div>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-person-gear"></i><span>Tài khoản nội bộ</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-shield-lock"></i><span>Phân quyền</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

            <span class="app-nav__link is-soon" aria-disabled="true">
                <i class="bi bi-clock-history"></i><span>Nhật ký hoạt động</span>
                <span class="app-nav__soon">Sắp có</span>
            </span>

        </c:if>

        <%-- Trường hợp chưa đăng nhập hoặc role không xác định --%>
        <c:if test="${empty sessionScope.user}">
            <div class="app-nav__group">Phiên làm việc</div>
            <a class="app-nav__link" href="${ctx}/auth/login">
                <i class="bi bi-box-arrow-in-right"></i><span>Đăng nhập</span>
            </a>
        </c:if>

    </nav>
</aside>
