<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Thống kê thực tập" />
<c:set var="activePage" value="hr-dashboard" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="hr-dashboard" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-graph-up-arrow"></i> TOÀN CẢNH THỰC TẬP</span>
            <h2>Thống kê thực tập</h2>
            <p>Số liệu hiện tại theo đợt thực tập, trường, ngành và mentor. Dữ liệu được lấy trực tiếp từ hệ thống.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-bar-chart-line"></i></div>
    </div>

    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>

    <div class="fe3-metrics" aria-label="Số liệu tổng quan">
        <div class="fe3-metric"><span><i class="bi bi-people"></i> Tổng TTS</span><strong id="metricTotalInterns">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-person-check"></i> Đang thực tập</span><strong id="metricActiveEnrollments">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-award"></i> Đã hoàn thành</span><strong id="metricCompletedEnrollments">—</strong></div>
        <div class="fe3-metric"><span><i class="bi bi-pie-chart"></i> Tỷ lệ hoàn thành</span><strong id="metricCompletionRatePercent">—</strong></div>
    </div>

    <div class="row g-3 fe3-dashboard-panels">
        <div class="col-12 col-xl-4">
            <div class="fe3-panel h-100">
                <span class="fe3-section-label">PHÂN BỔ</span>
                <h2>Thực tập sinh theo trường</h2>
                <div id="universityStats" class="fe3-rank-list">Đang tải số liệu…</div>
            </div>
        </div>
        <div class="col-12 col-xl-4">
            <div class="fe3-panel h-100">
                <span class="fe3-section-label">PHÂN BỔ</span>
                <h2>Thực tập sinh theo ngành</h2>
                <div id="majorStats" class="fe3-rank-list">Đang tải số liệu…</div>
            </div>
        </div>
        <div class="col-12 col-xl-4">
            <div class="fe3-panel h-100">
                <span class="fe3-section-label">HƯỚNG DẪN</span>
                <h2>TTS đang tham gia theo mentor</h2>
                <div id="mentorStats" class="fe3-rank-list">Đang tải số liệu…</div>
            </div>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
