<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Giao và theo dõi nhiệm vụ" />
<c:set var="activePage" value="mentor-tasks" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<section class="fe3-page" id="fe3App" data-page="mentor-tasks" data-context="${ctx}">
    <div class="fe3-hero">
        <div>
            <span class="fe3-hero__eyebrow"><i class="bi bi-list-check"></i> Công việc của Mentor</span>
            <h2>Giao nhiệm vụ rõ ràng, theo dõi tiến độ dễ dàng</h2>
            <p>Chọn thực tập sinh, đặt hạn hoàn thành và xem từng lần cập nhật tiến độ.</p>
        </div>
        <div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-kanban"></i></div>
    </div>
    <div id="fe3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-panel">
        <label class="form-label" for="enrollmentSelect">Đợt thực tập</label>
        <select class="form-select" id="enrollmentSelect"><option value="">Đang tải…</option></select>
    </div>
    <div class="fe3-two-column">
        <div class="fe3-panel">
            <h2>Tạo và giao nhiệm vụ</h2>
            <form id="taskCreateForm">
                <label class="form-label" for="taskTitle">Tên nhiệm vụ</label>
                <input class="form-control mb-3" id="taskTitle" name="title" maxlength="255" required>
                <label class="form-label" for="taskDescription">Mô tả</label>
                <textarea class="form-control mb-3" id="taskDescription" name="description" rows="3" maxlength="10000" required></textarea>
                <label class="form-label" for="taskDueDate">Hạn hoàn thành</label>
                <input class="form-control mb-3" id="taskDueDate" name="dueDate" type="date" required>
                <button class="btn btn-primary" type="submit">Tạo và giao cho TTS đã chọn</button>
            </form>
        </div>
        <div class="fe3-panel">
            <h2>Giao lại task đã tạo</h2>
            <p class="text-muted">Dùng cho task cũ chưa được giao. Task mới được tạo và giao trong cùng một lần lưu.</p>
            <form id="taskAssignForm">
                <label class="form-label" for="taskId">Mã task</label>
                <input class="form-control mb-3" id="taskId" name="taskId" type="number" min="1" required>
                <button class="btn btn-outline-primary" type="submit">Giao task</button>
            </form>
        </div>
    </div>
    <div class="fe3-panel">
        <h2>Nhiệm vụ đã giao</h2>
        <div class="table-responsive">
            <table class="table table-hover align-middle">
                <thead><tr><th>Nhiệm vụ</th><th>Hạn</th><th>Trạng thái</th><th>Tiến độ</th><th></th></tr></thead>
                <tbody id="taskRows"><tr><td colspan="5">Chọn đợt thực tập.</td></tr></tbody>
            </table>
        </div>
        <div id="progressPanel" class="fe3-subpanel d-none" aria-live="polite"></div>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
