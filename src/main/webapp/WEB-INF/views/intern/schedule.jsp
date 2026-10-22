<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Lịch làm việc của tôi"/><c:set var="activePage" value="intern-schedule"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<section id="internScheduleLeave" data-mode="schedule" data-context="${ctx}" data-enrollment="${enrollment.enrollmentId}">
<div class="page-head"><div><h2 class="page-head__title">Lịch làm việc của tôi</h2><p class="page-head__sub">Giờ ca làm theo Việt Nam · Lịch do HR thiết lập</p></div></div>
<div id="pageAlert" role="status" aria-live="polite"></div>
<form id="dateFilter" class="card card-body mb-4 d-flex flex-row gap-3 flex-wrap">
<label>Từ ngày<input id="from" type="date" class="form-control" required></label>
<label>Đến ngày<input id="to" type="date" class="form-control" required></label>
<button class="btn btn-primary align-self-end">Xem lịch</button><a class="btn btn-outline-brand align-self-end" href="${ctx}/intern/leave-requests">Đăng ký nghỉ</a>
</form>
<div class="card table-responsive"><table class="table mb-0"><thead><tr><th>Ngày</th><th>Ca làm</th><th>Chương trình</th><th>Ghi chú</th><th>Trạng thái</th></tr></thead><tbody id="pageRows"><tr><td colspan="5">Đang tải…</td></tr></tbody></table></div>
</section><script src="${ctx}/assets/js/intern-schedule-leave.js?v=20261013-integration" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
