<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Quản lý Thực Tập Sinh" />
<c:set var="activePage" value="interns" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="h4 mb-0">Quản lý Thực Tập Sinh</h2>
    </div>

    <!-- Form Tìm Kiếm & Lọc (Tích hợp BE2 logic + FE2 Layout) -->
    <div class="card shadow-sm mb-4">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/hr/interns" method="GET" class="row g-3 align-items-end">
                <div class="col-md-4">
                    <label class="form-label fw-semibold small text-muted">Họ và tên</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control" name="name" placeholder="Nhập tên thực tập sinh..." value="<c:out value='${searchName}'/>">
                    </div>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-semibold small text-muted">Trường Đại học</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-mortarboard"></i></span>
                        <input type="text" class="form-control" name="university" placeholder="Tên trường ĐH/CĐ..." value="<c:out value='${searchUniversity}'/>">
                    </div>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-semibold small text-muted">Chuyên ngành</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-book"></i></span>
                        <input type="text" class="form-control" name="major" placeholder="Chuyên ngành..." value="<c:out value='${searchMajor}'/>">
                    </div>
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-primary w-100">
                        <i class="bi bi-funnel me-1"></i>Tìm kiếm
                    </button>
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
                                                  <span class="badge bg-success">Đang thực tập</span>
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
