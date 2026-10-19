<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<%-- Chốt locale cho fmt:* theo quy ước của intern/contract.jsp. Thiếu dòng này,
     request không có Accept-Language (curl, bot, một số proxy) làm JSTL không
     xác định được locale và rơi về value.toString(): ngày ra
     "2026-09-05 09:00:00.0" thay vì "05/09/2026 09:00". --%>
<fmt:setLocale value="vi_VN" />

<%-- Chốt múi giờ, cùng lý do với locale ở trên. Thiếu dòng này thì fmt:formatDate
     rơi về TimeZone.getDefault() của JVM: máy dev Windows UTC+7 nên tình cờ đúng và
     che mất lỗi, còn host chạy UTC (container, CI, prod mặc định) hiện ngày nộp/ngày
     duyệt lùi đúng 7 tiếng. Đã tái hiện trên Tomcat: cùng dữ liệu, chỉ đổi
     -Duser.timezone=UTC là "01/09/2026 08:30" thành "01/09/2026 01:30".

     Không đụng tới dữ liệu: cột vẫn là TIMESTAMP và db.url đã ghim
     serverTimezone=Asia/Ho_Chi_Minh, nên instant đọc lên đúng ở mọi múi giờ JVM —
     chỉ khâu hiển thị cần ghim. Không cộng trừ giờ vào giá trị. --%>
<fmt:setTimeZone value="Asia/Ho_Chi_Minh" />

<c:set var="pageTitle" value="Đơn ứng tuyển của tôi" />
<%-- activePage rieng, KHONG dung 'documents': gia tri do thuoc muc
     "CV và đơn ứng tuyển" -> /intern/profile#documents trong sidebar. --%>
<c:set var="activePage" value="intern-applications" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="container-fluid mt-4">

    <div class="page-head mb-4">
        <h2 class="page-head__title">Đơn ứng tuyển của tôi</h2>
        <p class="page-head__sub">Theo dõi trạng thái các đơn bạn đã nộp cho nhà tuyển dụng.</p>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div>
    </c:if>
    <%-- errorMessage da duoc header.jsp render san (khoi flash chung).
         Render lai o day se hien 2 lan. --%>

    <div class="card shadow-sm mb-4">
        <div class="card-body d-flex justify-content-between align-items-center flex-wrap gap-2">
            <div>
                <div class="fw-bold">Nộp đơn ứng tuyển mới</div>
                <small class="text-muted">Đơn sau khi nộp sẽ ở trạng thái chờ HR duyệt.</small>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/intern/applications" class="m-0">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <button type="submit" class="btn btn-primary">
                    <i class="bi bi-send me-1"></i>Nộp đơn ứng tuyển mới
                </button>
            </form>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-header bg-white py-3 border-bottom">
            <h5 class="card-title mb-0 fw-bold">Danh sách đơn (${not empty applications ? applications.size() : 0})</h5>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">Mã đơn</th>
                            <th>Trạng thái</th>
                            <th>Ngày nộp</th>
                            <th>Người duyệt</th>
                            <th>Ngày duyệt</th>
                            <th class="pe-3">Lý do từ chối</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty applications}">
                                <c:forEach var="app" items="${applications}">
                                    <tr>
                                        <td class="ps-3 fw-bold text-muted">#<c:out value="${app.applicationId}"/></td>
                                        <td>
                                            <%-- Nhan hien thi la tieng Viet, gia tri gui cho BE2/BE3 van la
                                                 PENDING/APPROVED/REJECTED. Truoc day cho nay in thang ma
                                                 trang thai nen TTS doc ra "REJECTED"; nay dung cung bo tu
                                                 voi voi hr/application-review.jsp. --%>
                                            <c:choose>
                                                <c:when test="${app.status == 'PENDING'}">
                                                    <span class="badge bg-warning text-dark">Chờ duyệt</span>
                                                </c:when>
                                                <c:when test="${app.status == 'APPROVED'}">
                                                    <span class="badge bg-success">Đã duyệt</span>
                                                </c:when>
                                                <c:when test="${app.status == 'REJECTED'}">
                                                    <span class="badge bg-danger">Đã từ chối</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary"><c:out value="${app.status}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <fmt:formatDate value="${app.submittedAt}" pattern="dd/MM/yyyy HH:mm" var="submittedFmt"/>
                                            <c:choose>
                                                <c:when test="${not empty submittedFmt}"><c:out value="${submittedFmt}"/></c:when>
                                                <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty app.reviewerName}"><c:out value="${app.reviewerName}"/></c:when>
                                                <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <%-- reviewedAt NULL khi đơn còn PENDING: khong de trong o
                                             (de nham la loi tai du lieu), hien gach ngang. --%>
                                        <td>
                                            <fmt:formatDate value="${app.reviewedAt}" pattern="dd/MM/yyyy HH:mm" var="reviewedFmt"/>
                                            <c:choose>
                                                <c:when test="${not empty reviewedFmt}"><c:out value="${reviewedFmt}"/></c:when>
                                                <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="pe-3">
                                            <c:choose>
                                                <c:when test="${not empty app.rejectReason}"><c:out value="${app.rejectReason}"/></c:when>
                                                <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="6" class="text-center py-4 text-muted">
                                        <i class="bi bi-inbox fs-3 d-block mb-2"></i>
                                        Bạn chưa nộp đơn ứng tuyển nào.
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
