<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8" %>

<%@ page import="com.internship.model.User" %>

<%
    User currentUser =
            (User) session.getAttribute("currentUser");

    String currentRole =
            (String) session.getAttribute("currentRole");
%>

<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <title>Internship Management System</title>
</head>

<body>

    <h1>Internship Management System</h1>

    <% if (currentUser == null) { %>

        <p>Bạn chưa đăng nhập.</p>

        <a href="<%= request.getContextPath() %>/auth/login">
            Đăng nhập
        </a>

    <% } else { %>

        <p>
            Xin chào:
            <strong>
                <%= currentUser.getFullName() %>
            </strong>
        </p>

        <p>
            Username:
            <%= currentUser.getUsername() %>
        </p>

        <p>
            Role:
            <%= currentRole %>
        </p>

        <form
            method="post"
            action="<%= request.getContextPath() %>/auth/logout"
        >
            <button type="submit">
                Đăng xuất
            </button>
        </form>

    <% } %>

</body>

</html>