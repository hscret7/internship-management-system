<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Lịch làm việc" />
<c:set var="activePage" value="schedules" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<style>
.hr-schedule .panel{border:0;border-radius:1rem;box-shadow:0 .35rem 1.2rem #172b4d12}.hr-schedule .state{min-height:3rem}.hr-schedule .table td{vertical-align:middle}.hr-schedule .help{font-size:.875rem;color:#667085}
</style>
<div class="container-fluid py-4 hr-schedule" id="scheduleApp" data-api="${pageContext.request.contextPath}/api/hr/work-schedules">
 <div class="mb-4"><h2 class="h4 mb-1">Lịch làm việc</h2><p class="text-muted mb-0">Thiết lập ca theo Gmail thực tập sinh, theo dõi lịch và xung đột.</p></div>
 <div id="scheduleAlert" class="state" role="status" aria-live="polite"></div>
 <div class="row g-4">
  <div class="col-xl-4"><section class="card panel"><div class="card-body p-4">
   <h3 class="h5 mb-3">Thiết lập ca làm</h3>
   <form id="scheduleForm" novalidate>
    <input type="hidden" id="csrfToken">
    <div class="mb-3">
        <label class="form-label fw-bold" for="internEmail">
            <i class="bi bi-envelope-at text-primary me-1"></i>Gmail / Email thực tập sinh *
        </label>
        <div class="input-group">
            <span class="input-group-text bg-light"><i class="bi bi-search text-muted"></i></span>
            <input class="form-control" id="internEmail" type="text" list="internEmailList"
                   placeholder="Nhập hoặc chọn Gmail (vd: nam.nv@student.edu.vn)" required autocomplete="off">
            <button class="btn btn-outline-secondary" type="button" id="btnClearEmail" title="Xóa chọn">
                <i class="bi bi-x"></i>
            </button>
        </div>
        <datalist id="internEmailList"></datalist>
        <input type="hidden" id="enrollmentId" name="enrollmentId">
        <div class="invalid-feedback">Vui lòng nhập hoặc chọn Gmail thực tập sinh hợp lệ.</div>

        <!-- Hộp thông tin nhân viên tự động hiển thị khi nhập/chọn Gmail -->
        <div id="internInfoCard" class="mt-2 p-3 bg-light rounded border shadow-sm d-none">
            <div class="d-flex align-items-center mb-2">
                <div class="avatar-circle me-2 bg-primary text-white rounded-circle d-flex align-items-center justify-content-center shadow-sm"
                     style="width: 38px; height: 38px; font-weight: bold; font-size: 13px;" id="cardAvatar">TTS</div>
                <div class="overflow-hidden">
                    <strong id="cardName" class="d-block text-dark text-truncate">—</strong>
                    <small id="cardEmail" class="text-primary font-monospace text-truncate d-block">—</small>
                </div>
            </div>
            <div class="small text-secondary mb-1">
                <div><i class="bi bi-mortarboard me-1 text-muted"></i><span id="cardEdu">—</span></div>
                <div><i class="bi bi-diagram-3 me-1 text-muted"></i><span id="cardProgram" class="fw-semibold text-dark">—</span></div>
            </div>
            <div class="mt-2" id="cardStatusBadge">
                <span class="badge bg-success">Đang thực tập</span>
            </div>
        </div>
    </div>
    <div class="mb-3 d-none" id="scheduleEnrollmentGroup">
     <label class="form-label" for="scheduleEnrollmentChoice">Chương trình thực tập *</label>
     <select class="form-select" id="scheduleEnrollmentChoice" disabled></select>
     <div class="form-text">Email này có nhiều ghi danh. Chọn chương trình cần tạo ca.</div>
    </div>
    <div class="mb-3"><label class="form-label" for="workDate">Ngày làm việc *</label><input class="form-control" id="workDate" type="date" required><div class="invalid-feedback">Chọn ngày làm việc.</div></div>
    <div class="row g-3 mb-3"><div class="col-6"><label class="form-label" for="startTime">Bắt đầu *</label><input class="form-control" id="startTime" type="time" required><div class="invalid-feedback">Chọn giờ bắt đầu.</div></div><div class="col-6"><label class="form-label" for="endTime">Kết thúc *</label><input class="form-control" id="endTime" type="time" required><div class="invalid-feedback">Giờ kết thúc phải sau giờ bắt đầu.</div></div></div>
    <div class="mb-3"><label class="form-label" for="note">Ghi chú</label><textarea class="form-control" id="note" rows="2" maxlength="500"></textarea><div class="form-text">Tối đa 500 ký tự; không hỗ trợ ca qua đêm.</div></div>
    <div class="d-flex flex-wrap gap-2"><button class="btn btn-primary" type="submit">Lưu ca</button><button class="btn btn-outline-primary" type="button" id="bulkToggle">Tạo ca theo nhóm</button></div>
   </form>
   <form id="bulkForm" class="mt-4 pt-3 border-top d-none" novalidate><h4 class="h6 fw-bold"><i class="bi bi-people-fill text-primary me-1"></i>Tạo nhiều ca</h4><label class="form-label" for="enrollmentIds">Danh sách Gmail thực tập sinh, cách nhau bằng dấu phẩy *</label><textarea class="form-control mb-2" id="enrollmentIds" rows="2" required placeholder="nam.nv@student.edu.vn, nguyenqkhanh1626@gmail.com"></textarea><div class="form-text mb-3">Nhập các Gmail (hoặc mã ghi danh) duy nhất, phân tách bằng dấu phẩy. Dùng cùng ngày, giờ và ghi chú bên trên.</div><button class="btn btn-outline-primary" type="submit">Lưu theo nhóm</button></form>
  </div></section></div>
  <div class="col-xl-8"><section class="card panel"><div class="card-body p-4">
   <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3"><h3 class="h5 mb-0">Tra cứu lịch</h3><form id="filterForm" class="row g-2 align-items-end"><div class="col-auto"><label class="form-label small mb-1" for="filterFrom">Từ ngày</label><input class="form-control form-control-sm" id="filterFrom" type="date" required></div><div class="col-auto"><label class="form-label small mb-1" for="filterTo">Đến ngày</label><input class="form-control form-control-sm" id="filterTo" type="date" required></div><div class="col-auto"><label class="form-label small mb-1" for="filterProgram">Mã chương trình</label><input class="form-control form-control-sm" id="filterProgram" type="number" min="1" placeholder="Tất cả"></div><div class="col-auto"><button class="btn btn-sm btn-dark" type="submit">Lọc</button></div></form></div>
   <div class="table-responsive"><table class="table table-hover"><thead><tr><th>Ngày / giờ</th><th>Thực tập sinh</th><th>Chương trình</th><th>Ghi chú</th><th>Trạng thái</th><th></th></tr></thead><tbody id="scheduleRows"><tr><td colspan="6" class="text-center text-muted py-4">Đang tải lịch…</td></tr></tbody></table></div>
  </div></section></div>
 </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/work-schedules.js?v=20261019-enrollment-choice" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
