<%--
    intern/documents.jsp - Partial upload CV/đơn + danh sách tài liệu. Owner: FE1
    Được include từ intern/profile.jsp (US04 / task 5.2).

    Request attribute do BE2 forward sang:
        profile   : InternProfile hoặc null
        documents : List<Document>

    Tên field gửi lên KHÔNG ĐƯỢC ĐỔI - DocumentUploadServlet đang đọc:
        documentType (CV | APPLICATION_LETTER | OTHER), file
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<%-- Chốt locale cho fmt:* theo quy ước của intern/contract.jsp. Thiếu dòng này,
     request không có Accept-Language (curl, bot, một số proxy) làm JSTL rơi về
     value.toString() và dung lượng ra "1024.0 KB" thay vì "1024 KB". --%>
<fmt:setLocale value="vi_VN" />

<div class="card mb-4">
    <div class="card-header">
        <span>Tải lên CV / đơn xin thực tập</span>
    </div>

    <c:choose>

        <c:when test="${empty profile}">
            <div class="card-body">
                <div class="empty-state">
                    <i class="bi bi-person-plus"></i>
                    <p class="empty-state__title">Chưa có hồ sơ</p>
                    <p class="empty-state__hint">
                        Vui lòng tạo hồ sơ cá nhân trước khi tải tài liệu lên.
                    </p>
                </div>
            </div>
        </c:when>

        <c:otherwise>
            <form action="${ctx}/document/upload" method="post"
                  enctype="multipart/form-data" data-validate-upload>
    <input type="hidden" name="csrfToken" value="${csrfToken}" />

                <div class="card-body">

                    <div class="mb-3">
                        <label class="form-label" for="documentType">Loại tài liệu</label>
                        <select id="documentType" name="documentType" class="form-select">
                            <option value="CV">CV / sơ yếu lý lịch</option>
                            <option value="APPLICATION_LETTER">Đơn xin thực tập</option>
                            <option value="OTHER">Tài liệu khác</option>
                        </select>
                    </div>

                    <div data-file-field>
                        <label class="form-label" for="file">
                            Chọn file <span class="req">*</span>
                        </label>

                        <div class="dropzone">
                            <i class="bi bi-cloud-arrow-up"></i>
                            <p class="mb-1 fw-semibold">Kéo thả file vào đây hoặc bấm để chọn</p>
                            <p class="form-text mb-2">Nhận PDF, DOC, DOCX. Tối đa 5 MB.</p>
                            <input type="file" id="file" name="file" class="form-control"
                                   accept=".pdf,.doc,.docx" required data-validate-file>
                        </div>

                        <span class="form-text d-block mt-2" data-file-name></span>
                    </div>

                </div>

                <div class="card-footer text-end">
                    <button type="submit" class="btn btn-brand">
                        <i class="bi bi-upload me-1"></i>Tải lên
                    </button>
                </div>
            </form>
        </c:otherwise>

    </c:choose>
</div>

<div class="card">
    <div class="card-header">
        <span>Tài liệu đã tải lên</span>
        <c:if test="${not empty documents}">
            <span class="badge-status badge-status--neutral">${documents.size()}</span>
        </c:if>
    </div>

    <div class="card-body ${empty documents ? 'p-0' : 'py-2'}">
        <c:choose>

            <c:when test="${not empty documents}">
                <ul class="file-list">
                    <c:forEach var="doc" items="${documents}">
                        <li class="file-list__item">
                            <i class="bi bi-file-earmark-text file-list__icon"></i>

                            <div class="file-list__body">
                                <span class="file-list__name">
                                    <c:out value="${doc.fileName}" />
                                </span>
                                <span class="file-list__meta">
                                    <%-- Nhan tieng Viet cho documentType (CV | APPLICATION_LETTER |
                                         OTHER). Truoc day in thang ma enum nen TTS doc ra
                                         "APPLICATION_LETTER"; dung dung bo tu voi o chon loai
                                         tai lieu ngay ben tren. --%>
                                    <c:choose>
                                        <c:when test="${doc.documentType eq 'CV'}">CV / sơ yếu lý lịch</c:when>
                                        <c:when test="${doc.documentType eq 'APPLICATION_LETTER'}">Đơn xin thực tập</c:when>
                                        <c:when test="${doc.documentType eq 'OTHER'}">Tài liệu khác</c:when>
                                        <c:otherwise><c:out value="${doc.documentType}" /></c:otherwise>
                                    </c:choose>
                                    <span class="mx-1">-</span>
                                    <%-- file_size la so byte nguyen; EL phep chia luon tra ve
                                         Double nen ban cu in ra "1024.0 KB" (va "1234.5678 KB" voi
                                         file le byte). Da sua o contract.jsp, cho nay con sot. --%>
                                    <fmt:formatNumber value="${doc.fileSize / 1024}" maxFractionDigits="0" /> KB
                                </span>
                                <c:if test="${doc.status eq 'REJECTED' and not empty doc.rejectReason}">
                                    <div class="small text-danger mt-1">
                                        <i class="bi bi-exclamation-circle me-1"></i>Lý do từ chối: <c:out value="${doc.rejectReason}" />
                                    </div>
                                </c:if>
                            </div>

                            <span class="badge-status
                                ${doc.status eq 'APPROVED' ? 'badge-status--ok'
                                  : (doc.status eq 'REJECTED' ? 'badge-status--bad'
                                  : 'badge-status--pending')}">
                                <c:choose>
                                    <c:when test="${doc.status eq 'PENDING'}">Chờ duyệt</c:when>
                                    <c:when test="${doc.status eq 'APPROVED'}">Đã duyệt</c:when>
                                    <c:when test="${doc.status eq 'REJECTED'}">Đã từ chối</c:when>
                                    <c:otherwise><c:out value="${doc.status}" /></c:otherwise>
                                </c:choose>
                            </span>

                            <a href="${ctx}/document/download?id=${doc.documentId}"
                               class="btn btn-outline-brand btn-sm" title="Tải xuống">
                                <i class="bi bi-download"></i>
                                <span class="d-none d-xl-inline ms-1">Tải</span>
                            </a>
                        </li>
                    </c:forEach>
                </ul>
            </c:when>

            <c:otherwise>
                <div class="empty-state">
                    <i class="bi bi-inbox"></i>
                    <p class="empty-state__title">Chưa có tài liệu nào</p>
                    <p class="empty-state__hint">
                        Tải lên CV hoặc đơn xin thực tập để hoàn thiện hồ sơ.
                    </p>
                </div>
            </c:otherwise>

        </c:choose>
    </div>
</div>
