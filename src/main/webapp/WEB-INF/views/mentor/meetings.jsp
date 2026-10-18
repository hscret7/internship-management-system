<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Lịch họp mentor" />
<c:set var="activePage" value="mentor-meetings" />
<c:set var="pageStylesheet" value="fe3.css" />
<c:set var="pageScript" value="fe3-sprint3.js" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>
<section class="fe3-page" id="fe3Sprint3" data-page="meetings" data-context="${ctx}" data-user-id="${sessionScope.currentUser.userId}">
    <div class="fe3-hero"><div><span class="fe3-hero__eyebrow">ĐỒNG HÀNH CÙNG TTS</span><h2>Lịch họp của mentor</h2><p>Tạo, cập nhật hoặc hủy cuộc họp với các TTS hiện đang được phân công. Thời gian hiển thị theo Việt Nam (UTC+7).</p></div><div class="fe3-hero__mark" aria-hidden="true"><i class="bi bi-calendar-event"></i></div></div>
    <div id="s3Notice" class="alert d-none" role="status" aria-live="polite"></div>
    <div class="fe3-two-column"><section class="fe3-panel"><h2 id="meetingFormTitle">Tạo cuộc họp</h2><form id="meetingForm">
        <label class="form-label" for="meetingProgram">Chương trình</label><select id="meetingProgram" name="programId" class="form-select mb-3" required><option value="">Đang tải…</option></select>
        <label class="form-label" for="meetingTitle">Tiêu đề</label><input id="meetingTitle" name="title" class="form-control mb-3" maxlength="200" required>
        <label class="form-label" for="meetingLocation">Địa điểm / liên kết họp</label><input id="meetingLocation" name="location" class="form-control mb-3" maxlength="500" required>
        <label class="form-label" for="meetingDescription">Nội dung</label><textarea id="meetingDescription" name="description" class="form-control mb-3" rows="3" maxlength="10000"></textarea>
        <label class="form-label" for="meetingStart">Bắt đầu (giờ Việt Nam)</label><input id="meetingStart" name="startsAt" class="form-control mb-3" type="datetime-local" required>
        <label class="form-label" for="meetingEnd">Kết thúc (giờ Việt Nam)</label><input id="meetingEnd" name="endsAt" class="form-control mb-3" type="datetime-local" required>
        <fieldset class="mb-3"><legend class="h6">TTS tham gia</legend><div id="meetingParticipants">Chọn chương trình.</div></fieldset>
        <button id="meetingSave" class="btn btn-primary" type="submit">Lưu cuộc họp</button><button id="meetingReset" class="btn btn-outline-primary" type="button">Tạo mới</button>
    </form></section><section class="fe3-panel"><h2>Cuộc họp trong phạm vi phụ trách</h2><button id="meetingReload" class="btn btn-outline-primary mb-3" type="button">Tải lại</button><div id="meetingList" aria-live="polite">Đang tải…</div><button id="meetingMore" class="btn btn-outline-primary mt-3" type="button" hidden>Xem tiếp</button></section></div>
</section>
<%@ include file="/WEB-INF/views/common/footer.jsp" %>
