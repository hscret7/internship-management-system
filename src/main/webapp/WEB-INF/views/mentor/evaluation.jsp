<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đánh giá cuối kỳ" />
<c:set var="activePage" value="mentor-evaluation" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="mentor-evaluation" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-award"></i> Tổng kết hành trình</span>
            <h2>Đánh giá cuối kỳ</h2>
            <p>Ghi điểm và nhận xét tổng kết cho thực tập sinh bạn đang hướng dẫn. Mỗi đợt chỉ có một đánh giá.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-clipboard-check"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-panel">
        <label class="form-label" for="enrollmentSelect">Đợt thực tập</label>
        <select class="form-select" id="enrollmentSelect"><option value="">Đang tải…</option></select>
    </div>
    <div class="fe3-panel">
        <div id="evaluationResult" aria-live="polite">Chọn đợt thực tập.</div>
        <form id="evaluationForm" class="d-none mt-3">
            <label class="form-label" for="evaluationScore">Điểm tổng kết</label>
            <input class="form-control mb-3" id="evaluationScore" name="score" type="number" min="0" max="100" step="0.01" required>
            <label class="form-label" for="evaluationComment">Nhận xét</label>
            <textarea class="form-control mb-3" id="evaluationComment" name="comment" rows="5"></textarea>
            <button class="btn btn-primary" type="submit">Lưu đánh giá cuối kỳ</button>
        </form>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
