<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Quản lý Mentor" />
<c:set var="activePage" value="mentors" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="h4">Danh sách Mentor & Phòng ban</h2>
        <!-- Nút thêm Mentor giả định mở một Modal hoặc chuyển trang -->
        <button class="btn btn-primary shadow-sm">
            + Thêm Mentor
        </button>
    </div>

    <div class="card shadow-sm">
        <div class="card-body">
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>Mã Mentor</th>
                            <th>Họ Tên Mentor</th>
                            <th>Email Liên Hệ</th>
                            <th>Phòng Ban</th>
                            <th>Trạng Thái</th>
                            <th class="text-center">Thao Tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- Vòng lặp lấy danh sách mentor từ biến mentorList do BE3 cung cấp -->
                        <c:forEach var="mentor" items="${mentorList}">
                            <tr>
                                <td>#${mentor.mentorId}</td>
                                <td class="fw-bold text-dark">${mentor.fullName}</td>
                                <td>${mentor.email}</td>
                                <td>
                                    <span class="badge bg-info text-dark">${mentor.departmentName}</span>
                                </td>
                                <td>
                                    <!-- Hiển thị trạng thái hoạt động -->
                                    <c:choose>
                                        <c:when test="${mentor.status == 'ACTIVE'}">
                                            <span class="badge bg-success">Đang hoạt động</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary">Tạm nghỉ</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-center">
                                    <button class="btn btn-sm btn-outline-primary">Sửa đổi</button>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>