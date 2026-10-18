<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- Noi dung duoc chuyen nguyen khoi tu src/main/webapp/hr/support.jsp - mot ban
     markup da viet xong nhung khong servlet nao forward toi (SupportRequestServlet
     chi tra ve /WEB-INF/views/hr/support.jsp), nen khong bao gio chay. Ban o day la
     ban duy nhat con lai.

     Chi khac ban goc 3 cho:
       - Bo khoi <section class="card panel"> bi lap y het nhau 2 lan (dong 8 va 9
         cua ban goc): ca hai deu mo <section> + <div class="card-body p-4"> nhung
         chi mot lan duoc dong o cuoi, nen ban goc de lai 2 the chua dong va 2 bo loc
         trung id="searchSupport"/id="statusSupport" (getElementById chi thay cai dau,
         bo loc thu hai la UI chet).
       - activePage: 'support' -> 'hr-support' cho khop dieu kien highlight cua
         sidebar.jsp.
       - req.createdAt boc 'T' bang fn:replace, theo loi admin/accounts.jsp:109. --%>

<c:set var="pageTitle" value="Yêu cầu hỗ trợ" />
<c:set var="activePage" value="hr-support" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<style>
    .hr-support .panel{border:0;border-radius:1rem;box-shadow:0 .35rem 1.2rem #172b4d12}
    .hr-support .request-content{white-space:pre-wrap;overflow-wrap:anywhere;max-width:25rem}
    .hr-support .table td{vertical-align:middle}
    @media(max-width:575.98px){.hr-support .table{min-width:980px}}

    .support-response-dialog{
        width:min(36rem,calc(100% - 2rem));
        max-height:calc(100vh - 2rem);
        overflow:auto;
        padding:0;
        border:0;
        border-radius:1rem;
        box-shadow:0 1rem 3rem #0004;
    }
    .support-response-dialog::backdrop{background:#0007}

    .hr-support .support-status{
        color:#334155;
        background:#e2e8f0;
        border:1px solid #94a3b8;
        padding:.45rem .65rem;
        font-size:.8rem;
        font-weight:600;
        white-space:nowrap;
    }
    .hr-support .support-status[data-status="PENDING"]{
        color:#713f12;background:#fef3c7;border-color:#d97706;
    }
    .hr-support .support-status[data-status="IN_PROGRESS"]{
        color:#1e3a8a;background:#dbeafe;border-color:#2563eb;
    }
    .hr-support .support-status[data-status="RESOLVED"]{
        color:#14532d;background:#dcfce7;border-color:#16a34a;
    }
    .hr-support .support-status[data-status="REJECTED"]{
        color:#7f1d1d;background:#fee2e2;border-color:#dc2626;
    }
</style>

<div class="container-fluid py-4 hr-support">

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
            <h2 class="h4 mb-1">Yêu cầu hỗ trợ</h2>
            <p class="text-muted mb-0">Tiếp nhận, phản hồi và cập nhật trạng thái yêu cầu của TTS.</p>
        </div>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/hr/support">Làm mới</a>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${param.success eq 'true'}">
        <div class="alert alert-success">Đã cập nhật yêu cầu.</div>
    </c:if>

    <section class="card panel">
        <div class="card-body p-4">

            <div class="row g-2 mb-3" id="supportFilters">
                <div class="col-sm-6 col-lg-3">
                    <label class="form-label" for="searchSupport">Tìm kiếm</label>
                    <input class="form-control" id="searchSupport" placeholder="TTS, tiêu đề, nội dung">
                </div>
                <div class="col-sm-6 col-lg-3">
                    <label class="form-label" for="statusSupport">Trạng thái</label>
                    <select class="form-select" id="statusSupport">
                        <option value="">Tất cả</option>
                        <option value="PENDING">Chờ xử lý</option>
                        <option value="IN_PROGRESS">Đang xử lý</option>
                        <option value="RESOLVED">Đã giải quyết</option>
                        <option value="REJECTED">Từ chối</option>
                    </select>
                </div>
            </div>

            <div id="responseModals">
                <c:forEach var="req" items="${requests}">
                    <dialog class="support-response-dialog" id="responseModal${req.requestId}"
                            aria-labelledby="responseTitle${req.requestId}">
                        <div class="modal-dialog modal-dialog-centered">
                            <div class="modal-content">
                                <form method="post" action="${pageContext.request.contextPath}/hr/support">
                                    <div class="modal-header">
                                        <h2 class="modal-title fs-5" id="responseTitle${req.requestId}">Xử lý yêu cầu #<c:out value="${req.requestId}"/></h2>
                                        <button type="button" class="btn-close" data-support-close aria-label="Đóng"></button>
                                    </div>
                                    <div class="modal-body">
                                        <input type="hidden" name="csrfToken" value="<%= com.internship.util.CsrfToken.expose(request) %>">
                                        <input type="hidden" name="requestId" value="${req.requestId}">
                                        <label class="form-label" for="status${req.requestId}">Trạng thái *</label>
                                        <select class="form-select mb-3" id="status${req.requestId}" name="status" required>
                                            <option value="IN_PROGRESS">Tiếp nhận / đang xử lý</option>
                                            <option value="RESOLVED">Giải quyết</option>
                                            <option value="REJECTED">Từ chối</option>
                                        </select>
                                        <label class="form-label" for="response${req.requestId}">Phản hồi *</label>
                                        <textarea class="form-control" id="response${req.requestId}" name="response"
                                                  rows="4" maxlength="2000" required><c:out value="${req.response}"/></textarea>
                                    </div>
                                    <div class="modal-footer">
                                        <button class="btn btn-outline-secondary" type="button" data-support-close>Hủy</button>
                                        <button class="btn btn-primary" type="submit">Lưu phản hồi</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </dialog>
                </c:forEach>
            </div>

            <div class="table-responsive">
                <table class="table table-hover align-middle" id="supportTable">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>TTS</th>
                            <th>Loại</th>
                            <th>Tiêu đề / nội dung</th>
                            <th>Trạng thái</th>
                            <th>Ngày tạo</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="req" items="${requests}">
                            <tr data-status="<c:out value='${req.status}'/>"
                                data-search="<c:out value='${req.internName} ${req.title} ${req.content} ${req.requestType}'/>">
                                <td>#<c:out value="${req.requestId}"/></td>
                                <td><strong><c:out value="${req.internName}"/></strong></td>
                                <td><c:out value="${req.requestType}"/></td>
                                <td>
                                    <strong><c:out value="${req.title}"/></strong>
                                    <div class="request-content small text-muted"><c:out value="${req.content}"/></div>
                                    <c:if test="${not empty req.response}">
                                        <div class="small mt-1"><b>Phản hồi:</b> <c:out value="${req.response}"/></div>
                                    </c:if>
                                </td>
                                <td><span class="badge support-status" data-status="<c:out value='${req.status}'/>">
                                    <c:choose>
                                        <c:when test="${req.status eq 'PENDING'}">Chờ xử lý</c:when>
                                        <c:when test="${req.status eq 'IN_PROGRESS'}">Đang xử lý</c:when>
                                        <c:when test="${req.status eq 'RESOLVED'}">Đã giải quyết</c:when>
                                        <c:when test="${req.status eq 'REJECTED'}">Đã từ chối</c:when>
                                        <c:otherwise><c:out value="${req.status}"/></c:otherwise>
                                    </c:choose>
                                </span></td>
                                <td><c:out value="${fn:replace(req.createdAt, 'T', ' ')}"/></td>
                                <td>
                                    <button class="btn btn-sm btn-primary" type="button"
                                            data-support-open="responseModal${req.requestId}">
                                        Phản hồi
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty requests}">
                            <tr>
                                <td colspan="7" class="text-center text-muted py-5">Chưa có yêu cầu hỗ trợ.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>

            <div id="supportEmpty" class="d-none text-center text-muted py-4">
                Không có yêu cầu phù hợp với bộ lọc.
            </div>

        </div>
    </section>
</div>

<script>
    (() => {
        const q = document.getElementById('searchSupport'),
            s = document.getElementById('statusSupport'),
            rows = [...document.querySelectorAll('#supportTable tbody tr[data-status]')],
            empty = document.getElementById('supportEmpty');

        function filter() {
            const text = (q.value || '').toLowerCase().trim(), st = s.value;
            let n = 0;
            rows.forEach(r => {
                const ok = (!st || r.dataset.status === st) && (!text || r.dataset.search.toLowerCase().includes(text));
                r.classList.toggle('d-none', !ok);
                if (ok) n++;
            });
            empty.classList.toggle('d-none', n !== 0 || !rows.length);
        }

        document.querySelectorAll('[data-support-open]').forEach(button => {
            button.addEventListener('click', () => {
                const dialog = document.getElementById(button.dataset.supportOpen);
                if (dialog && !dialog.open) dialog.showModal();
            });
        });

        document.querySelectorAll('[data-support-close]').forEach(button => {
            button.addEventListener('click', () => {
                const dialog = button.closest('dialog');
                if (dialog) dialog.close();
            });
        });

        q.addEventListener('input', filter);
        s.addEventListener('change', filter);
    })();
</script>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
