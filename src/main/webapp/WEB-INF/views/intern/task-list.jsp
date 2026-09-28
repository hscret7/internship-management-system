<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Nhiệm vụ của tôi" />
<c:set var="activePage" value="intern-tasks" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="intern-tasks" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-check2-square"></i> Công việc của tôi</span>
            <h2>Mỗi nhiệm vụ, một bước tiến</h2>
            <p>Xem việc được giao, hạn hoàn thành và cập nhật tiến độ cho mentor.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-clipboard2-check"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-panel">
        <label class="form-label" for="enrollmentSelect">Đợt thực tập</label>
        <select class="form-select" id="enrollmentSelect"><option value="">Đang tải…</option></select>
    </div>
    <div class="fe3-panel">
        <h2>Task đã được giao</h2>
        <div class="table-responsive">
            <table class="table table-hover align-middle">
                <thead><tr><th>Nhiệm vụ</th><th>Hạn</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody id="taskRows"><tr><td colspan="4">Chọn đợt thực tập.</td></tr></tbody>
            </table>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
