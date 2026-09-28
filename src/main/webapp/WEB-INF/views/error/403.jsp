<%-- 403 - Không có quyền truy cập. Owner: FE1 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>403 - Không có quyền truy cập</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/app.css" rel="stylesheet">
</head>
<body>
<div class="error-page">
    <div class="error-page__inner">
        <div class="error-page__code">403</div>
        <h1 class="error-page__title">Bạn không có quyền truy cập</h1>
        <p class="error-page__text">
            Tài khoản của bạn không được phép xem trang này hoặc thực hiện thao tác này.
            Nếu bạn cho rằng đây là nhầm lẫn, hãy liên hệ HR để kiểm tra lại quyền.
        </p>
        <a href="${pageContext.request.contextPath}/" class="btn btn-brand">
            <i class="bi bi-house-door me-1"></i>Về trang chủ
        </a>
    </div>
</div>
</body>
</html>
