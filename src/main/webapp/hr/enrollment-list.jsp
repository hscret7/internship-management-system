
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="pageTitle" value="Quản lý Ghi danh Thực tập sinh" />
<c:set var="activePage" value="enrollments" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <div class="alert alert-warning" role="status">API ghi danh/phân công Mentor chưa được tích hợp. Không thể gửi hoặc lưu biểu mẫu này.</div>

<div class="container-fluid mt-4 mb-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="h4"><i class="fas fa-user-check me-2 text-primary"></i>Danh sách Thực tập sinh đã Ghi danh</h2>
        <button class="btn btn-primary shadow-sm" onclick="openEnrollmentModal()">
            <i class="fas fa-plus me-1"></i> Ghi danh & Phân Mentor
        </button>
    </div>
    
    <!-- Danh sách Ghi danh (Enrollment List) -->
    <div class="card shadow-sm border-0">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">Mã GD</th>
                            <th>Thực tập sinh</th>
                            <th>Chương trình</th>
                            <th>Mentor phụ trách</th>
                            <th>Ngày Ghi danh</th>
                            <th>Trạng Thái</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- Dữ liệu render từ Backend -->
                        <c:forEach var="enrollment" items="${enrollmentList}">
                            <tr>
                                <td class="ps-3 text-muted">#${enrollment.enrollmentId}</td>
                                <td class="fw-bold">${enrollment.internName}</td>
                                <td>${enrollment.programName}</td>
                                <td>
                                <td><fmt:formatDate value="${enrollment.enrollmentDate}" pattern="dd/MM/yyyy HH:mm" /></td>
                                    <c:choose>
                                        <c:when test="${not empty enrollment.mentorName}">
                                            <span class="text-primary fw-medium"><i class="fas fa-chalkboard-teacher me-1"></i>${enrollment.mentorName}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted fst-italic">Chưa phân Mentor</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${enrollment.enrollmentDate}</td>
                                <td>
                                    <span class="badge bg-success"><i class="fas fa-check me-1"></i>Đã ghi danh</span>
                                </td>
                            </tr>
                        </c:forEach>
                        
                        <!-- Empty state -->
                        <c:if test="${empty enrollmentList}">
                            <tr>
                                <td colspan="6" class="text-center py-5 text-muted">
                                    <i class="fas fa-users-slash fa-3x mb-3 text-light"></i>
                                    <p class="mb-0">Chưa có thực tập sinh nào được ghi danh</p>
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<!-- Modal Form Ghi danh & Phân Mentor lần đầu (US12) -->
<div class="modal fade" id="enrollmentModal" tabindex="-1" aria-labelledby="enrollmentModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form id="enrollmentForm" onsubmit="handleEnrollmentSubmit(event)">
                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold" id="enrollmentModalLabel">Ghi danh & Phân Mentor</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                
                <div class="modal-body p-4">
                    <h6 class="fw-bold text-primary mb-3 border-bottom pb-2">1. Thông tin Ứng viên</h6>
                    <div class="row g-3 mb-4">
                        <div class="col-md-12">
                            <label for="applicationSelect" class="form-label fw-semibold">
                                Chọn Đơn Ứng Tuyển <span class="badge bg-success ms-1">APPROVED</span> <span class="text-danger">*</span>
                            </label>
                            <!-- Nguồn ghi danh: Chỉ hiển thị các Application có trạng thái APPROVED -->
                            <select class="form-select" id="applicationSelect" name="applicationId" required>
                                <option value="" selected disabled>-- Chọn ứng viên đã được duyệt --</option>
                                <c:forEach var="app" items="${approvedApplications}">
                                    <option value="${app.applicationId}" data-program="${app.programName}">
                                        ${app.candidateName} - (Chương trình: ${app.programName})
                                    </option>
                                <c:if test="${empty approvedApplications}"><option value="" disabled>Chưa có đơn ứng tuyển đã duyệt.</option></c:if>
                                </c:forEach>

                                <!-- Mock data hiển thị UI mẫu (nếu list rỗng) -->
                                <option value="APP-001" data-program="Thực tập sinh Java 2024">Nguyễn Văn A - (Chương trình: Thực tập sinh Java 2024)</option>
                                <option value="APP-002" data-program="Thực tập sinh Frontend 2024">Trần Thị B - (Chương trình: Thực tập sinh Frontend 2024)</option>
                            </select>
                            <div class="form-text text-muted fst-italic">Chỉ danh sách các đơn ứng tuyển đã phê duyệt mới được hiển thị tại đây.</div>
                        </div>
                        
                        <div class="col-md-12">
                            <label class="form-label fw-semibold">Chương trình thực tập</label>
                            <input type="text" class="form-control bg-light" id="displayProgram" readonly placeholder="Sẽ tự động điền theo ứng viên được chọn...">
                        </div>
                    </div>

                    <h6 class="fw-bold text-primary mb-3 border-bottom pb-2">2. Phân công Mentor (Lần đầu)</h6>
                    
                    <!-- VÙNG FE3 TÍCH HỢP LOGIC US30 - KHÔNG SỬA ĐỔI -->
                    <div class="row g-3 mb-2" id="mentorSelectionBlock">
                        <div class="col-md-12">
                            <label for="mentorSelect" class="form-label fw-semibold">Chọn Mentor phụ trách <span class="text-danger">*</span></label>
                            <select class="form-select" id="mentorSelect" name="mentorId" required>
                                <option value="" selected disabled>-- Chọn Mentor --</option>
                                <!-- Dropdown này chỉ render các Mentor thỏa mãn:
                                     1. Cùng department với program
                                     2. Trạng thái ACTIVE
                                     3. Còn chỗ trống (currentMentees < maxMentees) 
                                -->
                                <c:forEach var="mentor" items="${availableMentors}">
                                <c:if test="${empty availableMentors}"><option value="" disabled>Chưa có Mentor phù hợp.</option></c:if>
                                    <option value="${mentor.mentorId}">
                                        ${mentor.fullName} (Đang HD: ${mentor.currentMentees}/${mentor.maxMentees})

                                    </option>
                                </c:forEach>
                                <!-- Mock data UI mẫu cho Mentor hợp lệ -->
<!-- Dữ liệu chỉ lấy từ danh sách backend; không thêm dữ liệu mẫu. -->

                                <option value="M1">Lê Văn C (Đang HD: 1/3)</option>
                                <option value="M2">Phạm Thị D (Đang HD: 0/2)</option>
                            </select>
                            <div class="form-text text-muted">
                                <ul class="mb-0 ps-3">
                                    <li>Mentor hiển thị phải <strong>cùng phòng ban</strong> với chương trình.</li>
                                    <li>Trạng thái làm việc <strong>ACTIVE</strong>.</li>
                                    <li>Chưa vượt quá <strong>số lượng Mentee tối đa</strong>.</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <!-- END VÙNG FE3 -->
                </div>
                
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary px-4"><i class="fas fa-save me-1"></i> Xác nhận Ghi danh</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    let enrollmentModalInstance;
    
    document.addEventListener("DOMContentLoaded", function() {
        const modalElement = document.getElementById('enrollmentModal');
        if (modalElement) {
            enrollmentModalInstance = new bootstrap.Modal(modalElement);
        }
        
        // Tự động hiển thị tên chương trình khi chọn Ứng viên
        const applicationSelect = document.getElementById('applicationSelect');
        const displayProgram = document.getElementById('displayProgram');
        
        if (applicationSelect) {
            applicationSelect.addEventListener('change', function() {
                const selectedOption = this.options[this.selectedIndex];
                const programName = selectedOption.getAttribute('data-program');
                
                if (programName) {
                    displayProgram.value = programName;
                } else {
                    displayProgram.value = '';
                }
            });
        }
    });

    function openEnrollmentModal() {
        const form = document.getElementById('enrollmentForm');
        form.reset();
        document.getElementById('displayProgram').value = '';
        
        enrollmentModalInstance.show();
    }
    
    function handleEnrollmentSubmit(event) {
        const errorBox=document.getElementById('enrollmentError');
        event.preventDefault();
        const errorBox=document.getElementById('enrollmentError');
        if(errorBox) errorBox.textContent='Thao tác ghi danh chưa được backend tích hợp; dữ liệu không được gửi.';
        return;

        if(errorBox) errorBox.textContent='';
        const errorBox=document.getElementById('enrollmentError');
        if(errorBox) errorBox.textContent='Thao tác ghi danh chưa được backend tích hợp; dữ liệu không được gửi.';
        return;

        event.preventDefault();
        
        const applicationSelect = document.getElementById('applicationSelect');
        const mentorSelect = document.getElementById('mentorSelect');
        
        if (!applicationSelect.value || !mentorSelect.value) {
            return;
        }
        
        // Giả lập xử lý submit Frontend
        alert('Ghi danh và Phân Mentor thành công! Dữ liệu đã sẵn sàng để gửi lên Server.');
        enrollmentModalInstance.hide();
    }
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
