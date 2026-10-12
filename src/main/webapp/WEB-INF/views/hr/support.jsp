<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<c:set var="pageTitle" value="Quản lý yêu cầu hỗ trợ" />
<c:set var="activePage" value="hr-support" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="h4 mb-1"><i class="bi bi-life-preserver me-2"></i>Quản lý yêu cầu hỗ trợ (US28)</h2>
        <p class="text-muted mb-0">Tiếp nhận, phản hồi và giải quyết yêu cầu hỗ trợ của thực tập sinh.</p>
    </div>
</div>

<c:if test="${not empty error or not empty param.error}">
    <div class="alert alert-danger" role="alert">
        <c:out value="${not empty error ? error : param.error}"/>
    </div>
</c:if>
<c:if test="${param.success eq 'true'}">
    <div class="alert alert-success" role="alert">
        Cập nhật yêu cầu thành công.
    </div>
</c:if>

<div class="card shadow-sm border-0">
    <div class="card-header bg-white py-3">
        <h5 class="card-title mb-0 fs-6 fw-bold">Danh sách yêu cầu hỗ trợ</h5>
    </div>
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>ID</th>
                        <th>Thực tập sinh</th>
                        <th>Loại yêu cầu</th>
                        <th>Tiêu đề & Nội dung</th>
                        <th>Trạng thái</th>
                        <th>Ngày tạo</th>
                        <th>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${empty requests}">
                            <tr><td colspan="7" class="text-center text-muted py-5"><i class="bi bi-inbox fs-2 d-block mb-2"></i>Chưa có yêu cầu hỗ trợ nào.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="req" items="${requests}">
                                <tr>
                                    <td>#<c:out value="${req.requestId}" /></td>
                                    <td>
                                        <strong><c:out value="${req.internName}" /></strong>
                                        <div class="small text-muted">ID:#<c:out value="${req.profileId}" /></div>
                                    </td>
                                    <td>
                                        <span class="badge text-bg-light border"><c:out value="${req.requestType}" /></span>
                                    </td>
                                    <td>
                                        <div class="fw-semibold"><c:out value="${req.title}" /></div>
                                        <div class="small text-muted text-break" style="max-width: 300px;"><c:out value="${req.content}" /></div>
                                        <c:if test="${not empty req.response}">
                                            <div class="small text-success mt-1">
                                                <i class="bi bi-reply-fill"></i> <strong>Phản hồi:</strong> <c:out value="${req.response}" />
                                            </div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${req.status eq 'RESOLVED'}"><span class="badge text-bg-success">Đã giải quyết</span></c:when>
                                            <c:when test="${req.status eq 'IN_PROGRESS'}"><span class="badge text-bg-info">Đang xử lý</span></c:when>
                                            <c:when test="${req.status eq 'CLOSED'}"><span class="badge text-bg-secondary">Đã đóng</span></c:when>
                                            <c:otherwise><span class="badge text-bg-warning">Chờ xử lý</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><small class="text-muted"><c:out value="${req.createdAt}" /></small></td>
                                    <td>
                                        <button type="button" class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#respondModal${req.requestId}">
                                            <i class="bi bi-pencil-square me-1"></i>Phản hồi
                                        </button>

                                        <!-- Response Modal -->
                                        <div class="modal fade" id="respondModal${req.requestId}" tabindex="-1" aria-labelledby="modalLabel${req.requestId}" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content">
                                                    <form action="${pageContext.request.contextPath}/hr/support" method="post">
                                                        <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
                                                        <input type="hidden" name="requestId" value="${req.requestId}">
                                                        <div class="modal-header">
                                                            <h5 class="modal-title fs-6" id="modalLabel${req.requestId}">Phản hồi yêu cầu #<c:out value="${req.requestId}" /></h5>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <div class="modal-body">
                                                            <div class="mb-3">
                                                                <label class="form-label">Thực tập sinh</label>
                                                                <div class="form-control-plaintext fw-bold"><c:out value="${req.internName}" /></div>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label">Tiêu đề</label>
                                                                <div class="form-control-plaintext"><c:out value="${req.title}" /></div>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label">Nội dung</label>
                                                                <div class="p-2 bg-light rounded text-muted small"><c:out value="${req.content}" /></div>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label" for="statusSelect${req.requestId}">Trạng thái *</label>
                                                                <select id="statusSelect${req.requestId}" name="status" class="form-select" required>
                                                                    <option value="PENDING" ${req.status eq 'PENDING' ? 'selected' : ''}>Chờ xử lý (PENDING)</option>
                                                                    <option value="IN_PROGRESS" ${req.status eq 'IN_PROGRESS' ? 'selected' : ''}>Đang xử lý (IN_PROGRESS)</option>
                                                                    <option value="RESOLVED" ${req.status eq 'RESOLVED' ? 'selected' : ''}>Đã giải quyết (RESOLVED)</option>
                                                                    <option value="CLOSED" ${req.status eq 'CLOSED' ? 'selected' : ''}>Đã đóng (CLOSED)</option>
                                                                </select>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label class="form-label" for="responseTxt${req.requestId}">Nội dung phản hồi *</label>
                                                                <textarea id="responseTxt${req.requestId}" name="response" class="form-control" rows="4" required maxlength="2000"><c:out value="${req.response}" /></textarea>
                                                            </div>
                                                        </div>
                                                        <div class="modal-footer">
                                                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Đóng</button>
                                                            <button type="submit" class="btn btn-primary"><i class="bi bi-send me-1"></i>Lưu phản hồi</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
