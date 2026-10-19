<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>

<fmt:setLocale value="vi_VN" />
<fmt:setTimeZone value="Asia/Ho_Chi_Minh" />

<c:set var="pageTitle" value="Yêu cầu hỗ trợ" />
<c:set var="activePage" value="support" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2 class="h3 mb-1"><i class="bi bi-life-preserver text-primary me-2"></i>Yêu cầu hỗ trợ</h2>
            <p class="text-muted mb-0">Gửi thắc mắc, đề xuất hoặc yêu cầu hỗ trợ đến bộ phận Nhân sự và Kỹ thuật.</p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i><c:out value="${error}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i><c:out value="${param.error}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty param.success}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <i class="bi bi-check-circle-fill me-2"></i>Yêu cầu của bạn đã được gửi thành công. Bộ phận phụ trách sẽ phản hồi sớm nhất!
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <div class="row g-4">
        <div class="col-lg-4">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-header bg-white py-3 border-bottom">
                    <h5 class="card-title mb-0 fs-6 fw-bold">
                        <i class="bi bi-pencil-square text-primary me-2"></i>Tạo yêu cầu mới
                    </h5>
                </div>
                <div class="card-body">
                    <form action="${pageContext.request.contextPath}/intern/support" method="post">
                        <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">

                        <div class="mb-3">
                            <label class="form-label fw-semibold">Loại yêu cầu <span class="text-danger">*</span></label>
                            <select name="requestType" class="form-select" required>
                                <option value="IT_SUPPORT">Hỗ trợ kỹ thuật (IT Support)</option>
                                <option value="HR_INQUIRY">Thắc mắc nhân sự (HR Inquiry)</option>
                                <option value="OTHER">Vấn đề khác (Other)</option>
                            </select>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-semibold">Tiêu đề <span class="text-danger">*</span></label>
                            <input type="text" name="title" class="form-control" placeholder="Tóm tắt yêu cầu..." maxlength="200" required>
                        </div>

                        <div class="mb-3">
                            <label class="form-label fw-semibold">Nội dung chi tiết <span class="text-danger">*</span></label>
                            <textarea name="content" class="form-control" rows="5" placeholder="Mô tả cụ thể vấn đề hoặc câu hỏi của bạn..." maxlength="10000" required></textarea>
                        </div>

                        <button type="submit" class="btn btn-primary w-100 py-2">
                            <i class="bi bi-send me-1"></i>Gửi yêu cầu hỗ trợ
                        </button>
                    </form>
                </div>
            </div>
        </div>

        <div class="col-lg-8">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0 fs-6 fw-bold">
                        <i class="bi bi-clock-history text-primary me-2"></i>Lịch sử yêu cầu của tôi
                    </h5>
                    <span class="badge bg-light text-dark border">
                        <c:out value="${empty requests ? 0 : requests.size()}"/> yêu cầu
                    </span>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th class="ps-3" style="width: 70px;">Mã</th>
                                    <th style="width: 150px;">Phân loại</th>
                                    <th>Tiêu đề & Nội dung</th>
                                    <th style="width: 130px;">Trạng thái</th>
                                    <th style="width: 130px;">Ngày gửi</th>
                                    <th>Phản hồi</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${empty requests}">
                                        <tr>
                                            <td colspan="6" class="text-center py-5 text-muted">
                                                <i class="bi bi-inbox fs-1 d-block mb-2 text-secondary"></i>
                                                Chưa có yêu cầu hỗ trợ nào. Khi bạn gửi yêu cầu, danh sách sẽ hiển thị tại đây.
                                            </td>
                                        </tr>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach var="req" items="${requests}">
                                            <tr>
                                                <td class="ps-3 text-muted fw-bold">#<c:out value="${req.requestId}" /></td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${req.requestType == 'IT_SUPPORT'}">
                                                            <span class="badge bg-info text-dark"><i class="bi bi-laptop me-1"></i>Kỹ thuật</span>
                                                        </c:when>
                                                        <c:when test="${req.requestType == 'HR_INQUIRY'}">
                                                            <span class="badge bg-primary"><i class="bi bi-person-badge me-1"></i>Nhân sự</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-secondary"><i class="bi bi-tag me-1"></i>Khác</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <div class="fw-bold text-dark"><c:out value="${req.title}" /></div>
                                                    <small class="text-muted d-block text-truncate" style="max-width: 250px;">
                                                        <c:out value="${req.content}" />
                                                    </small>
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${req.status == 'RESOLVED'}">
                                                            <span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Đã giải quyết</span>
                                                        </c:when>
                                                        <c:when test="${req.status == 'IN_PROGRESS'}">
                                                            <span class="badge bg-info text-dark"><i class="bi bi-arrow-repeat me-1"></i>Đang xử lý</span>
                                                        </c:when>
                                                        <c:when test="${req.status == 'REJECTED'}">
                                                            <span class="badge bg-danger"><i class="bi bi-x-circle me-1"></i>Từ chối</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-warning text-dark"><i class="bi bi-hourglass-split me-1"></i>Chờ xử lý</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="text-muted small">
                                                    <c:out value="${req.createdAt}" />
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${not empty req.response}">
                                                            <div class="p-2 rounded bg-light border border-success-subtle small">
                                                                <div class="fw-semibold text-success"><i class="bi bi-chat-left-quote me-1"></i><c:out value="${not empty req.handlerName ? req.handlerName : 'HR'}" />:</div>
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
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
