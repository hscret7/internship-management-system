<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Chi tiết Thực Tập Sinh" />
<c:set var="activePage" value="interns" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="h4 mb-0">Hồ Sơ Thực Tập Sinh: <c:out value="${profile.fullName}" /></h2>
        <a href="${pageContext.request.contextPath}/hr/interns" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Quay lại danh sách
        </a>
    </div>

    <div class="row">
        <!-- Thông tin tóm tắt bên trái (FE2 Layout + BE2 Action) -->
        <div class="col-lg-4 col-md-5 mb-4">
            <div class="card shadow-sm mb-4">
                <div class="card-body text-center p-4">
                    <div class="bg-primary text-white rounded-circle d-inline-flex align-items-center justify-content-center mb-3 fw-bold" style="width: 80px; height: 80px; font-size: 1.8rem;">
                        <c:choose>
                            <c:when test="${not empty profile.fullName}">
                                <c:out value="${fn:toUpperCase(fn:substring(profile.fullName, 0, 1))}" />
                            </c:when>
                            <c:otherwise>TTS</c:otherwise>
                        </c:choose>
                    </div>
                    <h5 class="card-title fw-bold mb-1"><c:out value="${profile.fullName}" /></h5>
                    <p class="text-muted small mb-2"><c:out value="${profile.email}" /></p>
                    <div class="mb-3">
                        <c:choose>
                            <c:when test="${profile.status == 'ACTIVE'}">
                                <span class="badge bg-success">Đang thực tập</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge bg-secondary"><c:out value="${profile.status}" /></span>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <!-- Nút chuyển trạng thái hồ sơ (BE2 Logic) -->
                    <div class="border-top pt-3">
                        <form action="${pageContext.request.contextPath}/intern/profile" method="post">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="action" value="update_status">
                            <input type="hidden" name="profileId" value="${profile.profileId}">
                            <input type="hidden" name="status" value="${profile.status == 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}">
                            <button type="submit" class="btn btn-sm ${profile.status == 'ACTIVE' ? 'btn-outline-danger' : 'btn-outline-success'} w-100">
                                <i class="bi bi-arrow-repeat me-1"></i>Chuyển sang ${profile.status == 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <!-- Chi tiết hồ sơ bên phải (FE2 Layout + BE2 Data & Forms) -->
        <div class="col-lg-8 col-md-7">
            <!-- Thông tin chi tiết cá nhân & học vấn -->
            <div class="card shadow-sm mb-4">
                <div class="card-header bg-white fw-bold py-3 border-bottom d-flex justify-content-between align-items-center">
                    <div>
                        <i class="bi bi-person-lines-fill me-2 text-primary"></i>Thông tin chi tiết hồ sơ (#${profile.profileId})
                    </div>
                    <button type="button" class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#editProfileModal">
                        <i class="bi bi-pencil-square me-1"></i>Chỉnh sửa thông tin
                    </button>
                </div>
                <div class="card-body">
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Họ và tên:</div>
                        <div class="col-sm-8 fw-semibold"><c:out value="${profile.fullName}" /></div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Ngày sinh:</div>
                        <div class="col-sm-8"><c:out value="${profile.dob}" /></div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Giới tính:</div>
                        <div class="col-sm-8"><c:out value="${profile.gender}" /></div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Số điện thoại:</div>
                        <div class="col-sm-8"><c:out value="${profile.phone}" /></div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Email liên hệ:</div>
                        <div class="col-sm-8"><c:out value="${profile.email}" /></div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Trường Đại học/Cao đẳng:</div>
                        <div class="col-sm-8 fw-semibold"><c:out value="${profile.university}" /></div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Chuyên ngành:</div>
                        <div class="col-sm-8"><c:out value="${profile.major}" /></div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Điểm GPA:</div>
                        <div class="col-sm-8">
                            <span class="badge ${profile.gpa >= 3.2 ? 'bg-success' : 'bg-primary'} fs-6">
                                ${profile.gpa != null ? profile.gpa : 'N/A'}
                            </span>
                        </div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-sm-4 text-muted">Kỹ năng chuyên môn:</div>
                        <div class="col-sm-8"><c:out value="${profile.skills}" /></div>
                    </div>
                    <div class="row">
                        <div class="col-sm-4 text-muted">Trạng thái hồ sơ:</div>
                        <div class="col-sm-8">
                            <span class="badge ${profile.status == 'ACTIVE' ? 'bg-success' : 'bg-secondary'}">
                                <c:out value="${profile.status}" />
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Tài liệu đính kèm của TTS (CV, Đơn xin thực tập) - US05 -->
            <div class="card shadow-sm mb-4">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0 text-dark fw-bold h6">
                        <i class="bi bi-file-earmark-check me-2 text-primary"></i>Tài liệu đính kèm (CV, Đơn xin thực tập)
                    </h5>
                    <c:if test="${not empty documents}">
                        <span class="badge bg-secondary">${documents.size()} tài liệu</span>
                    </c:if>
                </div>
                <div class="card-body p-0">
                    <c:choose>
                        <c:when test="${not empty documents}">
                            <ul class="list-group list-group-flush">
                                <c:forEach var="doc" items="${documents}">
                                    <li class="list-group-item p-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
                                        <div>
                                            <span class="badge bg-info text-dark me-2"><c:out value="${doc.documentType}" /></span>
                                            <strong><c:out value="${doc.fileName}" /></strong>
                                            <div class="small text-muted mt-1">
                                                Dung lượng: ${doc.fileSize / 1024} KB | Trạng thái: 
                                                <c:choose>
                                                    <c:when test="${doc.status eq 'APPROVED'}">
                                                        <span class="badge bg-success">Đã duyệt</span>
                                                        <c:if test="${not empty doc.reviewedAt}">
                                                            <span class="text-secondary small">(${doc.reviewedAt})</span>
                                                        </c:if>
                                                    </c:when>
                                                    <c:when test="${doc.status eq 'REJECTED'}">
                                                        <span class="badge bg-danger">Bị từ chối</span>
                                                        <c:if test="${not empty doc.rejectReason}">
                                                            <div class="text-danger small mt-1">
                                                                <i class="bi bi-exclamation-circle me-1"></i>Lý do từ chối: <c:out value="${doc.rejectReason}" />
                                                            </div>
                                                        </c:if>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-warning text-dark">Chờ duyệt</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                        <div class="d-flex align-items-center gap-2">
                                            <a href="${pageContext.request.contextPath}/document/download?id=${doc.documentId}" 
                                               class="btn btn-outline-primary btn-sm" title="Tải xuống tài liệu">
                                                <i class="bi bi-download me-1"></i>Tải về
                                            </a>
                                            <c:if test="${doc.status eq 'PENDING'}">
                                                <!-- Duyệt tài liệu (US05) -->
                                                <form action="${pageContext.request.contextPath}/document/review" method="post" class="d-inline m-0">
                                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                                    <input type="hidden" name="documentId" value="${doc.documentId}">
                                                    <input type="hidden" name="action" value="approve">
                                                    <input type="hidden" name="redirectUrl" value="${pageContext.request.contextPath}/hr/intern-detail?id=${profile.profileId}">
                                                    <button type="submit" class="btn btn-success btn-sm" title="Duyệt tài liệu">
                                                        <i class="bi bi-check-lg me-1"></i>Duyệt
                                                    </button>
                                                </form>
                                                <!-- Từ chối tài liệu (US05 - bắt buộc lý do) -->
                                                <button type="button" class="btn btn-danger btn-sm" 
                                                        onclick="openRejectDocModal('${doc.documentId}', '${doc.fileName}')" title="Từ chối tài liệu">
                                                    <i class="bi bi-x-lg me-1"></i>Từ chối
                                                </button>
                                            </c:if>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:when>
                        <c:otherwise>
                            <div class="p-4 text-center text-muted">
                                <i class="bi bi-inbox fs-3 d-block mb-2"></i>
                                TTS này chưa tải lên CV hoặc tài liệu nào.
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <!-- Quản lý hợp đồng thực tập (US09) -->
            <div class="card shadow-sm mb-4">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0 text-success fw-bold h6">
                        <i class="bi bi-file-earmark-lock me-2"></i>Quản lý hợp đồng thực tập (US09)
                    </h5>
                    <c:if test="${not empty contract}">
                        <span class="badge ${contract.status eq 'CONFIRMED' ? 'bg-success' : 'bg-warning text-dark'}">
                            ${contract.status eq 'CONFIRMED' ? 'Đã xác nhận' : 'Chờ xác nhận'}
                        </span>
                    </c:if>
                </div>
                <div class="card-body p-4">
                    <c:choose>
                        <c:when test="${not empty contract}">
                            <div class="border rounded p-3 mb-3 bg-light">
                                <div class="row align-items-center">
                                    <div class="col-md-7">
                                        <div class="fw-bold text-dark">
                                            <i class="bi bi-file-earmark-pdf text-danger me-2"></i><c:out value="${contract.fileName}" />
                                        </div>
                                        <small class="text-muted d-block mt-1">
                                            Dung lượng: ${contract.fileSize / 1024} KB | Ngày tải: ${contract.uploadedAt}
                                        </small>
                                        <c:if test="${not empty contract.confirmedAt}">
                                            <small class="text-success fw-semibold d-block mt-1">
                                                <i class="bi bi-check-circle-fill me-1"></i>TTS đã xác nhận lúc: ${contract.confirmedAt}
                                            </small>
                                        </c:if>
                                    </div>
                                    <div class="col-md-5 text-md-end mt-2 mt-md-0">
                                        <a href="${pageContext.request.contextPath}/contract/download?id=${contract.contractId}" 
                                           class="btn btn-outline-success btn-sm">
                                            <i class="bi bi-download me-1"></i>Tải hợp đồng về xem
                                        </a>
                                    </div>
                                </div>
                            </div>

                            <c:choose>
                                <c:when test="${contract.status eq 'CONFIRMED'}">
                                    <div class="alert alert-success d-flex align-items-center mb-0">
                                        <i class="bi bi-check-circle-fill me-2 fs-5"></i>
                                        <div>
                                            <strong>Hợp đồng đã được thực tập sinh xác nhận (CONFIRMED).</strong>
                                            <div class="small">Theo quy ước Sprint 2, bản hợp đồng đã xác nhận không thể thay thế hoặc ghi đè.</div>
                                        </div>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="alert alert-info py-2 small mb-3">
                                        <i class="bi bi-info-circle me-1"></i>
                                        Hợp đồng đang chờ TTS xác nhận. HR có thể thay thế bằng file mới trước khi TTS bấm xác nhận.
                                    </div>
                                    <form action="${pageContext.request.contextPath}/contract/upload" method="post" enctype="multipart/form-data">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="redirectUrl" value="${pageContext.request.contextPath}/hr/intern-detail?id=${profile.profileId}">
                                        <input type="hidden" name="applicationId" value="${contract.applicationId}">
                                        <div class="mb-3">
                                            <label class="form-label fw-semibold small">Thay thế file hợp đồng mới (PDF, Word, tối đa 5MB) <span class="text-danger">*</span></label>
                                            <input type="file" name="contractFile" class="form-control form-control-sm" accept=".pdf,.doc,.docx" required>
                                        </div>
                                        <button type="submit" class="btn btn-outline-warning btn-sm w-100 fw-semibold">
                                            <i class="bi bi-arrow-repeat me-1"></i>Cập nhật thay thế file hợp đồng
                                        </button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <p class="small text-muted mb-3">
                                Hợp đồng là tài liệu riêng. Sau khi ứng viên được duyệt đơn (APPROVED), HR tải lên bản hợp đồng để thực tập sinh xem và xác nhận.
                            </p>
                            <form action="${pageContext.request.contextPath}/contract/upload" method="post" enctype="multipart/form-data">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="redirectUrl" value="${pageContext.request.contextPath}/hr/intern-detail?id=${profile.profileId}">
                                <div class="mb-3">
                                    <label class="form-label fw-semibold small">Mã đơn ứng tuyển (Application ID) <span class="text-danger">*</span></label>
                                    <input type="number" name="applicationId" class="form-control form-control-sm" placeholder="Nhập ID đơn ứng tuyển đã duyệt (VD: 1)" required>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold small">Chọn file hợp đồng (PDF, Word, tối đa 5MB) <span class="text-danger">*</span></label>
                                    <input type="file" name="contractFile" class="form-control form-control-sm" accept=".pdf,.doc,.docx" required>
                                </div>
                                <button type="submit" class="btn btn-success btn-sm w-100 fw-bold">
                                    <i class="bi bi-cloud-arrow-up-fill me-1"></i>Tải lên Hợp đồng cho ứng viên
                                </button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Modal Chỉnh sửa hồ sơ TTS (US02 - Whitelist bảo mật) -->
<div class="modal fade" id="editProfileModal" tabindex="-1" aria-labelledby="editProfileModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form action="${pageContext.request.contextPath}/hr/interns/edit" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="hr_edit">
                <input type="hidden" name="profileId" value="${profile.profileId}">
                <input type="hidden" name="redirectUrl" value="${pageContext.request.contextPath}/hr/intern-detail?id=${profile.profileId}">
                
                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold" id="editProfileModalLabel">
                        <i class="bi bi-pencil-square me-2 text-primary"></i>Chỉnh sửa hồ sơ TTS (#${profile.profileId})
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                
                <div class="modal-body p-4">
                    <div class="alert alert-info py-2 small mb-3">
                        <i class="bi bi-shield-lock me-1"></i>
                        Quy ước Sprint 2: Chỉ cập nhật các thông tin cá nhân và học vấn trong whitelist. Không can thiệp tài khoản hay vai trò hệ thống.
                    </div>
                    
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label fw-semibold small">Họ và tên <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="fullName" value="<c:out value='${profile.fullName}'/>" required minlength="2" maxlength="100">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold small">Ngày sinh</label>
                            <input type="date" class="form-control" name="dob" value="${profile.dob}">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold small">Giới tính</label>
                            <select class="form-select" name="gender">
                                <option value="Nam" ${profile.gender eq 'Nam' ? 'selected' : ''}>Nam</option>
                                <option value="Nữ" ${profile.gender eq 'Nữ' ? 'selected' : ''}>Nữ</option>
                                <option value="Khác" ${profile.gender eq 'Khác' ? 'selected' : ''}>Khác</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold small">Số điện thoại <span class="text-danger">*</span></label>
                            <input type="tel" class="form-control" name="phone" value="<c:out value='${profile.phone}'/>" required pattern="^(0|\\+84)[0-9]{9,10}$" placeholder="0912345678">
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold small">Email liên hệ <span class="text-danger">*</span></label>
                            <input type="email" class="form-control" name="email" value="<c:out value='${profile.email}'/>" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold small">Trường Đại học/Cao đẳng <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="university" value="<c:out value='${profile.university}'/>" required maxlength="150">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold small">Chuyên ngành <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="major" value="<c:out value='${profile.major}'/>" required maxlength="100">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold small">Điểm GPA (hệ 4)</label>
                            <input type="number" step="0.01" min="0.0" max="4.0" class="form-control" name="gpa" value="${profile.gpa}" placeholder="3.50">
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold small">Kỹ năng chuyên môn</label>
                            <textarea class="form-control" name="skills" rows="3" placeholder="Java, Spring Boot, MySQL..."><c:out value="${profile.skills}"/></textarea>
                        </div>
                    </div>
                </div>
                
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary px-4 fw-semibold">
                        <i class="bi bi-save me-1"></i>Lưu thay đổi
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal Từ chối tài liệu (US05 - Bắt buộc lý do) -->
<div class="modal fade" id="rejectDocModal" tabindex="-1" aria-labelledby="rejectDocModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form action="${pageContext.request.contextPath}/document/review" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="reject">
                <input type="hidden" name="documentId" id="rejectModalDocId" value="">
                <input type="hidden" name="redirectUrl" value="${pageContext.request.contextPath}/hr/intern-detail?id=${profile.profileId}">
                
                <div class="modal-header bg-danger text-white">
                    <h5 class="modal-title fw-bold" id="rejectDocModalLabel">
                        <i class="bi bi-x-circle me-2"></i>Từ chối tài liệu
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <p class="mb-2">Bạn đang từ chối tài liệu: <strong id="rejectModalDocName"></strong></p>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small">Lý do từ chối <span class="text-danger">*</span></label>
                        <textarea class="form-control" name="rejectReason" rows="3" placeholder="Nhập lý do từ chối để thực tập sinh nộp lại..." required></textarea>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                    <button type="submit" class="btn btn-danger px-4 fw-semibold">
                        <i class="bi bi-x-lg me-1"></i>Xác nhận từ chối
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    function openRejectDocModal(docId, docName) {
        document.getElementById('rejectModalDocId').value = docId;
        document.getElementById('rejectModalDocName').textContent = docName;
        var modal = new bootstrap.Modal(document.getElementById('rejectDocModal'));
        modal.show();
    }
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

