<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Duyệt nghỉ phép" />
<c:set var="activePage" value="leave-requests" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<style>
.hr-leaves .panel{border:0;border-radius:1rem;box-shadow:0 .35rem 1.2rem #172b4d12}.hr-leaves .table td{vertical-align:middle}.hr-leaves .state{min-height:3rem}.hr-leaves .reason{white-space:pre-wrap;overflow-wrap:anywhere;max-width:24rem}
</style>
<div class="container-fluid py-4 hr-leaves" id="leaveApp" data-api="${pageContext.request.contextPath}/api/hr/leave-requests">
 <div class="mb-4"><h2 class="h4 mb-1">Duyệt nghỉ phép</h2><p class="text-muted mb-0">Lọc và xử lý đơn nghỉ của thực tập sinh đang thuộc chương trình.</p></div>
 <div id="leaveAlert" class="state" role="status" aria-live="polite"></div>
 <section class="card panel"><div class="card-body p-4">
  <form id="filterForm" class="row g-3 align-items-end mb-4">
   <div class="col-sm-6 col-lg-2"><label class="form-label" for="filterFrom">Từ ngày *</label><input class="form-control" id="filterFrom" type="date" required></div>
   <div class="col-sm-6 col-lg-2"><label class="form-label" for="filterTo">Đến ngày *</label><input class="form-control" id="filterTo" type="date" required></div>
   <div class="col-sm-6 col-lg-2"><label class="form-label" for="filterProgram">Mã chương trình</label><input class="form-control" id="filterProgram" type="number" min="1" placeholder="Tất cả"></div>
   <div class="col-sm-6 col-lg-2"><label class="form-label" for="filterStatus">Trạng thái</label><select class="form-select" id="filterStatus"><option value="">Tất cả</option><option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Từ chối</option></select></div>
   <div class="col-auto"><button class="btn btn-primary" type="submit">Lọc đơn</button></div>
  </form>
  <div class="table-responsive"><table class="table table-hover align-middle"><thead><tr><th>Thực tập sinh</th><th>Chương trình</th><th>Thời gian nghỉ</th><th>Lý do</th><th>Trạng thái</th><th>Lý do từ chối</th><th>Thao tác</th></tr></thead><tbody id="leaveRows"><tr><td colspan="7" class="text-center text-muted py-4">Đang tải đơn nghỉ…</td></tr></tbody></table></div>
 </div></section>
</div>
<input type="hidden" id="csrfToken" name="csrfToken" value="">

<div class="modal fade" id="rejectModal" tabindex="-1" aria-labelledby="rejectTitle" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content"><form id="rejectForm" novalidate><div class="modal-header"><h2 class="modal-title fs-5" id="rejectTitle">Từ chối đơn nghỉ</h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div><div class="modal-body"><input type="hidden" id="rejectLeaveId"><label class="form-label" for="rejectReason">Lý do từ chối *</label><textarea class="form-control" id="rejectReason" rows="4" maxlength="1000" required></textarea><div class="invalid-feedback">Nhập lý do (tối đa 1000 ký tự).</div><div class="form-text">Lý do sẽ được lưu và hiển thị trong lịch sử đơn.</div></div><div class="modal-footer"><button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button><button type="submit" class="btn btn-danger">Xác nhận từ chối</button></div></form></div></div></div>

<script src="${pageContext.request.contextPath}/assets/js/leave-requests.js?v=20261013-integration" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
