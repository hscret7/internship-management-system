<%--
    header.jsp - Khung đầu trang dùng chung cho mọi role.
    Owner: FE1

    Cách dùng trong một trang JSP:
        <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <c:set var="pageTitle"  value="Hồ sơ cá nhân" />
        <c:set var="activePage" value="profile" />
        <%@ include file="/WEB-INF/views/common/header.jsp" %>
        ... nội dung trang ...
        <%@ include file="/WEB-INF/views/common/footer.jsp" %>

    Trang KHÔNG cần lặp lại <html>, <head>, sidebar hay topbar.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:if test="${empty pageTitle}">
    <c:set var="pageTitle" value="Hệ thống quản lý thực tập" />
</c:if>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${pageTitle} - Internship Management System</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="${ctx}/assets/css/app.css" rel="stylesheet">
</head>
<body>

<div class="app-shell">

    <%@ include file="/WEB-INF/views/common/sidebar.jsp" %>

    <div class="app-backdrop" id="appBackdrop" hidden></div>

    <div class="app-main">

        <header class="app-topbar">
            <button type="button" class="app-menu-toggle" aria-label="Mở menu" aria-expanded="false">
                <i class="bi bi-list"></i>
            </button>

            <h1 class="app-topbar__title">${pageTitle}</h1>

            <div class="app-topbar__spacer"></div>

            <c:if test="${not empty sessionScope.user}">
                <div class="app-user">
                    <span class="app-user__avatar" aria-hidden="true">
                        <c:choose>
                            <c:when test="${not empty sessionScope.user.username}">
                                <c:out value="${fn:toUpperCase(fn:substring(sessionScope.user.username, 0, 1))}" />
                            </c:when>
                            <c:otherwise><i class="bi bi-person"></i></c:otherwise>
                        </c:choose>
                    </span>
                    <span class="app-user__meta">
                        <span class="app-user__name">${sessionScope.user.username}</span>
                        <span class="app-user__role">
                            <c:choose>
                                <c:when test="${sessionScope.user.admin}">Quản trị viên</c:when>
                                <c:when test="${sessionScope.user.HR}">Nhân sự (HR)</c:when>
                                <c:when test="${sessionScope.user.roleName eq 'MENTOR'}">Mentor</c:when>
                                <c:otherwise>Thực tập sinh</c:otherwise>
                            </c:choose>
                        </span>
                    </span>
                    <a href="${ctx}/auth/logout" class="btn btn-outline-brand btn-sm">
                        <i class="bi bi-box-arrow-right"></i>
                        <span class="d-none d-sm-inline ms-1">Đăng xuất</span>
                    </a>
                </div>
            </c:if>
        </header>

        <main class="app-content">

            <c:if test="${not empty sessionScope.flashSuccess}">
                <div class="alert alert-success d-flex align-items-start gap-2" role="alert">
                    <i class="bi bi-check-circle-fill mt-1"></i>
                    <div class="flex-grow-1">${sessionScope.flashSuccess}</div>
                    <button type="button" class="btn-close" data-dismiss="alert" aria-label="Đóng"></button>
                </div>
                <c:remove var="flashSuccess" scope="session" />
            </c:if>

            <c:if test="${not empty sessionScope.flashError}">
                <div class="alert alert-danger d-flex align-items-start gap-2" role="alert">
                    <i class="bi bi-exclamation-triangle-fill mt-1"></i>
                    <div class="flex-grow-1">${sessionScope.flashError}</div>
                    <button type="button" class="btn-close" data-dismiss="alert" aria-label="Đóng"></button>
                </div>
                <c:remove var="flashError" scope="session" />
            </c:if>

            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger d-flex align-items-start gap-2" role="alert">
                    <i class="bi bi-exclamation-octagon-fill mt-1"></i>
                    <div class="flex-grow-1">${errorMessage}</div>
                    <button type="button" class="btn-close" data-dismiss="alert" aria-label="Đóng"></button>
                </div>
            </c:if>
