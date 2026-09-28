<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Chi tiết nhiệm vụ" />
<c:set var="activePage" value="intern-tasks" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="intern-task-detail" data-context="${ctx}">
    <a href="${ctx}/intern/tasks" class="fe3-back"><i class="bi bi-arrow-left"></i> Quay lại danh sách</a>
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-graph-up-arrow"></i> Theo dõi tiến độ</span>
            <h2>Chi tiết nhiệm vụ</h2>
            <p>Ghi lại tiến độ theo từng mốc để mentor có thể theo dõi và hỗ trợ bạn.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-activity"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-two-column">
        <div class="fe3-panel">
            <h2 id="taskTitle">Đang tải nhiệm vụ…</h2>
            <dl class="fe3-details">
                <dt>Mô tả</dt><dd id="taskDescription">—</dd>
                <dt>Hạn hoàn thành</dt><dd id="taskDueDate">—</dd>
                <dt>Trạng thái</dt><dd id="taskStatus">—</dd>
            </dl>
        </div>
        <div class="fe3-panel">
            <h2>Cập nhật tiến độ</h2>
            <form id="progressForm" class="d-none">
                <label class="form-label" for="completionPercentage">Hoàn thành (%)</label>
                <input class="form-control mb-3" id="completionPercentage" name="completionPercentage" type="number" min="0" max="100" required>
                <label class="form-label" for="progressNote">Ghi chú</label>
                <textarea class="form-control mb-3" id="progressNote" name="note" rows="3"></textarea>
                <button class="btn btn-primary" type="submit">Lưu tiến độ</button>
            </form>
            <p id="progressReadOnly" class="text-muted"></p>
        </div>
    </div>
    <div class="fe3-panel">
        <h2>Lịch sử cập nhật</h2>
        <div id="progressHistory" class="fe3-list" aria-live="polite">Đang tải…</div>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
