<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Phân Công Mentor" />
<c:set var="activePage" value="mentors" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">
    <div class="alert alert-warning" role="status">Chức năng phân công Mentor chưa có API backend tương ứng nên hiện chưa khả dụng.</div>
    <div class="row justify-content-center">
        <div class="col-md-8">
            <div class="card shadow-sm">
                <div class="card-header bg-white pb-0 border-bottom-0 mt-3">
                    <h2 class="h4">Phân Công Mentor Hướng Dẫn</h2>
                </div>
                <div class="card-body">
                    <!-- Form gửi dữ liệu phân công lên API của BE3 -->
                    <form action="${pageContext.request.contextPath}/hr/mentor-assignment/save" method="POST">
    <input type="hidden" name="csrfToken" value="${csrfToken}" />
                        
                        <div class="mb-3">
                            <label class="form-label fw-bold">Thực Tập Sinh (Đợt Thực Tập) <span class="text-danger">*</span></label>
                            <select class="form-select" name="enrollment_id" required>
                                <option value="" disabled selected>-- Chọn thực tập sinh --</option>
                                <!-- BE3 sẽ truyền biến enrollmentList chứa các đợt thực tập ACTIVE -->
                                <c:forEach var="enrollment" items="${enrollmentList}">
                                    <option value="${enrollment.enrollmentId}">
                                        ${enrollment.internName} - ${enrollment.programName}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="mb-4">
                            <label class="form-label fw-bold">Mentor Hướng Dẫn <span class="text-danger">*</span></label>
                            <select class="form-select" name="mentor_id" required>
                                <option value="" disabled selected>-- Chọn Mentor --</option>
                                <!-- BE3 sẽ truyền biến mentorList -->
                                <c:forEach var="mentor" items="${mentorList}">
                                    <option value="${mentor.mentorId}">
                                        ${mentor.fullName} (${mentor.departmentName})
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="form-text">Lưu ý: Nếu đổi Mentor, hệ thống sẽ tự động kết thúc phân công cũ và tạo phân công mới.</div>
                        </div>

                        <div class="d-flex justify-content-end mt-4">
                            <a href="${pageContext.request.contextPath}/hr/mentors" class="btn btn-light border me-2">Hủy</a>
                            <button type="submit" class="btn btn-primary">Xác Nhận Phân Công</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>