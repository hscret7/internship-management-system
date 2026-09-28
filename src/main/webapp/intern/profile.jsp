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
<c:set var="activePage" value="profile" />
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="page-head">
    <div>
        <h2 class="page-head__title">Hồ sơ thực tập sinh</h2>
        <p class="page-head__sub">
            Thông tin này dùng để HR xét duyệt và làm hồ sơ thực tập.
        </p>
    </div>
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
