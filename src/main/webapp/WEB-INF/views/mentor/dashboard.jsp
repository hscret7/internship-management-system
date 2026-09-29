<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tổng quan Mentor" />
<c:set var="activePage" value="mentor-dashboard" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="mentor-dashboard" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-compass"></i> Không gian Mentor</span>
            <h2>Tổng quan hướng dẫn</h2>
            <p>Theo dõi thực tập sinh, tiến độ công việc và các báo cáo cần phản hồi trong một nơi.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-people"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-metrics" aria-label="Thống kê Mentor">
        <div class="fe3-metric"><span><i class="bi bi-people"></i> TTS đang hướng dẫn</span><strong id="metricActiveInterns">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-list-check"></i> Task chưa bắt đầu</span><strong id="metricTasksTodo">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-arrow-repeat"></i> Task đang làm</span><strong id="metricTasksInProgress">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-check-circle"></i> Task hoàn thành</span><strong id="metricTasksDone">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-chat-left-text"></i> Báo cáo chờ phản hồi</span><strong id="metricReportsSubmitted">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-chat-left-dots"></i> Báo cáo đã phản hồi</span><strong id="metricReportsReviewed">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-award"></i> Đánh giá đã thực hiện</span><strong id="metricEvaluationsCompleted">—</strong></div>
    </div>
    <div class="fe3-panel">
        <div class="fe3-panel__heading"><div><span class="fe3-section-label">ĐỢT THỰC TẬP</span><h2>TTS được phân công</h2></div></div>
        <div class="table-responsive">
            <table class="table table-hover align-middle">
                <thead><tr><th>TTS</th><th>Chương trình</th><th>Thời gian</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                <tbody id="enrollmentRows"><tr><td colspan="5">Đang tải…</td></tr></tbody>
            </table>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
