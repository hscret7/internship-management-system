<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Danh sách chương trình thực tập" />
<!-- Cấu hình biến activePage để menu sidebar highlight đúng mục Programs -->
<c:set var="activePage" value="programs" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="h4">Quản lý chương trình thực tập</h2>
        <!-- Nút điều hướng sang trang form tạo mới (program-form.jsp) -->
        <a href="${pageContext.request.contextPath}/hr/programs/create" class="btn btn-primary shadow-sm">
            + Tạo chương trình mới
        </a>
    </div>
    
    <div class="card shadow-sm">
        <div class="card-body">
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>Mã Chương Trình</th>
                            <th>Tên Chương Trình</th>
                            <th>Phòng Ban</th>
                            <th>Thời Gian</th>
                            <th>Trạng Thái</th>
                            <th class="text-center">Thao Tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- Giả định BE3 trả về biến programList từ Backend -->
                        <c:forEach var="prog" items="${programList}">
                            <tr>
                                <td>#${prog.programId}</td>
                                <td class="fw-bold text-primary">${prog.name}</td>
                                <td>${prog.departmentName}</td>
                                <td>${prog.startDate} đến ${prog.endDate}</td>
                                <td>
                                    <!-- Render màu sắc Badge dựa trên trạng thái chuẩn nghiệp vụ -->
                                    <c:choose>
                                        <c:when test="${prog.status == 'ACTIVE'}">
                                            <span class="badge bg-success">Đang diễn ra</span>
                                        </c:when>
                                        <c:when test="${prog.status == 'PLANNED'}">
                                            <span class="badge bg-info text-dark">Sắp tới</span>
                                        </c:when>
                                        <c:when test="${prog.status == 'COMPLETED'}">
                                            <span class="badge bg-secondary">Đã kết thúc</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-danger">Đã hủy</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-center">
                                    <button class="btn btn-sm btn-outline-primary">Sửa</button>
                                    <button class="btn btn-sm btn-outline-danger">Hủy</button>
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