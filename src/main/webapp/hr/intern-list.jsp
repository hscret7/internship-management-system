<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh sách Thực tập sinh - HR Portal</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
</head>
<body class="bg-light">

<nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm mb-4">
    <div class="container">
        <a class="navbar-brand fw-bold" href="#"><i class="bi bi-buildings me-2"></i>HR Portal - Internship Manager</a>
        <div class="d-flex align-items-center text-white">
            <span class="me-3"><i class="bi bi-person-circle me-1"></i>${not empty sessionScope.currentUser ? sessionScope.currentUser.username : sessionScope.user.username} (${not empty sessionScope.currentUser ? sessionScope.currentUser.role.roleName : sessionScope.user.roleName})</span>
            <a href="${pageContext.request.contextPath}/auth/logout" class="btn btn-outline-danger btn-sm">
                <i class="bi bi-box-arrow-right me-1"></i>Đăng xuất
            </a>
        </div>
    </div>
</nav>

<div class="container pb-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h4 class="text-primary fw-bold mb-0">
            <i class="bi bi-people-fill me-2"></i>Quản lý hồ sơ thực tập sinh (BE2)
        </h4>
    </div>

    <!-- Bộ lọc & Tìm kiếm theo Tên, Trường, Ngành (Yêu cầu BE2 - Ảnh 1) -->
    <div class="card shadow-sm border-0 mb-4">
        <div class="card-body p-4">
            <form action="${pageContext.request.contextPath}/hr/interns" method="get" class="row g-3">
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Tìm kiếm theo tên</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-search"></i></span>
                        <input type="text" name="name" class="form-control" placeholder="Nhập họ và tên..." value="${searchName}">
                    </div>
                </div>
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Tìm kiếm theo trường</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-mortarboard"></i></span>
                        <input type="text" name="university" class="form-control" placeholder="Tên trường ĐH/CĐ..." value="${searchUniversity}">
                    </div>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-semibold">Tìm kiếm theo ngành</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-book"></i></span>
                        <input type="text" name="major" class="form-control" placeholder="Chuyên ngành..." value="${searchMajor}">
                    </div>
                </div>
                <div class="col-md-1 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary w-100 fw-semibold">
                        <i class="bi bi-funnel"></i> Lọc
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- Danh sách Hồ sơ -->
    <div class="card shadow-sm border-0">
        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
            <h5 class="card-title mb-0 fw-bold">Danh sách hồ sơ (${profiles.size()})</h5>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">ID</th>
                            <th>Họ và tên</th>
                            <th>Số điện thoại / Email</th>
                            <th>Trường ĐH / Chuyên ngành</th>
                            <th>GPA</th>
                            <th>Trạng thái</th>
                            <th class="text-end pe-3">Hành động</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty profiles}">
                                <c:forEach var="p" items="${profiles}">
                                    <tr>
                                        <td class="ps-3 fw-bold text-muted">#${p.profileId}</td>
                                        <td>
                                            <div class="fw-bold text-dark">${p.fullName}</div>
                                            <small class="text-muted">${p.gender} | ${p.dob}</small>
                                        </td>
                                        <td>
                                            <div><i class="bi bi-telephone me-1 text-secondary"></i>${p.phone}</div>
                                            <small class="text-muted"><i class="bi bi-envelope me-1 text-secondary"></i>${p.email}</small>
                                        </td>
                                        <td>
                                            <div class="fw-semibold">${p.university}</div>
                                            <small class="text-muted">${p.major}</small>
                                        </td>
                                        <td>
                                            <span class="badge ${p.gpa >= 3.2 ? 'bg-success' : 'bg-secondary'}">
                                                ${p.gpa != null ? p.gpa : 'N/A'}
                                            </span>
                                        </td>
                                        <td>
                                            <span class="badge ${p.status == 'ACTIVE' ? 'bg-success' : 'bg-secondary'}">
                                                ${p.status}
                                            </span>
                                        </td>
                                        <td class="text-end pe-3">
                                            <a href="${pageContext.request.contextPath}/hr/intern-detail?id=${p.profileId}" 
                                               class="btn btn-outline-primary btn-sm">
                                                <i class="bi bi-eye me-1"></i>Chi tiết & Tài liệu
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="7" class="text-center py-4 text-muted">
                                        <i class="bi bi-search fs-3 d-block mb-2"></i>
                                        Không tìm thấy hồ sơ thực tập sinh phù hợp tiêu chí.
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
