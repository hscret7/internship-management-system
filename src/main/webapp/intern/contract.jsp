<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hợp đồng thực tập - Internship Management System</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
</head>
<body class="bg-light">

<nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm mb-4">
    <div class="container">
        <a class="navbar-brand fw-bold" href="#"><i class="bi bi-person-badge me-2"></i>Internship Manager</a>
        <div class="d-flex align-items-center text-white">
            <a href="${pageContext.request.contextPath}/intern/profile" class="btn btn-outline-light btn-sm me-2">
                <i class="bi bi-arrow-left me-1"></i>Quay lại Hồ sơ
            </a>
            <a href="${pageContext.request.contextPath}/auth/logout" class="btn btn-danger btn-sm">
                <i class="bi bi-box-arrow-right me-1"></i>Đăng xuất
            </a>
        </div>
    </div>
</nav>

<div class="container pb-5" style="max-width: 800px;">

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

    <div class="card shadow-sm border-0">
        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
            <h5 class="card-title mb-0 text-primary fw-bold">
                <i class="bi bi-file-earmark-text-fill me-2"></i>Hợp đồng thực tập sinh (US10)
            </h5>
            <c:if test="${not empty contract}">
                <span class="badge ${contract.status == 'CONFIRMED' ? 'bg-success' : 'bg-warning text-dark'} fs-6">
                    ${contract.status == 'CONFIRMED' ? 'ĐÃ XÁC NHẬN' : 'CHỜ XÁC NHẬN'}
                </span>
            </c:if>
        </div>
        <div class="card-body p-4">
            <c:choose>
                <c:when test="${not empty contract}">
                    <div class="alert alert-info d-flex align-items-center mb-4">
                        <i class="bi bi-info-circle-fill fs-3 me-3"></i>
                        <div>
                            <strong>Lưu ý:</strong> Vui lòng tải xuống và đọc kỹ nội dung hợp đồng trước khi bấm "Xác nhận hợp đồng".
                            Thao tác xác nhận được ghi nhận thời điểm trên hệ thống.
                        </div>
                    </div>

                    <table class="table table-bordered mb-4">
                        <tbody>
                            <tr>
                                <th class="w-25 bg-light">Tên hợp đồng:</th>
                                <td><strong>${contract.fileName}</strong></td>
                            </tr>
                            <tr>
                                <th class="bg-light">Dung lượng:</th>
                                <td>${contract.fileSize / 1024} KB</td>
                            </tr>
                            <tr>
                                <th class="bg-light">Ngày tải lên:</th>
                                <td>${contract.uploadedAt}</td>
                            </tr>
                            <tr>
                                <th class="bg-light">Trạng thái:</th>
                                <td>
                                    <span class="badge ${contract.status == 'CONFIRMED' ? 'bg-success' : 'bg-warning text-dark'}">
                                        ${contract.status}
                                    </span>
                                </td>
                            </tr>
                            <c:if test="${not empty contract.confirmedAt}">
                                <tr>
                                    <th class="bg-light">Thời điểm xác nhận:</th>
                                    <td class="text-success fw-bold">
                                        <i class="bi bi-check-all me-1"></i>${contract.confirmedAt}
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>

                    <div class="d-flex gap-3 justify-content-center">
                        <a href="${pageContext.request.contextPath}/contract/download?id=${contract.contractId}" 
                           class="btn btn-outline-primary px-4">
                            <i class="bi bi-cloud-arrow-down me-1"></i>Tải hợp đồng về xem
                        </a>

                        <c:if test="${contract.status == 'PENDING_CONFIRMATION'}">
                            <form action="${pageContext.request.contextPath}/contract/confirm" method="post" 
                                  onsubmit="return confirm('Bạn có chắc chắn muốn xác nhận hợp đồng này?');">
                                <input type="hidden" name="contractId" value="${contract.contractId}">
                                <button type="submit" class="btn btn-success px-4 fw-bold">
                                    <i class="bi bi-check2-circle me-1"></i>Tôi đồng ý & Xác nhận hợp đồng
                                </button>
                            </form>
                        </c:if>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="p-5 text-center text-muted">
                        <i class="bi bi-file-earmark-x fs-1 d-block mb-3 text-secondary"></i>
                        <h5>Chưa có hợp đồng nào được tải lên</h5>
                        <p>Bộ phận HR sẽ tải lên hợp đồng sau khi đơn ứng tuyển của bạn được phê duyệt (APPROVED).</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
