<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Quản lý Thực Tập Sinh" />
<c:set var="activePage" value="interns" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="h4 mb-0">Quản lý Thực Tập Sinh</h2>
        <a class="btn btn-primary" href="${ctx}/hr/applications">Duyệt đơn ứng tuyển chờ xử lý</a>
    </div>

    <!-- Thêm thư viện FontAwesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <!-- Thanh công cụ Tìm Kiếm & Lọc Thực Tập Sinh (US03) -->
    <div class="card shadow-sm mb-4 border-0">
        <div class="card-body bg-light rounded">
            <!-- Form HTML thực tế gửi request GET -->
            <form id="filterForm" class="row g-3 align-items-end" action="${pageContext.request.contextPath}/hr/interns" method="GET">
                <!-- 1. Input text để tìm kiếm theo Tên hoặc Email -->
                <div class="col-md-4 col-sm-12">
                    <label for="searchInput" class="form-label fw-semibold small text-muted">
                        <i class="fas fa-search me-1"></i>Tìm kiếm cá nhân
                    </label>
                    <input type="text" class="form-control" id="searchInput" name="keyword" value="<c:out value='${keyword}'/>" placeholder="Nhập Tên hoặc Email...">
                </div>
                
                <!-- 2. Thẻ Select lọc theo Chương trình thực tập -->
                <div class="col-md-3 col-sm-6">
                    <label for="programSelect" class="form-label fw-semibold small text-muted">
                        <i class="fas fa-graduation-cap me-1"></i>Chương trình thực tập
                    </label>
                    <select class="form-select" id="programSelect" name="program">
                        <option value="">-- Tất cả chương trình --</option>
                        <c:forEach var="program" items="${programs}">
                            <option value="${program.programId}" ${selectedProgram eq program.programId ? 'selected' : ''}><c:out value="${program.programName}"/></option>
                        </c:forEach>
                    </select>
                </div>
                
                <!-- 3. Thẻ Select lọc theo Trạng thái -->
                <div class="col-md-3 col-sm-6">
                    <label for="statusSelect" class="form-label fw-semibold small text-muted">
                        <i class="fas fa-info-circle me-1"></i>Trạng thái
                    </label>
                    <select class="form-select" id="statusSelect" name="status">
                        <option value="">-- Mọi hồ sơ --</option>
                        <option value="ACTIVE" ${selectedStatus eq 'ACTIVE' ? 'selected' : ''}>Hồ sơ hiệu lực</option>
                        <option value="INACTIVE" ${selectedStatus eq 'INACTIVE' ? 'selected' : ''}>Hồ sơ tạm dừng</option>
                    </select>
                </div>
                
                <!-- 4. Nút Tìm kiếm (Primary) và Xóa bộ lọc (Secondary) -->
                <div class="col-md-2 col-sm-12 d-flex gap-2">
                    <button type="submit" class="btn btn-primary flex-grow-1 shadow-sm" title="Tìm kiếm">
                        <i class="fas fa-filter me-1"></i>Lọc
                    </button>
                    <a href="${pageContext.request.contextPath}/hr/interns" class="btn btn-secondary shadow-sm" title="Xóa bộ lọc">
                        <i class="fas fa-sync-alt"></i>
                    </a>
                </div>
            </form>
        </div>
    </div>

    <!-- Bảng Danh Sách Hồ Sơ TTS -->
    <div class="card shadow-sm">
        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
            <h5 class="card-title mb-0 fw-bold">Danh sách hồ sơ (${not empty profiles ? profiles.size() : 0})</h5>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">Mã TTS</th>
                            <th>Họ Tên</th>
                            <th>Liên hệ</th>
                            <th>Trường Đại Học / Ngành</th>
                            <th>GPA</th>
                            <th>Trạng Thái</th>
                            <th class="text-center pe-3">Thao Tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty profiles}">
                                <c:forEach var="p" items="${profiles}">
                                    <tr>
                                        <td class="ps-3 fw-bold text-muted">#${p.profileId}</td>
                                        <td>
                                            <div class="fw-bold text-dark"><c:out value="${p.fullName}" /></div>
                                            <small class="text-muted"><c:out value="${p.gender}" /> | <c:out value="${p.dob}" /></small>
                                        </td>
                                        <td>
                                            <div><i class="bi bi-telephone me-1 text-secondary"></i><c:out value="${p.phone}" /></div>
                                            <small class="text-muted"><i class="bi bi-envelope me-1 text-secondary"></i><c:out value="${p.email}" /></small>
                                        </td>
                                        <td>
                                            <div class="fw-semibold"><c:out value="${p.university}" /></div>
                                            <small class="text-muted"><c:out value="${p.major}" /></small>
                                        </td>
                                        <td>
                                            <span class="badge ${p.gpa >= 3.2 ? 'bg-success' : 'bg-secondary'}">
                                                ${p.gpa != null ? p.gpa : 'N/A'}
                                            </span>
                                        </td>
                                        <td>
                                            <c:choose>
                                              <c:when test="${p.status == 'ACTIVE'}">
                                                  <span class="badge bg-success">Hồ sơ hiệu lực</span>
                                              </c:when>
                                              <c:otherwise>
                                                  <span class="badge bg-secondary"><c:out value="${p.status}" /></span>
                                              </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-center pe-3">
                                            <a href="${pageContext.request.contextPath}/hr/intern-detail?id=${p.profileId}" 
                                               class="btn btn-sm btn-outline-primary">
                                                <i class="bi bi-eye me-1"></i>Chi tiết
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

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

