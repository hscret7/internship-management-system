<%-- Trang duyệt hồ sơ chỉ hiển thị khi có route backend truyền dữ liệu và thao tác thật. --%>

<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Duyệt Hồ Sơ Ứng Tuyển" />
<c:set var="activePage" value="applications" />

<%@ include file="/WEB-INF/views/common/header.jsp" %>
    <div class="alert alert-warning" role="status">API duyệt hồ sơ chưa được cung cấp. Màn hình chưa cho phép thao tác để tránh gửi yêu cầu giả.</div>

<div class="container mt-4">
    <!-- Code bảng danh sách Bootstrap 5 và Form duyệt/từ chối của bạn viết ở đây -->
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>