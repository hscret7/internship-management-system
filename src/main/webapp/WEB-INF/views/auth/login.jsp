<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <title>Đăng nhập</title>
</head>

<body>

    <h1>Internship Management System</h1>

    <h2>Đăng nhập</h2>

    <% if (request.getAttribute("error") != null) { %>

        <p>
            <%= request.getAttribute("error") %>
        </p>

    <% } %>

    <form
        method="post"
        action="<%= request.getContextPath() %>/auth/login"
    >

        <div>
            <label for="username">
                Tên đăng nhập
            </label>

            <input
                type="text"
                id="username"
                name="username"
                value="<%=
                    request.getAttribute("username") != null
                        ? request.getAttribute("username")
                        : ""
                %>"
                required
                autocomplete="username"
            >
        </div>

        <br>

        <div>
            <label for="password">
                Mật khẩu
            </label>

            <input
                type="password"
                id="password"
                name="password"
                required
                autocomplete="current-password"
            >
        </div>

        <br>

        <button type="submit">
            Đăng nhập
        </button>

    </form>

</body>

</html>