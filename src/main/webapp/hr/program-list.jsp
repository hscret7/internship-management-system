<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="pageTitle" value="Danh sách chương trình thực tập" />
<!-- Cấu hình biến activePage để menu sidebar highlight đúng mục Programs -->
<c:set var="activePage" value="programs" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<!-- Bổ sung FontAwesome -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

<div class="container-fluid mt-4 mb-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="h4"><i class="fas fa-graduation-cap me-2 text-primary"></i>Quản lý chương trình thực tập</h2>
        <!-- Nút mở Modal Tạo Mới -->
        <button class="btn btn-primary shadow-sm" onclick="openAddModal()">
            <i class="fas fa-plus me-1"></i> Tạo chương trình mới
        </button>
    </div>
    
    <div class="card shadow-sm border-0">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">Mã CT</th>
                            <th>Tên Chương Trình</th>
                            <th>Phòng Ban</th>
                            <th>Thời Gian</th>
                                        <fmt:formatDate value="${prog.startDate}" pattern="dd/MM/yyyy" /> <i class="fas fa-arrow-right mx-1 text-muted" style="font-size: 0.8em;"></i> <fmt:formatDate value="${prog.endDate}" pattern="dd/MM/yyyy" />
                            <th>Trạng Thái</th>
                            <th class="text-center pe-3">Thao Tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- Giả định BE trả về biến programList từ Backend -->
                        <c:forEach var="prog" items="${programList}">
                            <tr>
                                <td class="ps-3 text-muted">#${prog.programId}</td>
                                <td class="fw-bold text-primary">${prog.name}</td>
                                <td>${prog.departmentName}</td>
                                <td>
                                    <div class="d-flex align-items-center">
                                        <i class="far fa-calendar-alt text-muted me-2"></i>
                                        <span>${prog.startDate} <i class="fas fa-arrow-right mx-1 text-muted" style="font-size: 0.8em;"></i> ${prog.endDate}</span>
                                    </div>
                                </td>
                                <td>
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
                                <td class="text-center pe-3">
                                    <!-- Nút Sửa gọi hàm openEditModal -->
                                    <button class="btn btn-sm btn-outline-primary me-1" 
                                            onclick="openEditModal('${prog.programId}', '${prog.name}', '${prog.departmentName}', '${prog.startDate}', '${prog.endDate}', '${prog.description}', ${prog.hasEnrollment != null ? prog.hasEnrollment : false}, '${prog.status}')"
                                            title="Sửa chương trình">
                                        <i class="fas fa-edit"></i>
                                    </button>
                                    <button class="btn btn-sm btn-outline-danger" title="Hủy chương trình">
                                        <i class="fas fa-trash-alt"></i>
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty programList}">
                            <tr>
                                <td colspan="6" class="text-center py-5 text-muted">
                                    <i class="fas fa-box-open fa-3x mb-3 text-light"></i>
                                    <p class="mb-0">Chưa có dữ liệu chương trình thực tập</p>
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<!-- Modal Form Thêm/Sửa Chương Trình -->
<div class="modal fade" id="programModal" tabindex="-1" aria-labelledby="modalTitle" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form id="programForm" onsubmit="handleFormSubmit(event)">
                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold" id="modalTitle">Tạo chương trình mới</h5>
                    <div id="modalStatusBadge" class="ms-3"></div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <input type="hidden" id="programId" name="programId">
                    
                    <div class="row g-3">
                        <!-- Alert Khóa Trường (Hiển thị khi có Enrollment) -->
                        <div class="col-12" id="enrollmentWarning" style="display: none;">
                            <div class="alert alert-warning d-flex align-items-center mb-0 py-2">
                                <i class="fas fa-exclamation-triangle me-2 fs-5"></i>
                                <div>
                                    Chương trình này đã có thực tập sinh ghi danh. 
                                    <strong>Phòng ban</strong> và <strong>Thời gian</strong> không thể thay đổi.
                                </div>
                            </div>
                        </div>

                        <div class="col-md-12">
                            <label for="programName" class="form-label fw-semibold">Tên chương trình <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="programName" name="name" required placeholder="VD: Thực tập sinh Java Backend 2024">
                        </div>
                        
                        <div class="col-md-12">
                            <label for="department" class="form-label fw-semibold">Phòng ban <span class="text-danger">*</span></label>
                            <select class="form-select" id="department" name="department" required>
                                <option value="" selected disabled>-- Chọn phòng ban --</option>
                                <option value="IT">IT - Công nghệ thông tin</option>
                                <option value="HR">HR - Nhân sự</option>
                                <option value="MKT">Marketing</option>
                                <option value="SALES">Sales</option>
                            </select>
                        </div>
                        
                        <div class="col-md-6">
                            <label for="startDate" class="form-label fw-semibold">Ngày bắt đầu <span class="text-danger">*</span></label>
                            <input type="date" class="form-control" id="startDate" name="startDate" required>
                        </div>
                        
                        <div class="col-md-6">
                            <label for="endDate" class="form-label fw-semibold">Ngày kết thúc <span class="text-danger">*</span></label>
                            <input type="date" class="form-control" id="endDate" name="endDate" required>
                            <div class="invalid-feedback" id="dateErrorMsg">
                                Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu.
                            </div>
                        </div>
                        
                        <div class="col-md-12">
                            <label for="description" class="form-label fw-semibold">Mô tả chương trình</label>
                            <textarea class="form-control" id="description" name="description" rows="4" placeholder="Nhập mô tả chi tiết..."></textarea>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary px-4"><i class="fas fa-save me-1"></i> Lưu thông tin</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    let programModalInstance;
    
    document.addEventListener("DOMContentLoaded", function() {
        // Khởi tạo Bootstrap Modal
        const modalElement = document.getElementById('programModal');
        if (modalElement) {
            programModalInstance = new bootstrap.Modal(modalElement);
        }
        
        // Lắng nghe sự kiện để validate ngày realtime
        const startDateInput = document.getElementById('startDate');
        const endDateInput = document.getElementById('endDate');
        
        if (startDateInput && endDateInput) {
            startDateInput.addEventListener('change', validateDates);
            endDateInput.addEventListener('change', validateDates);
        }
    });

    // Validate: Ngày kết thúc >= Ngày bắt đầu
    function validateDates() {
        const startDateInput = document.getElementById('startDate');
        const endDateInput = document.getElementById('endDate');
        
        if (startDateInput.value && endDateInput.value) {
            const start = new Date(startDateInput.value);
            const end = new Date(endDateInput.value);
            
            if (start > end) {
                endDateInput.classList.add('is-invalid');
                return false;
            } else {
                endDateInput.classList.remove('is-invalid');
                return true;
            }
        }
        return true;
    }

    // Hàm mở Modal Tạo mới
    function openAddModal() {
        const form = document.getElementById('programForm');
        form.reset();
        
        document.getElementById('programId').value = '';
        document.getElementById('modalTitle').innerText = 'Tạo chương trình mới';
        
        // Mặc định trạng thái tạo mới là PLANNED
        document.getElementById('modalStatusBadge').innerHTML = '<span class="badge bg-info text-dark">PLANNED</span>';
        
        // Mở khóa các trường
        toggleRestrictedFields(false);
        
        // Reset validation
        document.getElementById('endDate').classList.remove('is-invalid');
        
        programModalInstance.show();
    }

    // Hàm mở Modal Sửa
    function openEditModal(id, name, dept, start, end, desc, hasEnrollment, status) {
        const form = document.getElementById('programForm');
        form.reset();
        
        document.getElementById('modalTitle').innerText = 'Cập nhật chương trình';
        
        // Render Badge trạng thái
        let badgeHtml = '';
        if (status === 'ACTIVE') badgeHtml = '<span class="badge bg-success">Đang diễn ra</span>';
        else if (status === 'PLANNED') badgeHtml = '<span class="badge bg-info text-dark">Sắp tới</span>';
        else if (status === 'COMPLETED') badgeHtml = '<span class="badge bg-secondary">Đã kết thúc</span>';
        else badgeHtml = '<span class="badge bg-danger">Đã hủy</span>';
        
        document.getElementById('modalStatusBadge').innerHTML = badgeHtml;
        
        // Điền dữ liệu vào form
        document.getElementById('programId').value = id;
        document.getElementById('programName').value = name;
        
        // Xử lý select phòng ban
        const deptSelect = document.getElementById('department');
        const optionExists = Array.from(deptSelect.options).some(opt => opt.value === dept || opt.text === dept);
        if(optionExists) {
           Array.from(deptSelect.options).forEach(opt => {
               if(opt.value === dept || opt.text === dept) opt.selected = true;
           });
        }
        
        document.getElementById('startDate').value = start;
        document.getElementById('endDate').value = end;
        
        if (desc && desc !== 'undefined' && desc !== 'null') {
             document.getElementById('description').value = desc;
        } else {
             document.getElementById('description').value = '';
        }
        
        // Xử lý logic khóa field nếu đã có enrollment
        toggleRestrictedFields(hasEnrollment === true || hasEnrollment === 'true');
        
        // Reset validation
        document.getElementById('endDate').classList.remove('is-invalid');
        
        programModalInstance.show();
    }
    
    // Hàm bật/tắt các trường dữ liệu
    function toggleRestrictedFields(disabled) {
        document.getElementById('department').disabled = disabled;
        document.getElementById('startDate').disabled = disabled;
        document.getElementById('endDate').disabled = disabled;
        
        const warningBox = document.getElementById('enrollmentWarning');
        warningBox.style.display = disabled ? 'block' : 'none';
    }

    // Hàm xử lý Submit form
    function handleFormSubmit(event) {
        event.preventDefault();
        
        if (!validateDates()) {
            return; // Dừng submit nếu ngày sai
        }
        
        // Tại đây thực hiện gọi API Backend (fetch/axios) hoặc submit form.
        // Giả lập thành công:
        alert('Lưu thông tin thành công!');
        programModalInstance.hide();
    }
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>