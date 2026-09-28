<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Kết quả đánh giá" />
<c:set var="activePage" value="hr-evaluations" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="hr-evaluations" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-award"></i> Tổng kết thực tập</span>
            <h2>Kết quả đánh giá</h2>
            <p>Tra cứu điểm cuối kỳ và nhận xét của mentor theo từng đợt thực tập.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-clipboard-data"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-metrics fe3-metrics--compact" aria-label="Tổng hợp đánh giá">
        <div class="fe3-metric"><span><i class="bi bi-file-earmark-check"></i> Tổng đánh giá</span><strong id="metricEvaluationCount">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-people"></i> TTS đã đánh giá</span><strong id="metricEvaluatedInterns">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-graph-up-arrow"></i> Điểm trung bình</span><strong id="metricAverageScore">—</strong></div>
    </div>
    <div class="fe3-panel">
        <div class="fe3-panel__heading">
            <div>
                <span class="fe3-section-label">DANH SÁCH</span>
                <h2>Đánh giá cuối kỳ</h2>
            </div>
            <div class="fe3-search">
                <i class="bi bi-search" aria-hidden="true"></i>
                <label class="visually-hidden" for="evaluationSearch">Tìm kiếm đánh giá</label>
                <input class="form-control" id="evaluationSearch" type="search"
                       placeholder="Tìm TTS, mentor, chương trình…" autocomplete="off">
            </div>
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle">
                <thead><tr><th>Thực tập sinh</th><th>Chương trình</th><th>Mentor</th><th>Điểm</th><th>Ngày đánh giá</th><th>Nhận xét</th></tr></thead>
                <tbody id="evaluationRows"><tr><td colspan="6">Đang tải kết quả…</td></tr></tbody>
            </table>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
