<%--
    intern/profile.jsp - Hồ sơ thực tập sinh (US01 / task 3.3). Owner: FE1
    Được chia thành 3 file theo bản chốt, nhưng vẫn giữ đúng đường dẫn này
    để InternProfileServlet của BE2 không phải sửa RequestDispatcher.

    Request attribute do BE2 forward sang:
        profile      : InternProfile hoặc null
        documents    : List<Document>
        errorMessage : String (khi validation thất bại)
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Hồ sơ cá nhân" />
<%-- Trang nay phuc vu 2 muc menu: "Hồ sơ cá nhân" (/intern/profile) va
     "Tài liệu của tôi" (/intern/profile?view=documents#documents).
     Trinh duyet KHONG gui phan #documents len server, nen neu chi dua vao
     fragment thi server khong the biet dang o muc nao - phai dung query param. --%>
<c:set var="activePage" value="${param.view eq 'documents' ? 'documents' : 'profile'}" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Hồ sơ thực tập sinh</h2>
        <p class="page-head__sub">
            Thông tin này dùng để HR xét duyệt và làm hồ sơ thực tập.
        </p>
    </div>
</div>

<div class="alert alert-info">
    <strong>Lưu hồ sơ chưa phải là duyệt thực tập.</strong>
    Sau khi hoàn thiện hồ sơ/tài liệu, <a href="${ctx}/intern/applications">nộp đơn ứng tuyển</a>.
    Đơn mới chờ HR xét duyệt; chỉ sau đó HR mới ghi danh bạn vào chương trình.
    <c:if test="${not empty myApplications}">Đơn gần nhất: <strong><c:out value="${myApplications[0].status}"/></strong>.</c:if>
</div>
<div class="row g-4">

    <div class="col-lg-7">
        <%@ include file="profile-form.jsp" %>
    </div>

    <div class="col-lg-5" id="documents">
        <%@ include file="documents.jsp" %>
    </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
