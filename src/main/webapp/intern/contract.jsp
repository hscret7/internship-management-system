<%--
    intern/contract.jsp - TTS xem và xác nhận hợp đồng. Owner: FE1 (US9 / US10)

    GHI ĐÈ bản cũ của BE2. Giữ nguyên 100% hợp đồng dữ liệu để KHÔNG phải sửa
    ContractServlet:
        - request attribute : "contract" (com.internship.model.Contract)
        - field dùng        : contractId, fileName, fileSize, mimeType,
                              uploadedAt, status, confirmedAt
        - route tải về      : ${ctx}/contract/download?id=<contractId>
        - route xác nhận    : POST ${ctx}/contract/confirm, param "contractId"
        - giá trị status    : 'PENDING_CONFIRMATION' | 'CONFIRMED'

    Khác bản cũ ở 4 điểm, đều là sửa lỗi hiển thị:
        1. Dùng chung header/sidebar/topbar/footer thay vì navbar riêng.
        2. Dung lượng: bản cũ in ${contract.fileSize / 1024} ra số thực dài
           (vd 1234.5678 KB). Nay format bằng fmt:formatNumber.
        3. Ngày giờ: bản cũ in nguyên Timestamp (2026-09-22 10:15:30.0).
           Nay format dd/MM/yyyy HH:mm.
        4. Bỏ onsubmit="return confirm(...)" inline, dùng data-confirm để
           toàn bộ hộp thoại xác nhận nằm một chỗ trong app.js.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<c:set var="pageTitle" value="Hợp đồng thực tập" />
<c:set var="activePage" value="contract" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<c:choose>

    <%-- ============================ CÓ HỢP ĐỒNG ============================ --%>
    <c:when test="${not empty contract}">

        <div class="page-head">
            <div>
                <h2 class="page-head__title">Hợp đồng thực tập</h2>
                <p class="page-head__sub">
                    Đọc kỹ nội dung hợp đồng trước khi xác nhận. Thời điểm xác nhận
                    được hệ thống ghi nhận lại.
                </p>
            </div>
            <span class="badge-status ${contract.status eq 'CONFIRMED' ? 'badge-status--ok' : 'badge-status--pending'}">
                <c:choose>
                    <c:when test="${contract.status eq 'CONFIRMED'}">Đã xác nhận</c:when>
                    <c:otherwise>Chờ xác nhận</c:otherwise>
                </c:choose>
            </span>
        </div>

        <div class="row g-4">

            <div class="col-lg-7">
                <div class="card h-100">
                    <div class="card-header">
                        <span><i class="bi bi-file-earmark-text me-1"></i>Thông tin hợp đồng</span>
                    </div>

                    <div class="card-body">
                        <dl class="row mb-0">
                            <dt class="col-sm-4">Tên tệp</dt>
                            <dd class="col-sm-8">
                                <i class="bi bi-file-earmark-pdf text-danger me-1"></i>
                                <c:out value="${contract.fileName}" />
                            </dd>

                            <dt class="col-sm-4">Dung lượng</dt>
                            <dd class="col-sm-8">
                                <fmt:formatNumber value="${contract.fileSize / 1024}"
                                                  maxFractionDigits="0" /> KB
                            </dd>

                            <dt class="col-sm-4">Ngày tải lên</dt>
                            <dd class="col-sm-8">
                                <c:choose>
                                    <c:when test="${not empty contract.uploadedAt}">
                                        <fmt:formatDate value="${contract.uploadedAt}"
                                                        pattern="dd/MM/yyyy HH:mm" />
                                    </c:when>
                                    <c:otherwise>Không xác định</c:otherwise>
                                </c:choose>
                            </dd>

                            <dt class="col-sm-4">Trạng thái</dt>
                            <dd class="col-sm-8">
                                <span class="badge-status ${contract.status eq 'CONFIRMED' ? 'badge-status--ok' : 'badge-status--pending'}">
                                    <c:choose>
                                        <c:when test="${contract.status eq 'CONFIRMED'}">Đã xác nhận</c:when>
                                        <c:otherwise>Chờ xác nhận</c:otherwise>
                                    </c:choose>
                                </span>
                            </dd>

                            <c:if test="${not empty contract.confirmedAt}">
                                <dt class="col-sm-4">Thời điểm xác nhận</dt>
                                <dd class="col-sm-8 mb-0">
                                    <i class="bi bi-check-circle-fill text-success me-1"></i>
                                    <fmt:formatDate value="${contract.confirmedAt}"
                                                    pattern="dd/MM/yyyy HH:mm" />
                                </dd>
                            </c:if>
                        </dl>
                    </div>

                    <div class="card-footer d-flex flex-wrap gap-2">
                        <a href="${ctx}/contract/download?id=${contract.contractId}"
                           class="btn btn-outline-brand">
                            <i class="bi bi-cloud-arrow-down me-1"></i>Tải hợp đồng về xem
                        </a>
                    </div>
                </div>
            </div>

            <div class="col-lg-5">
                <div class="card h-100">
                    <div class="card-header">
                        <span><i class="bi bi-pen me-1"></i>Xác nhận hợp đồng</span>
                    </div>

                    <div class="card-body">
                        <c:choose>

                            <%-- Chờ xác nhận: cho phép bấm --%>
                            <c:when test="${contract.status eq 'PENDING_CONFIRMATION'}">
                                <div class="alert alert-info">
                                    <i class="bi bi-info-circle-fill me-1"></i>
                                    Hãy tải hợp đồng về đọc trước. Khi bấm xác nhận, hệ thống
                                    ghi lại thời điểm bạn đồng ý với nội dung hợp đồng.
                                </div>

                                <form action="${ctx}/contract/confirm" method="post"
                                      data-confirm="Bạn đã đọc kỹ hợp đồng và đồng ý xác nhận? Thao tác này không thể hoàn tác.">
                                    <input type="hidden" name="contractId" value="${contract.contractId}">
                                    <button type="submit" class="btn btn-brand w-100">
                                        <i class="bi bi-check2-circle me-1"></i>Tôi đồng ý và xác nhận hợp đồng
                                    </button>
                                </form>
                            </c:when>

                            <%-- Đã xác nhận: chỉ hiển thị, không cho bấm lại --%>
                            <c:otherwise>
                                <div class="empty-state">
                                    <i class="bi bi-patch-check-fill"></i>
                                    <p class="empty-state__title">Hợp đồng đã được xác nhận</p>
                                    <p class="empty-state__hint">
                                        Bạn đã xác nhận hợp đồng này. Nếu cần thay đổi,
                                        hãy liên hệ bộ phận HR.
                                    </p>
                                </div>
                            </c:otherwise>

                        </c:choose>
                    </div>
                </div>
            </div>

        </div>

    </c:when>

    <%-- =========================== CHƯA CÓ HỢP ĐỒNG ========================= --%>
    <c:otherwise>
        <div class="row">
            <div class="col-lg-8">
                <div class="card">
                    <div class="card-body">
                        <div class="empty-state">
                            <i class="bi bi-file-earmark-x"></i>
                            <p class="empty-state__title">Chưa có hợp đồng nào</p>
                            <p class="empty-state__hint">
                                Bộ phận HR sẽ tải hợp đồng lên sau khi đơn ứng tuyển của bạn
                                được phê duyệt. Bạn sẽ thấy hợp đồng tại trang này.
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

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
