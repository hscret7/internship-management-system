<%--
    intern/notifications.jsp - Man hinh "Thong bao cua toi" cho Thuc tap sinh (US36).
    Owner: FE1.

    Route BE1: /intern/notifications -> JSP này, chỉ cho INTERN ACTIVE.
    Backend JSON /notifications vẫn dùng chung cho các vai trò đăng nhập.
    Sidebar bật liên kết trang; chuông thông báo dùng chung không thay đổi.

    HOP DONG DU LIEU (docs/BE1_US36_INAPP_CONTRACT.md):
        GET  ${ctx}/notifications?page&size&unreadOnly
             -> {items:[{id,kind,sourceType,sourceId,sourceRevision,title,body,
                         availableAt,readAt}],unreadCount,page,size,csrfToken}
             Mac dinh page=1 size=20 unreadOnly=false; size toi da 100.
             unreadCount la TONG so chua doc cua nguoi dung, khong chi trong trang.
             readAt=null nghia la chua doc.
        POST ${ctx}/notifications  (application/x-www-form-urlencoded)
             action=read&id=<id>&csrfToken=<token tu lan GET gan nhat>
             -> {"id":12,"read":true}
        KHONG co endpoint danh dau tat ca da doc -> trang chi co nut tung thong bao.

    QUYEN: /notifications khong nam duoi /admin|/hr|/mentor|/intern nen
    AuthorizationFilter khong chan theo vai tro; servlet chi doi co session
    currentUser. Tuy nhien route TRANG chi mo cho INTERN (giong
    /intern/meetings) vi day la man hinh trong khu vuc Thuc tap sinh.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Thông báo của tôi" />
<c:set var="activePage" value="intern-notifications" />
<c:set var="pageScript" value="intern-notifications.js" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<%-- data-context de JS biet context path, giong quy uoc cua cac trang FE3
     (data-context="${ctx}"). --%>
<div class="container-fluid mt-4" id="notificationsApp" data-context="${ctx}">

    <div class="page-head mb-4">
        <div>
            <h2 class="page-head__title">Thông báo của tôi</h2>
            <p class="page-head__sub">
                Cuộc họp, nhiệm vụ và các mốc thời gian trong chương trình thực tập.
            </p>
        </div>
        <span class="text-muted small" id="notificationUnreadCount"></span>
    </div>

    <%-- Vung thong bao dung chung cho ca trang thai loi/rong. JS do noi dung bang
         textContent nen khong co duong XSS tu du lieu API. --%>
    <div id="notificationsNotice" class="alert d-none" role="status" aria-live="polite"></div>

    <div class="card shadow-sm mb-4">
        <div class="card-body d-flex flex-wrap gap-3 align-items-end">
            <div>
                <label class="form-label mb-1" for="notificationUnreadOnly">Hiển thị</label>
                <select class="form-select form-select-sm" id="notificationUnreadOnly">
                    <option value="">Tất cả</option>
                    <option value="true">Chỉ chưa đọc</option>
                </select>
            </div>
            <div>
                <label class="form-label mb-1" for="notificationSize">Số dòng mỗi trang</label>
                <select class="form-select form-select-sm" id="notificationSize">
                    <option value="10">10</option>
                    <option value="20" selected>20</option>
                    <option value="50">50</option>
                </select>
            </div>
            <button type="button" class="btn btn-outline-brand btn-sm" id="notificationReload">
                <i class="bi bi-arrow-clockwise me-1"></i>Tải lại
            </button>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
            <h5 class="card-title mb-0 fw-bold">Danh sách thông báo</h5>
            <span class="text-muted small" id="notificationPageLabel"></span>
        </div>
        <div class="card-body p-0">
            <%-- Dong "Đang tải…" nay la trang thai mac dinh trong HTML: nguoi dung
                 thay ngay ca khi JS loi hoac bi chan. JS se thay the. --%>
            <div id="notificationList">
                <p class="text-center text-muted py-4 mb-0">Đang tải…</p>
            </div>
        </div>
        <div class="card-footer bg-white d-flex justify-content-between align-items-center">
            <button type="button" class="btn btn-outline-brand btn-sm" id="notificationPrev" disabled>
                <i class="bi bi-chevron-left me-1"></i>Trang trước
            </button>
            <button type="button" class="btn btn-outline-brand btn-sm" id="notificationNext" disabled>
                Trang sau<i class="bi bi-chevron-right ms-1"></i>
            </button>
        </div>
    </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
