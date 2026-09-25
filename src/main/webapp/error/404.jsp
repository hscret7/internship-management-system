<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>404 - Không tìm thấy trang</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light d-flex align-items-center justify-content-center min-vh-100">
    <div class="text-center p-5 bg-white rounded shadow-sm" style="max-width: 500px;">
        <h1 class="display-1 text-warning fw-bold">404</h1>
        <h4 class="mb-3">Không tìm thấy tài nguyên</h4>
        <p class="text-muted">Đường dẫn bạn yêu cầu không tồn tại trên hệ thống.</p>
        <a href="${pageContext.request.contextPath}/" class="btn btn-primary mt-2">Quay lại trang chủ</a>
    </div>
</body>
</html>
