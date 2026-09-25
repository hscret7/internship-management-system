<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ thực tập sinh - Internship Management System</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
</head>
<body class="bg-light">

<nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm mb-4">
    <div class="container">
        <a class="navbar-brand fw-bold" href="#"><i class="bi bi-person-badge me-2"></i>Internship Manager</a>
        <div class="d-flex align-items-center text-white">
            <span class="me-3"><i class="bi bi-person-circle me-1"></i>${sessionScope.user.username} (${sessionScope.user.roleName})</span>
            <a href="${pageContext.request.contextPath}/contract/view" class="btn btn-outline-light btn-sm me-2">
                <i class="bi bi-file-earmark-text me-1"></i>Hợp đồng
            </a>
            <a href="${pageContext.request.contextPath}/auth/logout" class="btn btn-danger btn-sm">
                <i class="bi bi-box-arrow-right me-1"></i>Đăng xuất
            </a>
        </div>
    </div>
</nav>

<div class="container pb-5">

    <c:if test="${not empty sessionScope.flashSuccess}">
        <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-check-circle-fill me-2"></i>${sessionScope.flashSuccess}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <c:remove var="flashSuccess" scope="session"/>
    </c:if>

    <c:if test="${not empty sessionScope.flashError}">
        <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>${sessionScope.flashError}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <c:remove var="flashError" scope="session"/>
    </c:if>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-exclamation-octagon-fill me-2"></i>${errorMessage}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="row g-4">
        <!-- Cột 1: Thông tin hồ sơ cá nhân -->
        <div class="col-lg-7">
            <div class="card shadow-sm border-0">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0 text-primary fw-bold">
                        <i class="bi bi-person-lines-fill me-2"></i>Thông tin hồ sơ thực tập sinh
                    </h5>
                    <c:if test="${not empty profile}">
                        <span class="badge ${profile.status == 'ACTIVE' ? 'bg-success' : 'bg-secondary'}">
                            ${profile.status}
                        </span>
                    </c:if>
                </div>
                <div class="card-body p-4">
                    <form action="${pageContext.request.contextPath}/intern/profile" method="post">
                        <input type="hidden" name="profileId" value="${profile.profileId}">

                        <div class="mb-3">
                            <label class="form-label fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                            <input type="text" name="fullName" class="form-control" value="${profile.fullName}" required placeholder="Ví dụ: Nguyễn Văn Nam">
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Ngày sinh</label>
                                <input type="date" name="dob" class="form-control" value="${profile.dob}">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Giới tính</label>
                                <select name="gender" class="form-select">
                                    <option value="Nam" ${profile.gender == 'Nam' ? 'selected' : ''}>Nam</option>
                                    <option value="Nữ" ${profile.gender == 'Nữ' ? 'selected' : ''}>Nữ</option>
                                    <option value="Khác" ${profile.gender == 'Khác' ? 'selected' : ''}>Khác</option>
                                </select>
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Số điện thoại <span class="text-danger">*</span></label>
                                <input type="tel" name="phone" class="form-control" value="${profile.phone}" required placeholder="0912345678">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Email liên hệ <span class="text-danger">*</span></label>
                                <input type="email" name="email" class="form-control" value="${profile.email}" required placeholder="nam@student.edu.vn">
                            </div>
                        </div>

                        <div class="row mb-3">
                            <div class="col-md-8">
                                <label class="form-label fw-semibold">Trường Đại học / Cao đẳng <span class="text-danger">*</span></label>
                                <input type="text" name="university" class="form-control" value="${profile.university}" required placeholder="Đại học Bách Khoa Hà Nội">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">GPA (hệ 4)</label>
                                <input type="number" step="0.01" min="0" max="4" name="gpa" class="form-control" value="${profile.gpa}" placeholder="3.50">
                            </div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-semibold">Chuyên ngành <span class="text-danger">*</span></label>
                            <input type="text" name="major" class="form-control" value="${profile.major}" required placeholder="Công nghệ thông tin">
                        </div>

                        <div class="mb-4">
                            <label class="form-label fw-semibold">Kỹ năng / Mô tả bản thân</label>
                            <textarea name="skills" rows="3" class="form-control" placeholder="Java, Servlet, MySQL, Git, Spring Boot...">${profile.skills}</textarea>
                        </div>

                        <div class="text-end">
                            <button type="submit" class="btn btn-primary px-4 fw-semibold shadow-sm">
                                <i class="bi bi-save me-1"></i>${empty profile ? 'Tạo hồ sơ' : 'Cập nhật hồ sơ'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- Cột 2: Upload tài liệu & Danh sách tài liệu -->
        <div class="col-lg-5">
            <!-- Form Upload -->
            <div class="card shadow-sm border-0 mb-4">
                <div class="card-header bg-white py-3 border-bottom">
                    <h5 class="card-title mb-0 text-success fw-bold">
                        <i class="bi bi-cloud-arrow-up-fill me-2"></i>Tải lên CV / Đơn xin thực tập
                    </h5>
                </div>
                <div class="card-body p-4">
                    <form action="${pageContext.request.contextPath}/document/upload" method="post" enctype="multipart/form-data">
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Loại tài liệu</label>
                            <select name="documentType" class="form-select">
                                <option value="CV">CV / Sơ yếu lý lịch</option>
                                <option value="APPLICATION_LETTER">Đơn xin thực tập</option>
                                <option value="OTHER">Tài liệu khác</option>
                            </select>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-semibold">Chọn file (PDF, DOC, DOCX, tối đa 5MB)</label>
                            <input type="file" name="file" class="form-control" accept=".pdf,.doc,.docx" required>
                        </div>

                        <button type="submit" class="btn btn-success w-100 fw-semibold shadow-sm" ${empty profile ? 'disabled' : ''}>
                            <i class="bi bi-upload me-1"></i>Tải lên tài liệu
                        </button>
                        <c:if test="${empty profile}">
                            <small class="text-danger d-block mt-2 text-center">Vui lòng tạo hồ sơ trước khi tải tài liệu.</small>
                        </c:if>
                    </form>
                </div>
            </div>

            <!-- Danh sách tài liệu đã tải -->
            <div class="card shadow-sm border-0">
                <div class="card-header bg-white py-3 border-bottom">
                    <h5 class="card-title mb-0 text-dark fw-bold">
                        <i class="bi bi-folder2-open me-2"></i>Tài liệu đã tải lên
                    </h5>
                </div>
                <div class="card-body p-0">
                    <c:choose>
                        <c:when test="${not empty documents}">
                            <ul class="list-group list-group-flush">
                                <c:forEach var="doc" items="${documents}">
                                    <li class="list-group-item p-3 d-flex justify-content-between align-items-center">
                                        <div>
                                            <span class="badge bg-info text-dark me-2">${doc.documentType}</span>
                                            <strong class="text-truncate d-inline-block" style="max-width: 180px;">${doc.fileName}</strong>
                                            <div class="small text-muted">
                                                Dung lượng: ${doc.fileSize / 1024} KB | 
                                                <span class="badge ${doc.status == 'APPROVED' ? 'bg-success' : (doc.status == 'REJECTED' ? 'bg-danger' : 'bg-warning text-dark')}">
                                                    ${doc.status}
                                                </span>
                                            </div>
                                        </div>
                                        <a href="${pageContext.request.contextPath}/document/download?id=${doc.documentId}" 
                                           class="btn btn-outline-primary btn-sm" title="Tải xuống">
                                            <i class="bi bi-download"></i> Tải
                                        </a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:when>
                        <c:otherwise>
                            <div class="p-4 text-center text-muted">
                                <i class="bi bi-inbox fs-2 d-block mb-2"></i>
                                Chưa có tài liệu nào được tải lên.
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
