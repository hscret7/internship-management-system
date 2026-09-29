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
                <div class="card-header bg-white fw-bold py-3 border-bottom">
                    <i class="bi bi-person-lines-fill me-2 text-primary"></i>Thông tin chi tiết hồ sơ (#${profile.profileId})
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

            <!-- Tài liệu đính kèm của TTS (CV, Đơn xin thực tập) -->
            <div class="card shadow-sm mb-4">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0 text-dark fw-bold h6">
                        <i class="bi bi-file-earmark-check me-2 text-primary"></i>Tài liệu đính kèm (CV, Đơn xin thực tập)
                    </h5>
                </div>
                <div class="card-body p-0">
                    <c:choose>
                        <c:when test="${not empty documents}">
                            <ul class="list-group list-group-flush">
                                <c:forEach var="doc" items="${documents}">
                                    <li class="list-group-item p-3 d-flex justify-content-between align-items-center">
                                        <div>
                                            <span class="badge bg-info text-dark me-2"><c:out value="${doc.documentType}" /></span>
                                            <strong><c:out value="${doc.fileName}" /></strong>
                                            <div class="small text-muted">
                                                Dung lượng: ${doc.fileSize / 1024} KB | Trạng thái: <c:out value="${doc.status}" />
                                            </div>
                                        </div>
                                        <a href="${pageContext.request.contextPath}/document/download?id=${doc.documentId}" 
                                           class="btn btn-primary btn-sm">
                                            <i class="bi bi-download me-1"></i>Tải về
                                        </a>
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

            <!-- Quản lý hợp đồng thực tập (US9) -->
            <div class="card shadow-sm mb-4">
                <div class="card-header bg-white py-3 border-bottom">
                    <h5 class="card-title mb-0 text-success fw-bold h6">
                        <i class="bi bi-file-earmark-lock me-2"></i>Quản lý hợp đồng thực tập (US9)
                    </h5>
                </div>
                <div class="card-body p-4">
                    <p class="small text-muted mb-3">
                        Hợp đồng là tài liệu riêng. Sau khi ứng viên được duyệt đơn, HR tải lên bản hợp đồng để thực tập sinh xem và xác nhận.
                    </p>
                    <form action="${pageContext.request.contextPath}/contract/upload" method="post" enctype="multipart/form-data">
                        <input type="hidden" name="redirectUrl" value="${pageContext.request.contextPath}/hr/intern-detail?id=${profile.profileId}">
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Mã đơn ứng tuyển (Application ID) <span class="text-danger">*</span></label>
                            <input type="number" name="applicationId" class="form-control" placeholder="Nhập ID đơn ứng tuyển đã duyệt (VD: 1)" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Chọn file hợp đồng (PDF, Word, tối đa 5MB) <span class="text-danger">*</span></label>
                            <input type="file" name="contractFile" class="form-control" accept=".pdf,.doc,.docx" required>
                        </div>
                        <button type="submit" class="btn btn-success w-100 fw-bold">
                            <i class="bi bi-cloud-arrow-up-fill me-1"></i>Tải lên Hợp đồng cho ứng viên
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
