<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Nghỉ phép của tôi"/><c:set var="activePage" value="intern-leaves"/>
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<section id="internScheduleLeave" data-mode="leave" data-context="${ctx}" data-enrollment="${enrollment.enrollmentId}">
<div class="page-head"><div><h2 class="page-head__title">Nghỉ phép của tôi</h2><p class="page-head__sub">Gửi yêu cầu và theo dõi kết quả HR xét duyệt</p></div></div>
<div id="pageAlert" role="status" aria-live="polite"></div>
<c:choose><c:when test="${not empty enrollment}">
<form id="leaveForm" class="card card-body mb-4"><h3 class="h5">Đăng ký nghỉ</h3><div class="row g-3">
<div class="col-md-6"><label for="leaveFrom" class="form-label">Từ ngày</label><input id="leaveFrom" type="date" class="form-control" required></div>
<div class="col-md-6"><label for="leaveTo" class="form-label">Đến ngày</label><input id="leaveTo" type="date" class="form-control" required></div>
<div class="col-12"><label for="reason" class="form-label">Lý do</label><textarea id="reason" class="form-control" maxlength="1000" rows="3" required></textarea></div>
<div class="col-12"><button class="btn btn-primary">Gửi đơn chờ duyệt</button></div></div></form>
</c:when><c:otherwise><div class="alert alert-info">Bạn chưa có ghi danh hoạt động nên chưa thể đăng ký nghỉ.</div></c:otherwise></c:choose>
<form id="dateFilter" class="card card-body mb-4 d-flex flex-row gap-3 flex-wrap">
<label>Từ ngày<input id="from" type="date" class="form-control" required></label><label>Đến ngày<input id="to" type="date" class="form-control" required></label><button class="btn btn-outline-brand align-self-end">Tra cứu</button></form>
<div class="card table-responsive"><table class="table mb-0"><thead><tr><th>Khoảng nghỉ</th><th>Chương trình</th><th>Lý do</th><th>Trạng thái</th><th>Phản hồi HR</th></tr></thead><tbody id="pageRows"><tr><td colspan="5">Đang tải…</td></tr></tbody></table></div>
</section><script src="${ctx}/assets/js/intern-schedule-leave.js?v=20261013-integration" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
