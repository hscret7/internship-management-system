<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Lịch làm việc" />
<c:set var="activePage" value="schedules" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<style>
.hr-schedule .panel{border:0;border-radius:1rem;box-shadow:0 .35rem 1.2rem #172b4d12}.hr-schedule .state{min-height:3rem}.hr-schedule .table td{vertical-align:middle}.hr-schedule .help{font-size:.875rem;color:#667085}
</style>
<div class="container-fluid py-4 hr-schedule" id="scheduleApp" data-api="${pageContext.request.contextPath}/api/hr/work-schedules">
 <div class="mb-4"><h2 class="h4 mb-1">Lịch làm việc</h2><p class="text-muted mb-0">Tìm thực tập sinh bằng email, chọn chương trình và thiết lập ca.</p></div>
 <div id="scheduleAlert" class="state" role="status" aria-live="polite"></div>
 <div class="row g-4">
  <div class="col-xl-4"><section class="card panel"><div class="card-body p-4">
   <h3 class="h5 mb-3">Thiết lập ca làm</h3>
   <form id="scheduleForm" novalidate>
    <input type="hidden" id="csrfToken">
    <div class="mb-3"><label class="form-label" for="enrollmentEmail">Email thực tập sinh *</label><div class="input-group"><input class="form-control" id="enrollmentEmail" type="email" maxlength="254" placeholder="Nhập email đầy đủ"><button type="button" id="lookupEnrollment" class="btn btn-outline-primary">Tìm ghi danh</button></div></div>
    <div class="mb-3"><label class="form-label" for="enrollmentId">Chương trình ghi danh *</label><select class="form-select" id="enrollmentId" required><option value="">Tìm bằng email rồi chọn ghi danh</option></select><div class="help mt-1">Nếu có nhiều chương trình, chọn đúng ghi danh trước khi lưu ca.</div><button type="button" id="addBulkEnrollment" class="btn btn-sm btn-outline-secondary mt-2">Thêm ghi danh đã chọn vào nhóm</button></div>
    <div class="mb-3"><label class="form-label" for="workDate">Ngày làm việc *</label><input class="form-control" id="workDate" type="date" required><div class="invalid-feedback">Chọn ngày làm việc.</div></div>
    <div class="row g-3 mb-3"><div class="col-6"><label class="form-label" for="startTime">Bắt đầu *</label><input class="form-control" id="startTime" type="time" required><div class="invalid-feedback">Chọn giờ bắt đầu.</div></div><div class="col-6"><label class="form-label" for="endTime">Kết thúc *</label><input class="form-control" id="endTime" type="time" required><div class="invalid-feedback">Giờ kết thúc phải sau giờ bắt đầu.</div></div></div>
    <div class="mb-3"><label class="form-label" for="note">Ghi chú</label><textarea class="form-control" id="note" rows="2" maxlength="500"></textarea><div class="form-text">Tối đa 500 ký tự; không hỗ trợ ca qua đêm.</div></div>
    <div class="d-flex flex-wrap gap-2"><button class="btn btn-primary" type="submit">Lưu ca</button><button class="btn btn-outline-primary" type="button" id="bulkToggle">Tạo ca theo nhóm</button></div>
   </form>
   <form id="bulkForm" class="mt-4 pt-3 border-top d-none" novalidate><h4 class="h6">Tạo nhiều ca</h4><label class="form-label" for="enrollmentIds">Mã ghi danh, cách nhau bằng dấu phẩy *</label><textarea class="form-control mb-2" id="enrollmentIds" rows="2" required placeholder="101, 102, 103"></textarea><div class="form-text mb-3">Tối đa 100 mã duy nhất. Dùng cùng ngày, giờ và ghi chú bên trên.</div><button class="btn btn-outline-primary" type="submit">Lưu theo nhóm</button></form>
  </div></section></div>
  <div class="col-xl-8"><section class="card panel"><div class="card-body p-4">
   <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3"><h3 class="h5 mb-0">Tra cứu lịch</h3><form id="filterForm" class="row g-2 align-items-end"><div class="col-auto"><label class="form-label small mb-1" for="filterFrom">Từ ngày</label><input class="form-control form-control-sm" id="filterFrom" type="date" required></div><div class="col-auto"><label class="form-label small mb-1" for="filterTo">Đến ngày</label><input class="form-control form-control-sm" id="filterTo" type="date" required></div><div class="col-auto"><label class="form-label small mb-1" for="filterProgram">Mã chương trình</label><input class="form-control form-control-sm" id="filterProgram" type="number" min="1" placeholder="Tất cả"></div><div class="col-auto"><button class="btn btn-sm btn-dark" type="submit">Lọc</button></div></form></div>
   <div class="table-responsive"><table class="table table-hover"><thead><tr><th>Ngày / giờ</th><th>Thực tập sinh</th><th>Chương trình</th><th>Ghi chú</th><th>Trạng thái</th><th></th></tr></thead><tbody id="scheduleRows"><tr><td colspan="6" class="text-center text-muted py-4">Đang tải lịch…</td></tr></tbody></table></div>
  </div></section></div>
 </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/work-schedules.js?v=20261018-email-lookup" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
