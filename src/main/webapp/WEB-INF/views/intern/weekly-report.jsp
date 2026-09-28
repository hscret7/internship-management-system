<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Báo cáo tuần" />
<c:set var="activePage" value="intern-reports" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="intern-reports" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-journal-text"></i> Nhật ký thực tập</span>
            <h2>Báo cáo từng tuần</h2>
            <p>Lưu bản nháp, hoàn thiện nội dung rồi nộp báo cáo để nhận phản hồi từ mentor.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-pencil-square"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-panel">
        <label class="form-label" for="enrollmentSelect">Đợt thực tập</label>
        <select class="form-select" id="enrollmentSelect"><option value="">Đang tải…</option></select>
    </div>
    <div class="fe3-two-column">
        <div class="fe3-panel">
            <h2>Tạo báo cáo nháp</h2>
            <form id="reportCreateForm">
                <label class="form-label" for="weekNumber">Tuần thứ</label>
                <input class="form-control mb-3" id="weekNumber" name="weekNumber" type="number" min="1" max="32767" required>
                <label class="form-label" for="reportContent">Nội dung</label>
                <textarea class="form-control mb-3" id="reportContent" name="content" rows="6"></textarea>
                <button class="btn btn-primary" type="submit">Lưu bản nháp</button>
            </form>
            <h2 class="mt-4">Báo cáo đã tạo</h2>
            <div id="reportList" class="fe3-list" aria-live="polite">Chọn đợt thực tập.</div>
        </div>
        <div class="fe3-panel">
            <h2>Chi tiết báo cáo</h2>
            <div id="reportDetail">Chọn một báo cáo.</div>
            <form id="reportEditForm" class="d-none mt-3">
                <label class="form-label" for="editReportContent">Nội dung bản nháp</label>
                <textarea class="form-control mb-3" id="editReportContent" name="content" rows="8"></textarea>
                <button class="btn btn-outline-primary" type="submit">Lưu chỉnh sửa</button>
                <button class="btn btn-primary" id="reportSubmitButton" type="button">Nộp báo cáo</button>
            </form>
            <div id="feedbackList" class="fe3-feedback-list mt-3"></div>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
