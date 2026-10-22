<%-- 404 - Không tìm thấy trang. Owner: FE1 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>404 - Không tìm thấy trang</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/app.css" rel="stylesheet">
</head>
<body>
<div class="error-page">
    <div class="error-page__inner">
        <div class="error-page__code">404</div>
        <h1 class="error-page__title">Không tìm thấy trang</h1>
        <p class="error-page__text">
            Đường dẫn bạn truy cập không tồn tại hoặc đã bị thay đổi.
            Kiểm tra lại địa chỉ trên thanh trình duyệt, hoặc quay về trang chủ.
        </p>
        <a href="${pageContext.request.contextPath}/" class="btn btn-brand">
            <i class="bi bi-house-door me-1"></i>Về trang chủ
        </a>
    </div>
</div>
</body>
</html>
