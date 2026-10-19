<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Kết quả đánh giá thực tập sinh" />
<c:set var="activePage" value="hr-evaluations" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<style>
    @media print {
        header, footer, nav, .navbar, .fe3-hero, .no-print, .fe3-search, .btn {
            display: none !important;
        }
        body { background: #fff !important; }
        .fe3-page { max-width: 100% !important; margin: 0 !important; padding: 0 !important; }
        .table { font-size: 10pt; }
    }
</style>

<section class="fe3-page" id="fe3App" data-page="hr-evaluations" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-award"></i> Quản lý chất lượng & Đánh giá</span>
            <h2>Kết quả đánh giá thực tập sinh</h2>
            <p>Tổng hợp kết quả đánh giá thực tập (Kỹ năng chuyên môn, Thái độ làm việc & Xếp loại) do Người hướng dẫn (Mentor) trực tiếp đánh giá.</p>
            <div class="d-flex flex-wrap gap-2 mt-3">
                <a class="btn btn-primary" href="${ctx}/hr/end-term-report"><i class="bi bi-file-earmark-spreadsheet me-1"></i> Báo cáo cuối kỳ & Xuất file</a>
                <button class="btn btn-outline-light" onclick="window.print()"><i class="bi bi-printer me-1"></i> In bảng đánh giá</button>
            </div>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-clipboard-data"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-metrics fe3-metrics--compact" aria-label="Tổng hợp đánh giá">
        <div class="fe3-metric"><span><i class="bi bi-file-earmark-check"></i> Tổng số đánh giá</span><strong id="metricEvaluationCount">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-people"></i> TTS đã đánh giá</span><strong id="metricEvaluatedInterns">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-graph-up-arrow"></i> Điểm TB hiển thị (quy đổi /100)</span><strong id="metricAverageScore">—</strong></div>
    </div>
    <div class="fe3-panel">
        <div class="fe3-panel__heading">
            <div>
                <span class="fe3-section-label">DANH SÁCH TOÀN HỆ THỐNG</span>
                <h2>Chi tiết đánh giá thực tập</h2>
            </div>
            <div class="fe3-search">
                <i class="bi bi-search" aria-hidden="true"></i>
                <label class="visually-hidden" for="evaluationSearch">Tìm kiếm đánh giá</label>
                <input class="form-control" id="evaluationSearch" type="search"
                       placeholder="Tìm TTS, mentor, chương trình, xếp loại…" autocomplete="off">
            </div>
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle">
                <thead><tr><th>Thực tập sinh</th><th>Chương trình</th><th>Mentor hướng dẫn</th><th>Kết quả & Xếp loại</th><th>Ngày đánh giá</th><th>Nhận xét & Đề xuất</th></tr></thead>
                <tbody id="evaluationRows"><tr><td colspan="6">Đang tải kết quả…</td></tr></tbody>
            </table>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
