<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đánh giá cuối kỳ" />
<c:set var="activePage" value="intern-evaluation" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="intern-evaluation" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-award"></i> Thành quả của tôi</span>
            <h2>Đánh giá cuối kỳ</h2>
            <p>Xem kết quả và nhận xét của mentor cho từng đợt thực tập đã tham gia.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-trophy"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-panel">
        <label class="form-label" for="enrollmentSelect">Đợt thực tập</label>
        <select class="form-select" id="enrollmentSelect"><option value="">Đang tải…</option></select>
    </div>
    <div class="fe3-panel" id="evaluationResult" aria-live="polite">Chọn đợt thực tập.</div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
