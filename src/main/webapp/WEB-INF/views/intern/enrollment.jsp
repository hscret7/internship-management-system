<%--
    intern/enrollment.jsp - Man hinh "Chuong trinh thuc tap cua toi" (US14 - Task 9.2)
    Owner: FE1.

    Contract voi BE3 (docs/BE3_SPRINT2_HANDOFF.md, muc "Thong tin thuc tap - FE1"):
        GET /intern/enrollment   (vai tro INTERN)
        enrollment     : ProgramEnrollment hoac null
                         programName, departmentName, startDate, endDate,
                         status, enrolledAt, currentMentorId, currentMentorName
        currentMentor  : MentorAssignment hoac null (mentorName, assignedAt)
        errorMessage   : co gia tri khi tai that bai
        Response header Cache-Control: no-store do servlet dat

    Ba trang thai phai TACH ROI, khong duoc gop:
        1. Loi tai        -> errorMessage co gia tri
        2. Chua ghi danh  -> khong loi, enrollment null
        3. Dang tham gia  -> enrollment khac null

    Luu y: header.jsp da render alert cho errorMessage (khoi flash chung), nen o
    day chi CHON NHANH hien thi, khong in lai noi dung loi.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<%-- Chot locale cho fmt:* . Khong co dong nay, khi request thieu
     Accept-Language thi JSTL khong xac dinh duoc locale va roi ve
     value.toString(): ngay ra "2026-09-01", so ra "56.10468880787037".
     Trinh duyet luon gui header nay nen nguoi dung that khong thay, nhung
     curl / client API / bot thi thay gia tri tho. Da kiem chung tren Tomcat. --%>
<fmt:setLocale value="vi_VN" />

<c:set var="pageTitle" value="Chương trình thực tập của tôi" />
<c:set var="activePage" value="intern-enrollment" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">

    <div class="page-head mb-4">
        <div>
            <h2 class="page-head__title">Chương trình thực tập của tôi</h2>
            <p class="page-head__sub">
                Chương trình bạn đang tham gia, khoảng thời gian thực tập và mentor hướng dẫn.
            </p>
        </div>
    </div>

    <c:choose>

        <%-- ============================ 1. LỖI TẢI ============================ --%>
        <%-- Khong hien "chua ghi danh": nguoi dung se hieu sai la minh khong co
             chuong trinh, trong khi thuc te chi la loi doc du lieu. --%>
        <c:when test="${not empty errorMessage}">
            <div class="row">
                <div class="col-lg-8">
                    <div class="card">
                        <div class="card-body">
                            <div class="empty-state">
                                <i class="bi bi-cloud-slash"></i>
                                <p class="empty-state__title">Chưa thể tải thông tin thực tập</p>
                                <p class="empty-state__hint">
                                    Hệ thống không đọc được dữ liệu chương trình của bạn.
                                    Vui lòng thử lại sau hoặc liên hệ bộ phận HR.
                                </p>
                                <a href="${ctx}/intern/enrollment" class="btn btn-outline-brand mt-3">
                                    <i class="bi bi-arrow-clockwise me-1"></i>Thử lại
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </c:when>

        <%-- ========================= 3. ĐANG THAM GIA ========================= --%>
        <c:when test="${not empty enrollment}">

            <%-- BE3 da merge vao develop (a61249f): ProgramEnrollment co
                 departmentName / startDate / endDate, nen doc truc tiep duoc.
                 Cau noi c:catch tam thoi da go bo o day. --%>
            <c:set var="programDepartment" value="${enrollment.departmentName}" />
            <c:set var="programStart" value="${enrollment.startDate}" />
            <c:set var="programEnd" value="${enrollment.endDate}" />

            <div class="row g-4">

                <div class="col-lg-7">
                    <div class="card">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-diagram-3 me-1"></i>Chương trình đang tham gia</span>
                            <c:choose>
                                <c:when test="${enrollment.status eq 'ACTIVE'}">
                                    <span class="badge-status badge-status--ok">Đang thực tập</span>
                                </c:when>
                                <c:when test="${enrollment.status eq 'COMPLETED'}">
                                    <span class="badge-status badge-status--neutral">Đã hoàn thành</span>
                                </c:when>
                                <c:when test="${enrollment.status eq 'CANCELLED'}">
                                    <span class="badge-status badge-status--bad">Đã huỷ</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge-status badge-status--neutral">
                                        <c:out value="${enrollment.status}" />
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <div class="card-body">
                            <dl class="row mb-0">
                                <dt class="col-sm-4">Tên chương trình</dt>
                                <dd class="col-sm-8 fw-semibold">
                                    <c:out value="${enrollment.programName}" />
                                </dd>

                                <dt class="col-sm-4">Phòng ban</dt>
                                <dd class="col-sm-8">
                                    <c:choose>
                                        <c:when test="${not empty programDepartment}">
                                            <c:out value="${programDepartment}" />
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">Chưa có thông tin</span>
                                        </c:otherwise>
                                    </c:choose>
                                </dd>

                                <dt class="col-sm-4">Ngày ghi danh</dt>
                                <dd class="col-sm-8">
                                    <c:choose>
                                        <c:when test="${not empty enrollment.enrolledAt}">
                                            <fmt:formatDate value="${enrollment.enrolledAt}"
                                                            pattern="dd/MM/yyyy HH:mm" />
                                        </c:when>
                                        <c:otherwise>Không xác định</c:otherwise>
                                    </c:choose>
                                </dd>
                            </dl>

                            <hr class="my-4">

                            <%-- ---------------------- Lịch tổng quan ---------------------- --%>
                            <c:choose>
                                <c:when test="${not empty programStart and not empty programEnd}">

                                    <jsp:useBean id="now" class="java.util.Date" />

                                    <c:set var="spanMs" value="${programEnd.time - programStart.time}" />
                                    <c:set var="elapsedMs" value="${now.time - programStart.time}" />
                                    <c:set var="remainingMs" value="${programEnd.time - now.time}" />

                                    <c:set var="percent" value="${spanMs gt 0 ? (elapsedMs * 100 / spanMs) : 100}" />
                                    <c:set var="percent" value="${percent lt 0 ? 0 : (percent gt 100 ? 100 : percent)}" />

                                    <c:set var="totalDays" value="${spanMs / 86400000 + 1}" />
                                    <c:set var="remainingDays" value="${remainingMs / 86400000}" />

                                    <p class="fw-semibold mb-3">
                                        <i class="bi bi-calendar3 me-1"></i>Lịch thực tập
                                    </p>

                                    <div class="timeline">
                                        <div class="timeline__track">
                                            <div class="timeline__fill ${remainingMs lt 0 ? 'timeline__fill--done' : ''}"
                                                 style="width: <fmt:formatNumber value="${percent}" maxFractionDigits="0" />%"></div>
                                        </div>

                                        <div class="timeline__marks">
                                            <div class="timeline__mark">
                                                <span class="timeline__label">Bắt đầu</span>
                                                <span class="timeline__value">
                                                    <fmt:formatDate value="${programStart}" pattern="dd/MM/yyyy" />
                                                </span>
                                            </div>
                                            <div class="timeline__mark timeline__mark--end">
                                                <span class="timeline__label">Kết thúc</span>
                                                <span class="timeline__value">
                                                    <fmt:formatDate value="${programEnd}" pattern="dd/MM/yyyy" />
                                                </span>
                                            </div>
                                        </div>

                                        <p class="timeline__note">
                                            <c:choose>
                                                <c:when test="${now.time lt programStart.time}">
                                                    Chương trình chưa bắt đầu — còn
                                                    <fmt:formatNumber value="${(programStart.time - now.time) / 86400000}" maxFractionDigits="0" />
                                                    ngày nữa.
                                                </c:when>
                                                <c:when test="${remainingMs lt 0}">
                                                    Chương trình đã kết thúc theo lịch.
                                                </c:when>
                                                <c:otherwise>
                                                    Đang trong thời gian thực tập — còn
                                                    <fmt:formatNumber value="${remainingDays}" maxFractionDigits="0" />
                                                    ngày nữa tới ngày kết thúc.
                                                </c:otherwise>
                                            </c:choose>
                                            Tổng thời lượng:
                                            <fmt:formatNumber value="${totalDays}" maxFractionDigits="0" /> ngày.
                                        </p>
                                    </div>

                                </c:when>
                                <c:otherwise>
                                    <p class="fw-semibold mb-3">
                                        <i class="bi bi-calendar3 me-1"></i>Lịch thực tập
                                    </p>
                                    <p class="text-muted mb-0">
                                        Chương trình chưa được đặt khoảng thời gian bắt đầu – kết thúc.
                                        Bộ phận HR sẽ cập nhật khi chương trình được kích hoạt.
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <div class="card-footer d-flex flex-wrap gap-2">
                            <a href="${ctx}/contract/view" class="btn btn-outline-brand">
                                <i class="bi bi-file-earmark-text me-1"></i>Hợp đồng thực tập
                            </a>
                        </div>
                    </div>
                </div>

                <div class="col-lg-5">
                    <div class="card">
                        <div class="card-header">
                            <span><i class="bi bi-person-badge me-1"></i>Mentor hướng dẫn</span>
                        </div>

                        <div class="card-body">
                            <c:choose>
                                <c:when test="${not empty currentMentor}">
                                    <dl class="row mb-0">
                                        <dt class="col-sm-5">Mentor hiện tại</dt>
                                        <dd class="col-sm-7 fw-semibold">
                                            <c:out value="${currentMentor.mentorName}" />
                                        </dd>

                                        <dt class="col-sm-5">Ngày phân công</dt>
                                        <dd class="col-sm-7 mb-0">
                                            <c:choose>
                                                <c:when test="${not empty currentMentor.assignedAt}">
                                                    <fmt:formatDate value="${currentMentor.assignedAt}"
                                                                    pattern="dd/MM/yyyy HH:mm" />
                                                </c:when>
                                                <c:otherwise>Không xác định</c:otherwise>
                                            </c:choose>
                                        </dd>
                                    </dl>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-state">
                                        <i class="bi bi-person-dash"></i>
                                        <p class="empty-state__title">Chưa được phân mentor</p>
                                        <p class="empty-state__hint">
                                            Bộ phận HR sẽ phân mentor cho bạn sau khi chương trình
                                            được sắp xếp. Bạn sẽ thấy thông tin tại đây.
                                        </p>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

            </div>

        </c:when>

        <%-- =========================== 2. CHƯA GHI DANH ======================= --%>
        <c:otherwise>
            <div class="row">
                <div class="col-lg-8">
                    <div class="card">
                        <div class="card-body">
                            <div class="empty-state">
                                <i class="bi bi-diagram-3"></i>
                                <p class="empty-state__title">Bạn chưa được ghi danh vào chương trình nào</p>
                                <p class="empty-state__hint">
                                    Sau khi đơn ứng tuyển của bạn được duyệt, bộ phận HR sẽ ghi danh
                                    bạn vào một chương trình thực tập. Thông tin chương trình, thời gian
                                    và mentor sẽ hiển thị tại trang này.
                                </p>
                                <a href="${ctx}/intern/profile" class="btn btn-outline-brand mt-3">
                                    <i class="bi bi-arrow-left me-1"></i>Về hồ sơ cá nhân
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </c:otherwise>

    </c:choose>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
