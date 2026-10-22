<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tạo Chương Trình Thực Tập" />
<c:set var="activePage" value="programs" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">
    <div class="row justify-content-center">
        <div class="col-md-8">
            <div class="card shadow-sm">
                <div class="card-header bg-white pb-0 border-bottom-0 mt-3">
                    <h2 class="h4">Tạo Chương Trình Mới</h2>
                </div>
                <div class="card-body">
                    <!-- Form gửi dữ liệu tạo chương trình lên server -->
                    <form action="${pageContext.request.contextPath}/hr/programs/create" method="POST">
    <input type="hidden" name="csrfToken" value="${csrfToken}" />
                        
                        <div class="mb-3">
                            <label for="programName" class="form-label fw-bold">Tên chương trình <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="programName" name="name" required placeholder="Ví dụ: Thực tập sinh Java Web đợt 1...">
                        </div>

                        <div class="mb-3">
                            <label for="departmentId" class="form-label fw-bold">Phòng ban <span class="text-danger">*</span></label>
                            <select class="form-select" id="departmentId" name="department_id" required>
                                <option value="" disabled selected>-- Chọn phòng ban --</option>
                                <!-- Vòng lặp lấy danh sách phòng ban từ BE3 -->
                                <c:forEach var="dept" items="${departmentList}">
                                    <option value="${dept.departmentId}">${dept.name}</option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="startDate" class="form-label fw-bold">Ngày bắt đầu <span class="text-danger">*</span></label>
                                <input type="date" class="form-control" id="startDate" name="start_date" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="endDate" class="form-label fw-bold">Ngày kết thúc <span class="text-danger">*</span></label>
                                <!-- Ngày kết thúc phải sau hoặc bằng ngày bắt đầu theo luồng chốt -->
                                <input type="date" class="form-control" id="endDate" name="end_date" required>
                            </div>
                        </div>

                        <div class="d-flex justify-content-end mt-4">
                            <a href="${pageContext.request.contextPath}/hr/programs" class="btn btn-secondary me-2">Hủy</a>
                            <button type="submit" class="btn btn-primary">Lưu Chương Trình</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>