<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tổng Quan Hệ Thống HR" />
<c:set var="activePage" value="dashboard" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">
    <h2 class="h4 mb-4">Tổng Quan (HR Dashboard)</h2>
    
    <!-- Hàng chứa các thẻ thống kê -->
    <div class="row mb-4">
        <div class="col-md-3">
            <div class="card shadow-sm text-white bg-primary">
                <div class="card-body">
                    <h5 class="card-title">Hồ Sơ Chờ Duyệt</h5>
                    <p class="card-text fs-2 fw-bold">15</p>
                    <a href="${pageContext.request.contextPath}/hr/applications" class="text-white text-decoration-none small">Xem chi tiết ➔</a>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card shadow-sm text-white bg-success">
                <div class="card-body">
                    <h5 class="card-title">TTS Đang Thực Tập</h5>
                    <p class="card-text fs-2 fw-bold">42</p>
                    <a href="${pageContext.request.contextPath}/hr/interns" class="text-white text-decoration-none small">Xem chi tiết ➔</a>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card shadow-sm text-white bg-warning text-dark">
                <div class="card-body">
                    <h5 class="card-title">Chương Trình Active</h5>
                    <p class="card-text fs-2 fw-bold">3</p>
                    <a href="${pageContext.request.contextPath}/hr/programs" class="text-dark text-decoration-none small">Xem chi tiết ➔</a>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card shadow-sm text-white bg-info">
                <div class="card-body">
                    <h5 class="card-title">Mentor Đang Hướng Dẫn</h5>
                    <p class="card-text fs-2 fw-bold">12</p>
                    <a href="${pageContext.request.contextPath}/hr/mentors" class="text-white text-decoration-none small">Xem chi tiết ➔</a>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>