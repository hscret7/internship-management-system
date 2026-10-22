<%--
    intern/meetings.jsp - Man hinh "Lich hop cua toi" cho Thuc tap sinh (US35 / task 35.5)
    Owner: FE1.

    TRANG NAY DA CHAY THAT (cap nhat 10/10/2026):
        - BE1 da merge InternMeetingPageServlet (@WebServlet("/intern/meetings"))
          forward sang chinh file nay, va da merge hop dong JSON ${ctx}/meetings;
        - sidebar.jsp da doi muc "Lịch họp" thanh <a href="${ctx}/intern/meetings">.
    DA NGHIEM THU TREN TRINH DUYET THAT (10/10/2026, Chrome headless qua CDP,
    khong phai chi doc code) - ket qua:
        - Nap trang: 200, tieu de "Lich hop cua toi - Internship Management System",
          0 loi JS, 0 request loi; muc "Lịch họp" sang trang thai is-active.
        - Du lieu: 3 cuoc hop hien dung, khong tran ngang o 1440px.
        - MUI GIO: 2026-10-20T09:00:00Z hien "20/10/2026 16:00" (UTC+7) - dung.
          (startsAt/endsAt la Instant UTC; trang doi sang Asia/Ho_Chi_Minh.)
        - Het phien: vao thang khi chua dang nhap -> 302 ve /auth/login.
        - Bi tu choi: mentor goi /intern/meetings -> 403 dung trang loi.
        - Phan trang/bo loc: nut "Trang truoc/Trang sau" va "Tải lại" co mat;
          CHUA bam thu voi du lieu nhieu trang vi DB chi co 3 ban ghi.
    => Phan con lai: chi con kiem thu phan trang khi co >size ban ghi.

    HOP DONG DU LIEU (theo docs/BE1_MEETING_CONTRACT.md):
        GET  ${ctx}/meetings?programId&status&page&size
             -> {items:[{id,programId,organizerUserId,title,description,location,
                         startsAt,endsAt,status,revision,enrollmentIds}],page,size,csrfToken}
             Intern chi thay cuoc hop cua chinh minh, enrollmentIds luon rong.
        INTERN KHONG DUOC GHI (khong create/update/cancel) -> trang nay chi doc,
        khong co nut thao tac va khong gui csrfToken len.
        startsAt/endsAt la Instant UTC -> hien thi theo Asia/Ho_Chi_Minh.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Lịch họp của tôi" />
<c:set var="activePage" value="intern-meetings" />
<c:set var="pageScript" value="intern-meetings.js" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<%-- data-context de JS biet context path, giong quy uoc cua cac trang FE3
     (data-context="${ctx}"). --%>
<div class="container-fluid mt-4" id="meetingsApp" data-context="${ctx}">

    <div class="page-head mb-4">
        <div>
            <h2 class="page-head__title">Lịch họp của tôi</h2>
            <p class="page-head__sub">
                Các buổi họp giữa bạn, mentor và bộ phận HR trong chương trình thực tập.
            </p>
        </div>
    </div>

    <%-- Vung thong bao dung chung cho ca 3 trang thai loi/rong: JS do nội dung
         bang textContent nen khong co duong XSS tu du lieu API. --%>
    <div id="meetingsNotice" class="alert d-none" role="status" aria-live="polite"></div>

    <div class="card shadow-sm mb-4">
        <div class="card-body d-flex flex-wrap gap-3 align-items-end">
            <div>
                <label class="form-label mb-1" for="meetingStatusFilter">Trạng thái</label>
                <select class="form-select form-select-sm" id="meetingStatusFilter">
                    <option value="">Tất cả</option>
                    <option value="SCHEDULED">Sắp diễn ra</option>
                    <option value="CANCELLED">Đã huỷ</option>
                </select>
            </div>
            <div>
                <label class="form-label mb-1" for="meetingSize">Số dòng mỗi trang</label>
                <select class="form-select form-select-sm" id="meetingSize">
                    <option value="10">10</option>
                    <option value="20" selected>20</option>
                    <option value="50">50</option>
                </select>
            </div>
            <button type="button" class="btn btn-outline-brand btn-sm" id="meetingReload">
                <i class="bi bi-arrow-clockwise me-1"></i>Tải lại
            </button>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
            <h5 class="card-title mb-0 fw-bold">Danh sách buổi họp</h5>
            <span class="text-muted small" id="meetingPageLabel"></span>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">Thời gian</th>
                            <th>Tiêu đề</th>
                            <th>Địa điểm</th>
                            <th>Người tổ chức</th>
                            <th class="pe-3">Trạng thái</th>
                        </tr>
                    </thead>
                    <%-- Dong "Đang tải…" nay la trang thai mac dinh trong HTML: nguoi
                         dung thay ngay ca khi JS loi hoac bi chan. JS se thay the. --%>
                    <tbody id="meetingRows">
                        <tr>
                            <td colspan="5" class="text-center py-4 text-muted">Đang tải…</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="card-footer bg-white d-flex justify-content-between align-items-center">
            <button type="button" class="btn btn-outline-brand btn-sm" id="meetingPrev" disabled>
                <i class="bi bi-chevron-left me-1"></i>Trang trước
            </button>
            <button type="button" class="btn btn-outline-brand btn-sm" id="meetingNext" disabled>
                Trang sau<i class="bi bi-chevron-right ms-1"></i>
            </button>
        </div>
    </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
