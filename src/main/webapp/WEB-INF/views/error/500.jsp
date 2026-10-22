<%-- 500 - Lỗi hệ thống. Owner: FE1
     Bản cũ là HTML trần, không có CSS chung nên trông lệch hẳn so với 403/404.
     Cùng một sự cố mà HR gặp hai giao diện lỗi khác nhau tuỳ theo lỗi do
     Tomcat map (web.xml error-page) hay do servlet forward tay. --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>500 - Lỗi hệ thống</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/app.css" rel="stylesheet">
</head>
<body>
<div class="error-page">
    <div class="error-page__inner">
        <div class="error-page__code">500</div>
        <h1 class="error-page__title">Hệ thống đang gặp sự cố</h1>
        <p class="error-page__text">
            Yêu cầu của bạn chưa thực hiện được do lỗi phía máy chủ.
            Vui lòng thử lại sau ít phút; nếu vẫn lỗi, hãy báo cho HR kèm thời điểm bạn gặp lỗi.
        </p>
        <%-- Không in ${exception} / stack trace ra đây: lộ đường dẫn, tên bảng và
             câu SQL cho người dùng cuối. Chi tiết đã nằm trong log của Tomcat. --%>
        <a href="${pageContext.request.contextPath}/" class="btn btn-brand">
            <i class="bi bi-house-door me-1"></i>Về trang chủ
        </a>
    </div>
</div>
</body>
</html>
