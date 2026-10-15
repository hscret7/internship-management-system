<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Trang chủ"/>
<c:set var="activePage" value="home"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid py-4">
    <section class="card border-0 shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5">
            <span class="badge text-bg-primary mb-3">Internship Manager</span>
            <h2 class="fw-bold mb-3">
                Xin chào, <c:out value="${sessionScope.currentUser.fullName}"/>!
            </h2>
            <p class="text-muted mb-0">
                Chào mừng bạn đến với hệ thống quản lý thực tập.
                Chọn chức năng trong thanh bên để bắt đầu công việc.
            </p>
        </div>
    </section>

    <div class="row g-4">
        <div class="col-lg-5">
            <section class="card border-0 shadow-sm h-100">
                <div class="card-body p-4">
                    <h3 class="h5 mb-4">Tài khoản của bạn</h3>
                    <dl class="row mb-0">
                        <dt class="col-sm-5">Họ và tên</dt>
                        <dd class="col-sm-7"><c:out value="${sessionScope.currentUser.fullName}"/></dd>
                        <dt class="col-sm-5">Tên đăng nhập</dt>
                        <dd class="col-sm-7"><c:out value="${sessionScope.currentUser.username}"/></dd>
                        <dt class="col-sm-5">Vai trò</dt>
                        <dd class="col-sm-7">
                            <c:choose>
                                <c:when test="${sessionScope.currentRole eq 'ADMIN'}">Quản trị viên</c:when>
                                <c:when test="${sessionScope.currentRole eq 'HR'}">Nhân sự</c:when>
                                <c:when test="${sessionScope.currentRole eq 'MENTOR'}">Người hướng dẫn</c:when>
                                <c:when test="${sessionScope.currentRole eq 'INTERN'}">Thực tập sinh</c:when>
                                <c:otherwise>Chưa xác định</c:otherwise>
                            </c:choose>
                        </dd>
                    </dl>
                </div>
            </section>
        </div>

        <div class="col-lg-7">
            <section class="card border-0 shadow-sm h-100">
                <div class="card-body p-4">
                    <h3 class="h5 mb-3">Bắt đầu công việc</h3>
                    <c:choose>
                        <c:when test="${sessionScope.currentRole eq 'ADMIN'}">
                            <p class="text-muted">Quản lý tài khoản, phân quyền và theo dõi nhật ký hệ thống.</p>
                            <a class="btn btn-brand" href="${ctx}/admin/accounts">Quản lý tài khoản</a>
                            <a class="btn btn-outline-secondary ms-2" href="${ctx}/admin/roles">Phân quyền</a>
                        </c:when>
                        <c:when test="${sessionScope.currentRole eq 'HR'}">
                            <p class="text-muted">Theo dõi chương trình thực tập, hồ sơ và các yêu cầu cần xử lý.</p>
                            <a class="btn btn-brand" href="${ctx}/hr/dashboard">Xem tổng quan</a>
                            <a class="btn btn-outline-secondary ms-2" href="${ctx}/hr/applications">Duyệt hồ sơ</a>
                        </c:when>
                        <c:when test="${sessionScope.currentRole eq 'MENTOR'}">
                            <p class="text-muted">Theo dõi thực tập sinh, giao nhiệm vụ và phản hồi báo cáo.</p>
                            <a class="btn btn-brand" href="${ctx}/mentor/dashboard">Xem tổng quan</a>
                            <a class="btn btn-outline-secondary ms-2" href="${ctx}/mentor/tasks">Giao nhiệm vụ</a>
                        </c:when>
                        <c:when test="${sessionScope.currentRole eq 'INTERN'}">
                            <p class="text-muted">Cập nhật hồ sơ, theo dõi nhiệm vụ và báo cáo kết quả thực tập.</p>
                            <a class="btn btn-brand" href="${ctx}/intern/profile">Hồ sơ cá nhân</a>
                            <a class="btn btn-outline-secondary ms-2" href="${ctx}/intern/tasks">Nhiệm vụ của tôi</a>
                        </c:when>
                        <c:otherwise>
                            <a class="btn btn-brand" href="${ctx}/auth/login">Đăng nhập</a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
