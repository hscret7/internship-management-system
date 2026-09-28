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
                                    <c:out value="${doc.documentType}" />
                                    <span class="mx-1">-</span>
                                    ${doc.fileSize / 1024} KB
                                </span>
                            </div>

                            <span class="badge-status
                                ${doc.status eq 'APPROVED' ? 'badge-status--ok'
                                  : (doc.status eq 'REJECTED' ? 'badge-status--bad'
                                  : 'badge-status--pending')}">
                                <c:out value="${doc.status}" />
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
