package com.internship.controller;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.service.UserService;
import com.internship.service.AuditLogService;
import com.internship.service.email.EmailException;
import com.internship.service.email.SmtpEmailService;
import com.internship.util.CsrfToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AdminAccountServlet - Admin xem danh sach tai khoan noi bo va tao tai khoan moi.
 * Route: /admin/accounts        (US39 - Task 1.3, owner FE1)
 *
 * Vi sao khong dung /admin/users:
 *   /admin/users da thuoc AdminUserServlet cua BE1 (chi co doPost, tra JSON).
 *   Hai servlet khong the trung url-pattern nen FE1 mo route rieng. File cua
 *   BE1 KHONG bi sua.
 *
 * Phan quyen: AuthorizationFilter da chan /admin/* -> chi ADMIN di qua.
 *             Servlet van kiem tra lai truoc khi doc/ghi (defense in depth).
 *
 * Request attribute doGet dat ra cho view:
 *   users     : List&lt;User&gt; - toan bo tai khoan (UserDAO.findAll)
 *   roles     : List&lt;Role&gt; - vai tro do vao select
 *   csrfToken : String       - qua CsrfToken.expose()
 */
@WebServlet("/admin/accounts")
public class AdminAccountServlet extends HttpServlet {

    private static final Logger LOGGER =
            Logger.getLogger(AdminAccountServlet.class.getName());

    private static final String ROUTE = "/admin/accounts";
    private static final String VIEW = "/WEB-INF/views/admin/accounts.jsp";

    private final AuditLogService auditLog;

    public AdminAccountServlet() {
        this(null, new AuditLogService());
    }

    public AdminAccountServlet(UserService userService, AuditLogService auditLog) {
        this.userService = userService;
        this.auditLog = java.util.Objects.requireNonNull(auditLog);
    }

    private UserDAO userDAO;
    private RoleDAO roleDAO;
    private UserService userService;

    @Override
    public void init() throws ServletException {

        userDAO = new UserDAO();
        roleDAO = new RoleDAO();

        if (userService != null) return;

        try {
            userService = new UserService(
                    userDAO,
                    roleDAO,
                    new SmtpEmailService()
            );
        } catch (RuntimeException e) {
            // Thieu mail.properties khong duoc lam chet ca man hinh:
            // van tao duoc tai khoan, chi la khong gui duoc mail.
            LOGGER.log(
                    Level.WARNING,
                    "EmailService initialization failed; account emails disabled.",
                    e
            );
            userService = new UserService(
                    userDAO,
                    roleDAO,
                    (recipient, subject, content) -> {
                        throw new EmailException("Email service unavailable", e);
                    }
            );
        }
    }

    /**
     * GET /admin/accounts - danh sach tai khoan + form tao moi.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        CsrfToken.expose(request);

        try {
            List<User> users = userDAO.findAll();
            List<Role> roles = roleDAO.findAll();

            request.setAttribute("users", users);
            request.setAttribute("roles", roles);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading accounts", e);
            request.setAttribute(
                    "errorMessage",
                    "Không tải được danh sách tài khoản. Vui lòng thử lại."
            );
        }

        transferSessionMessages(request);

        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /**
     * POST /admin/accounts - tao tai khoan moi.
     *
     * Parameters: username, email, fullName, role, csrfToken
     * Thanh cong: redirect ve GET kem successMessage
     * That bai  : redirect ve GET kem errorMessage
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!CsrfToken.valid(request)) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Invalid form token"
            );
            return;
        }

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String fullName = request.getParameter("fullName");
        String role = request.getParameter("role");

        HttpSession session = request.getSession();

        try {
            UserService.CreateUserResult result =
                    userService.createUser(username, email, fullName, role);

            User actor = (User) session.getAttribute("currentUser");
            auditLog.recordBestEffort(actor.getUserId(), "ACCOUNT_CREATE", "USER",
                    result.user().getUserId(), "SUCCESS");

            session.setAttribute(
                    "successMessage",
                    buildSuccessMessage(result)
            );

        } catch (IllegalArgumentException e) {
            session.setAttribute(
                    "errorMessage",
                    toVietnamese(e.getMessage())
            );

        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error creating account", e);
            session.setAttribute(
                    "errorMessage",
                    "Lỗi hệ thống khi tạo tài khoản. Vui lòng thử lại."
            );
        }

        response.sendRedirect(request.getContextPath() + ROUTE);
    }

    // --- Private helpers ---

    private boolean isAdmin(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        Object attribute = session.getAttribute("currentUser");

        if (!(attribute instanceof User user) || user.getRole() == null) {
            return false;
        }

        return "ADMIN".equalsIgnoreCase(user.getRole().getRoleName());
    }

    /**
     * AdminUserServlet cua BE1 chi tra userId + emailSent ra JSON, khong tra
     * temporaryPassword. O route nay FE1 goi thang UserService nen lay duoc
     * mat khau tam khi gui mail that bai - neu khong thi tai khoan da tao
     * nhung khong ai dang nhap duoc.
     */
    private String buildSuccessMessage(UserService.CreateUserResult result) {

        StringBuilder message = new StringBuilder()
                .append("Đã tạo tài khoản \"")
                .append(result.user().getUsername())
                .append("\" với vai trò ")
                .append(result.user().getRole().getRoleName())
                .append(".");

        if (result.emailSent()) {
            message.append(" Email thông tin tài khoản đã được gửi tới ")
                    .append(result.user().getEmail())
                    .append(".");
        } else {
            message.append(" GỬI EMAIL THẤT BẠI — mật khẩu tạm thời là: ")
                    .append(result.temporaryPassword())
                    .append(". Hãy gửi trực tiếp cho người dùng và yêu cầu")
                    .append(" đổi mật khẩu ở lần đăng nhập đầu tiên.");
        }

        return message.toString();
    }

    /** UserService tra message tieng Anh; doi sang tieng Viet cho khop giao dien. */
    private String toVietnamese(String message) {

        if (message == null || message.isBlank()) {
            return "Không tạo được tài khoản.";
        }

        if (message.startsWith("Invalid role")) {
            return "Vai trò không hợp lệ.";
        }

        return switch (message) {
            case "Username must not be blank." -> "Tên đăng nhập không được để trống.";
            case "Email must not be blank." -> "Email không được để trống.";
            case "Full name must not be blank." -> "Họ tên không được để trống.";
            case "Role must not be blank." -> "Vui lòng chọn vai trò.";
            case "Invalid email format." -> "Email không hợp lệ.";
            case "Username already exists." -> "Tên đăng nhập đã tồn tại.";
            case "Email already exists." -> "Email đã tồn tại.";
            default -> message;
        };
    }

    private void transferSessionMessages(HttpServletRequest request) {

        HttpSession session = request.getSession();

        String success = (String) session.getAttribute("successMessage");
        String error = (String) session.getAttribute("errorMessage");

        if (success != null) {
            request.setAttribute("successMessage", success);
            session.removeAttribute("successMessage");
        }

        if (error != null) {
            request.setAttribute("errorMessage", error);
            session.removeAttribute("errorMessage");
        }
    }
}

