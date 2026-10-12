<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<c:set var="pageTitle" value="Yêu cầu hỗ trợ của tôi" />
<c:set var="activePage" value="support" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="h4 mb-1"><i class="bi bi-life-preserver me-2"></i>Yêu cầu hỗ trợ (US27)</h2>
        <p class="text-muted mb-0">Gửi yêu cầu hỗ trợ đến bộ phận quản lý/HR và theo dõi tiến độ phản hồi.</p>
    </div>
</div>

<c:if test="${not empty error or not empty param.error}">
    <div class="alert alert-danger" role="alert">
        <c:out value="${not empty error ? error : param.error}"/>
    </div>
</c:if>
<c:if test="${param.success eq 'true'}">
    <div class="alert alert-success" role="alert">
        Tạo yêu cầu hỗ trợ thành công.
    </div>
</c:if>

<div class="card mb-4 shadow-sm border-0">
    <div class="card-header bg-white py-3">
        <h5 class="card-title mb-0 fs-6 fw-bold">Tạo yêu cầu hỗ trợ mới</h5>
    </div>
    <div class="card-body">
        <form action="${pageContext.request.contextPath}/intern/support" method="post">
            <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
            <div class="mb-3">
                <label class="form-label" for="reqType">Loại yêu cầu *</label>
                <select id="reqType" name="requestType" class="form-select" required>
                    <option value="IT_SUPPORT">Hỗ trợ kỹ thuật / IT (IT_SUPPORT)</option>
                    <option value="HR_INQUIRY">Thắc mắc nhân sự (HR_INQUIRY)</option>
                    <option value="OTHER">Khác (OTHER)</option>
                </select>
            </div>
            <div class="mb-3">
                <label class="form-label" for="reqTitle">Tiêu đề *</label>
                <input type="text" id="reqTitle" name="title" class="form-control" required maxlength="255" placeholder="Tóm tắt vấn đề cần hỗ trợ">
            </div>
            <div class="mb-3">
                <label class="form-label" for="reqContent">Nội dung chi tiết *</label>
                <textarea id="reqContent" name="content" class="form-control" rows="4" required maxlength="2000" placeholder="Mô tả cụ thể khó khăn hoặc thông tin cần giải đáp"></textarea>
            </div>
            <button type="submit" class="btn btn-primary">
                <i class="bi bi-send me-1"></i>Gửi yêu cầu
            </button>
        </form>
    </div>
</div>

<div class="card shadow-sm border-0">
    <div class="card-header bg-white py-3">
        <h5 class="card-title mb-0 fs-6 fw-bold">Lịch sử yêu cầu hỗ trợ của tôi</h5>
    </div>
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>ID</th>
                        <th>Loại yêu cầu</th>
                        <th>Tiêu đề & Nội dung</th>
                        <th>Trạng thái</th>
                        <th>Ngày gửi</th>
                        <th>Phản hồi từ HR</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${empty requests}">
                            <tr><td colspan="6" class="text-center text-muted py-5"><i class="bi bi-inbox fs-2 d-block mb-2"></i>Bạn chưa gửi yêu cầu hỗ trợ nào.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="req" items="${requests}">
                                <tr>
                                    <td>#<c:out value="${req.requestId}" /></td>
                                    <td><span class="badge text-bg-light border"><c:out value="${req.requestType}" /></span></td>
                                    <td>
                                        <div class="fw-semibold"><c:out value="${req.title}" /></div>
                                        <div class="small text-muted text-break" style="max-width: 300px;"><c:out value="${req.content}" /></div>
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
                                        <c:choose>
                                            <c:when test="${not empty req.response}">
                                                <div class="p-2 bg-light rounded text-success small">
                                                    <c:out value="${req.response}" />
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted fst-italic small">Chưa có phản hồi</span>
                                            </c:otherwise>
                                        </c:choose>
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
