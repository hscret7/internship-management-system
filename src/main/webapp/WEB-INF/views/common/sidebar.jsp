<%--
    sidebar.jsp - Menu điều hướng theo role. Owner: FE1

    Quy ước:
    - Trang đặt <c:set var="activePage" value="..."/> TRƯỚC khi include header.
    - Mục chưa có route thật (BE/FE khác chưa làm xong) render bằng
      <span class="app-nav__link is-soon">, KHÔNG dùng <a> để tránh link chết 404.
      Khi route sẵn sàng thì đổi span thành a và bỏ class is-soon.

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

    Trước 28/09/2026 file này đọc sessionScope.user - attribute KHÔNG ai set -
    nên cả 4 khối <c:if> theo role đều false và menu luôn rỗng với mọi role.
    ============================================================================
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
        <c:if test="${sessionScope.currentRole eq 'INTERN'}">

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

            <a class="app-nav__link ${activePage eq 'intern-tasks' ? 'is-active' : ''}"
               href="${ctx}/intern/tasks">
                <i class="bi bi-list-check"></i><span>Nhiệm vụ của tôi</span>
            </a>

            <a class="app-nav__link ${activePage eq 'intern-reports' ? 'is-active' : ''}"
               href="${ctx}/intern/reports">
                <i class="bi bi-journal-text"></i><span>Báo cáo tuần</span>
            </a>

            <a class="app-nav__link ${activePage eq 'intern-evaluation' ? 'is-active' : ''}"
               href="${ctx}/intern/evaluation">
                <i class="bi bi-clipboard-check"></i><span>Đánh giá cuối kỳ</span>
            </a>

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
        <%-- ADMIN đi qua nhánh ADMIN bên dưới; AuthorizationFilter cũng cho ADMIN
             qua mọi /hr/*, nên nếu cần menu HR cho ADMIN thì bổ sung có chủ đích. --%>
        <c:if test="${sessionScope.currentRole eq 'HR'}">

            <div class="app-nav__group">Tuyển dụng</div>

            <a class="app-nav__link ${activePage eq 'interns' ? 'is-active' : ''}"
               href="${ctx}/hr/interns">
                <i class="bi bi-people"></i><span>Hồ sơ thực tập sinh</span>
            </a>

            <a class="app-nav__link ${activePage eq 'hr-applications' ? 'is-active' : ''}"
               href="${ctx}/hr/applications">
                <i class="bi bi-inbox"></i><span>Duyệt hồ sơ</span>
            </a>

            <div class="app-nav__group">Chương trình</div>

            <a class="app-nav__link ${activePage eq 'hr-catalog' ? 'is-active' : ''}"
               href="${ctx}/hr/catalog">
                <i class="bi bi-diagram-3"></i><span>Chương trình thực tập</span>
            </a>

            <a class="app-nav__link ${activePage eq 'hr-enrollments' ? 'is-active' : ''}"
               href="${ctx}/hr/enrollments">
                <i class="bi bi-person-badge"></i><span>Ghi danh &amp; phân mentor</span>
            </a>

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

            <a class="app-nav__link ${activePage eq 'hr-evaluations' ? 'is-active' : ''}"
               href="${ctx}/hr/evaluations">
                <i class="bi bi-award"></i><span>Kết quả đánh giá</span>
            </a>

            <a class="app-nav__link ${activePage eq 'hr-dashboard' ? 'is-active' : ''}"
               href="${ctx}/hr/dashboard">
                <i class="bi bi-bar-chart"></i><span>Thống kê</span>
            </a>

        </c:if>

        <%-- ============================ MENTOR ============================ --%>
        <c:if test="${sessionScope.currentRole eq 'MENTOR'}">

            <div class="app-nav__group">Hướng dẫn</div>

            <a class="app-nav__link ${activePage eq 'mentor-dashboard' ? 'is-active' : ''}"
               href="${ctx}/mentor/dashboard">
                <i class="bi bi-speedometer2"></i><span>Dashboard và TTS</span>
            </a>

            <a class="app-nav__link ${activePage eq 'mentor-tasks' ? 'is-active' : ''}"
               href="${ctx}/mentor/tasks">
                <i class="bi bi-list-check"></i><span>Giao nhiệm vụ</span>
            </a>

            <div class="app-nav__group">Theo dõi</div>

            <a class="app-nav__link ${activePage eq 'mentor-reports' ? 'is-active' : ''}"
               href="${ctx}/mentor/reports">
                <i class="bi bi-journal-text"></i><span>Báo cáo tuần</span>
            </a>

            <a class="app-nav__link ${activePage eq 'mentor-evaluation' ? 'is-active' : ''}"
               href="${ctx}/mentor/evaluation">
                <i class="bi bi-clipboard-check"></i><span>Đánh giá</span>
            </a>

        </c:if>

        <%-- ============================= ADMIN ============================ --%>
        <c:if test="${sessionScope.currentRole eq 'ADMIN'}">

            <div class="app-nav__group">Quản trị hệ thống</div>

            <a class="app-nav__link ${activePage eq 'hr-evaluations' ? 'is-active' : ''}"
               href="${ctx}/hr/evaluations">
                <i class="bi bi-award"></i><span>Kết quả đánh giá</span>
            </a>

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
        <c:if test="${empty sessionScope.currentUser}">
            <div class="app-nav__group">Phiên làm việc</div>
            <a class="app-nav__link" href="${ctx}/auth/login">
                <i class="bi bi-box-arrow-in-right"></i><span>Đăng nhập</span>
            </a>
        </c:if>

    </nav>
</aside>
