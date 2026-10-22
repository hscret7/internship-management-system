<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Báo cáo cuối kỳ" />
<c:set var="activePage" value="hr-end-term" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3-sprint3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<section class="fe3-page" id="fe3Sprint3" data-page="end-term" data-context="${ctx}">
    <div class="fe3-hero"><div><span class="fe3-hero__eyebrow">TỔNG KẾT THỰC TẬP</span><h2>Báo cáo cuối kỳ</h2><p>Đối chiếu công việc, báo cáo tuần và lịch sử đánh giá trên từng đợt thực tập.</p><a class="btn btn-outline-primary" href="${ctx}/hr/evaluations">Xem đánh giá thang 100 cũ</a></div><div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-file-earmark-bar-graph"></i></div></div>
    <div id="s3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <section class="fe3-panel"><form id="endTermFilter" class="row g-3 align-items-end">
        <div class="col-md-3"><label class="form-label" for="programFilter">Mã chương trình (để trống: tất cả)</label><input id="programFilter" name="programId" class="form-control" type="number" min="1"></div>
        <div class="col-md-3" data-email-selection="enrollment" data-context="${ctx}" data-optional="true"><input id="enrollmentFilter" name="enrollmentId" type="hidden" data-email-value></div>
        <div class="col-md-3"><label class="form-label" for="statusFilter">Trạng thái đợt</label><select id="statusFilter" name="status" class="form-select"><option value="">Tất cả</option><option value="ACTIVE">Đang tham gia</option><option value="COMPLETED">Hoàn thành</option><option value="CANCELLED">Đã hủy</option></select></div>
        <div class="col-md-3"><button class="btn btn-primary" type="submit">Áp dụng bộ lọc</button></div>
    </form><p class="text-muted mt-3 mb-0">Tra cứu email để chọn đợt thực tập. Để trống email để xem tất cả; mỗi dòng tương ứng một ghi danh.</p></section>
    <section class="fe3-panel"><div class="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3"><div><h2>Kết quả theo bộ lọc</h2><p id="reportSnapshot" class="text-muted mb-0"></p></div><div class="d-flex gap-2"><button id="exportExcel" class="btn btn-outline-primary" type="button">Xuất Excel</button><button id="exportPdf" class="btn btn-outline-primary" type="button">Xuất PDF</button></div></div>
    <div class="table-responsive"><table class="table table-hover align-middle"><thead><tr><th>TTS / ghi danh</th><th>Chương trình</th><th>Trường / ngành</th><th>Task hoàn thành</th><th>Báo cáo đã nộp / phản hồi</th><th>Đánh giá mới nhất</th><th>Lịch sử</th></tr></thead><tbody id="endTermRows"><tr><td colspan="7">Đang tải dữ liệu…</td></tr></tbody></table></div>
    <div class="d-flex justify-content-between align-items-center"><button id="reportPrevious" class="btn btn-outline-primary" type="button">Trang trước</button><span id="reportPage"></span><button id="reportNext" class="btn btn-outline-primary" type="button">Trang sau</button></div>
    </section><section id="assessmentHistory" class="fe3-panel" tabindex="-1" hidden><h2>Lịch sử đánh giá</h2><div id="assessmentHistoryRows"></div></section>
</section>
<script src="${ctx}/assets/js/email-selection.js?v=20261019-email-suggest" defer></script>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
