<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chi tiết hồ sơ - HR Portal</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
</head>
<body class="bg-light">

<nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm mb-4">
    <div class="container">
        <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/hr/interns">
            <i class="bi bi-buildings me-2"></i>HR Portal - Internship Manager
        </a>
        <div class="d-flex align-items-center text-white">
            <a href="${pageContext.request.contextPath}/hr/interns" class="btn btn-outline-light btn-sm me-2">
                <i class="bi bi-arrow-left me-1"></i>Quay lại danh sách
            </a>
            <a href="${pageContext.request.contextPath}/auth/logout" class="btn btn-outline-danger btn-sm">
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

    <div class="row g-4">
        <!-- Thông tin chi tiết hồ sơ TTS -->
        <div class="col-lg-6">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0 text-primary fw-bold">
                        <i class="bi bi-person-bounding-box me-2"></i>Hồ sơ #${profile.profileId} - ${profile.fullName}
                    </h5>
                    <form action="${pageContext.request.contextPath}/intern/profile" method="post" class="d-inline">
                        <input type="hidden" name="action" value="update_status">
                        <input type="hidden" name="profileId" value="${profile.profileId}">
                        <input type="hidden" name="status" value="${profile.status == 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}">
                        <button type="submit" class="btn btn-sm ${profile.status == 'ACTIVE' ? 'btn-outline-danger' : 'btn-outline-success'}">
                            Chuyển sang ${profile.status == 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}
                        </button>
                    </form>
                </div>
                <div class="card-body p-4">
                    <table class="table table-borderless">
                        <tbody>
                            <tr>
                                <th class="w-35 text-secondary">Họ và tên:</th>
                                <td class="fw-bold">${profile.fullName}</td>
                            </tr>
                            <tr>
                                <th class="text-secondary">Ngày sinh:</th>
                                <td>${profile.dob}</td>
                            </tr>
                            <tr>
                                <th class="text-secondary">Giới tính:</th>
                                <td>${profile.gender}</td>
                            </tr>
                            <tr>
                                <th class="text-secondary">Số điện thoại:</th>
                                <td>${profile.phone}</td>
                            </tr>
                            <tr>
                                <th class="text-secondary">Email:</th>
                                <td>${profile.email}</td>
                            </tr>
                            <tr>
                                <th class="text-secondary">Trường ĐH/CĐ:</th>
                                <td>${profile.university}</td>
                            </tr>
                            <tr>
                                <th class="text-secondary">Chuyên ngành:</th>
                                <td>${profile.major}</td>
                            </tr>
                            <tr>
                                <th class="text-secondary">GPA:</th>
                                <td><span class="badge bg-primary fs-6">${profile.gpa != null ? profile.gpa : 'N/A'}</span></td>
                            </tr>
                            <tr>
                                <th class="text-secondary">Kỹ năng:</th>
                                <td>${profile.skills}</td>
                            </tr>
                            <tr>
                                <th class="text-secondary">Trạng thái hồ sơ:</th>
                                <td>
                                    <span class="badge ${profile.status == 'ACTIVE' ? 'bg-success' : 'bg-secondary'}">
                                        ${profile.status}
                                    </span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Tài liệu (CV, Đơn) & Upload Hợp đồng (US9) -->
        <div class="col-lg-6">
            <!-- Tài liệu đính kèm của TTS -->
            <div class="card shadow-sm border-0 mb-4">
                <div class="card-header bg-white py-3 border-bottom">
                    <h5 class="card-title mb-0 text-dark fw-bold">
                        <i class="bi bi-file-earmark-check me-2"></i>Tài liệu đính kèm (CV, Đơn xin thực tập)
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
                                            <strong>${doc.fileName}</strong>
                                            <div class="small text-muted">
                                                Dung lượng: ${doc.fileSize / 1024} KB | Trạng thái: ${doc.status}
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

            <!-- US9: HR Upload Hợp đồng thực tập -->
            <div class="card shadow-sm border-0">
                <div class="card-header bg-white py-3 border-bottom">
                    <h5 class="card-title mb-0 text-success fw-bold">
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
                            <label class="form-label fw-semibold">Chọn file hợp đồng (PDF, Word, tối đa 5MB)</label>
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

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
