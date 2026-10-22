package com.internship.controller.admin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.service.UserService;
import com.internship.service.AuditLogService;
import com.internship.dao.AuditLogDAO;
import com.internship.util.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminUserServletTest {

    @Test
    void postWithoutCsrfTokenReturns403() throws Exception {
        AtomicBoolean createUserCalled = new AtomicBoolean(false);
        UserService userService = new UserService(new UserDAO(), new RoleDAO()) {
            @Override
            public CreateUserResult createUser(String username, String email, String fullName, String role) {
                createUserCalled.set(true);
                return null;
            }
        };

        CapturingAudit audit = new CapturingAudit();
        AdminUserServlet servlet = new AdminUserServlet(userService, audit);
        FakeHttp http = new FakeHttp();

        servlet.doPost(http.request, http.response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, http.getStatus());
        assertEquals(0, audit.calls);
        assertTrue(http.getBody().contains("Invalid CSRF token."));
        assertFalse(createUserCalled.get(), "UserService.createUser must not be called when CSRF token is missing");
    }

    @Test
    void postWithInvalidCsrfTokenReturns403() throws Exception {
        AtomicBoolean createUserCalled = new AtomicBoolean(false);
        UserService userService = new UserService(new UserDAO(), new RoleDAO()) {
            @Override
            public CreateUserResult createUser(String username, String email, String fullName, String role) {
                createUserCalled.set(true);
                return null;
            }
        };

        CapturingAudit audit = new CapturingAudit();
        AdminUserServlet servlet = new AdminUserServlet(userService, audit);
        FakeHttp http = new FakeHttp();
        http.params.put("csrfToken", "invalid-or-forged-token");

        servlet.doPost(http.request, http.response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, http.getStatus());
        assertEquals(0, audit.calls);
        assertTrue(http.getBody().contains("Invalid CSRF token."));
        assertFalse(createUserCalled.get(), "UserService.createUser must not be called when CSRF token is invalid");
    }

    @Test
    void postWithValidCsrfAndEmailSuccessReturns201WithoutTemporaryPassword() throws Exception {
        User user = new User();
        user.setUserId(42L);
        Role role = new Role();
        role.setRoleName("HR");
        user.setRole(role);

        UserService userService = new UserService(new UserDAO(), new RoleDAO()) {
            @Override
            public CreateUserResult createUser(String username, String email, String fullName, String roleName) {
                return new CreateUserResult(user, "SuperSecretPassword123", true);
            }
        };

        CapturingAudit audit = new CapturingAudit();
        AdminUserServlet servlet = new AdminUserServlet(userService, audit);
        FakeHttp http = new FakeHttp();
        String token = CsrfToken.expose(http.request);
        http.params.put("csrfToken", token);
        http.params.put("username", "hr_user");
        http.params.put("email", "hr@example.com");
        http.params.put("fullName", "HR Manager");
        http.params.put("role", "HR");

        servlet.doPost(http.request, http.response);

        assertEquals(HttpServletResponse.SC_CREATED, http.getStatus());
        assertEquals(1, audit.calls);
        assertEquals(99L, audit.actor);
        assertEquals(user.getUserId(), audit.target);
        assertEquals("ACCOUNT_CREATE", audit.action);
        assertEquals("SUCCESS", audit.result);
        String body = http.getBody();
        assertTrue(body.contains("\"success\": true"));
        assertTrue(body.contains("\"userId\": 42"));
        assertTrue(body.contains("\"emailSent\": true"));
        assertFalse(body.contains("temporaryPassword"), "Response must NOT expose temporaryPassword when emailSent is true");
        assertFalse(body.contains("SuperSecretPassword123"), "Response must NOT expose plain password when emailSent is true");
    }

    @Test
    void postWithValidCsrfAndEmailFailureReturns201WithTemporaryPassword() throws Exception {
        User user = new User();
        user.setUserId(84L);
        Role role = new Role();
        role.setRoleName("MENTOR");
        user.setRole(role);

        UserService userService = new UserService(new UserDAO(), new RoleDAO()) {
            @Override
            public CreateUserResult createUser(String username, String email, String fullName, String roleName) {
                return new CreateUserResult(user, "FallbackTempPass789", false);
            }
        };

        CapturingAudit audit = new CapturingAudit();
        AdminUserServlet servlet = new AdminUserServlet(userService, audit);
        FakeHttp http = new FakeHttp();
        String token = CsrfToken.expose(http.request);
        http.params.put("csrfToken", token);
        http.params.put("username", "mentor_jane");
        http.params.put("email", "jane@example.com");
        http.params.put("fullName", "Jane Mentor");
        http.params.put("role", "MENTOR");

        servlet.doPost(http.request, http.response);

        assertEquals(HttpServletResponse.SC_CREATED, http.getStatus());
        assertEquals(1, audit.calls);
        assertEquals(99L, audit.actor);
        assertEquals(user.getUserId(), audit.target);
        assertEquals("ACCOUNT_CREATE", audit.action);
        assertEquals("SUCCESS", audit.result);
        String body = http.getBody();
        assertTrue(body.contains("\"success\": true"));
        assertTrue(body.contains("\"userId\": 84"));
        assertTrue(body.contains("\"emailSent\": false"));
        assertTrue(body.contains("\"temporaryPassword\": \"FallbackTempPass789\""),
                "Response MUST return temporaryPassword for Admin when email sending fails");
    }

    @Test
    void postWithValidationErrorReturns400() throws Exception {
        UserService userService = new UserService(new UserDAO(), new RoleDAO()) {
            @Override
            public CreateUserResult createUser(String username, String email, String fullName, String roleName) {
                throw new IllegalArgumentException("Username already exists.");
            }
        };

        CapturingAudit audit = new CapturingAudit();
        AdminUserServlet servlet = new AdminUserServlet(userService, audit);
        FakeHttp http = new FakeHttp();
        String token = CsrfToken.expose(http.request);
        http.params.put("csrfToken", token);
        http.params.put("username", "duplicate_user");

        servlet.doPost(http.request, http.response);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, http.getStatus());
        assertEquals(0, audit.calls);
        String body = http.getBody();
        assertTrue(body.contains("\"success\": false"));
        assertTrue(body.contains("Username already exists."));
    }

    @Test
    void initSafelyInitializesUserServiceWhenSmtpUnavailable() throws Exception {
        AdminUserServlet servlet = new AdminUserServlet();
        assertDoesNotThrow(() -> {
            servlet.init();
        }, "AdminUserServlet.init() must not crash even if SMTP configuration is absent");

        Field userServiceField = AdminUserServlet.class.getDeclaredField("userService");
        userServiceField.setAccessible(true);
        Object userServiceInstance = userServiceField.get(servlet);
        assertNotNull(userServiceInstance, "userService must be instantiated with fallback email service after init()");
    }

    private static final class CapturingAudit extends AuditLogService {
        int calls;
        Long actor, target;
        String action, result;
        CapturingAudit() {
            super(new AuditLogDAO(() -> { throw new AssertionError("Unit test must never open an audit DB connection"); }));
        }
        @Override public boolean recordBestEffort(Long actor, String action, String type, Long target, String result) {
            this.calls++;
            this.actor = actor;
            this.target = target;
            this.action = action;
            this.result = result;
            assertEquals("USER", type);
            return true;
        }
    }

    private static final class FakeHttp {
        final Map<String, Object> sessionAttributes = new HashMap<>();
        FakeHttp() {
            User admin = new User();
            admin.setUserId(99L);
            admin.setStatus(com.internship.model.UserStatus.ACTIVE);
            var role = new com.internship.model.Role(); role.setRoleName("ADMIN"); admin.setRole(role);
            sessionAttributes.put("currentUser", admin);
        }
        final Map<String, Object> requestAttributes = new HashMap<>();
        final Map<String, String> params = new HashMap<>();
        final StringWriter out = new StringWriter();
        final PrintWriter writer = new PrintWriter(out);
        final int[] status = new int[]{HttpServletResponse.SC_OK};

        final HttpSession session = (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> sessionAttributes.get(args[0]);
                    case "setAttribute" -> { sessionAttributes.put((String) args[0], args[1]); yield null; }
                    default -> null;
                }
        );

        final HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSession" -> session;
                    case "getParameter" -> params.get(args[0]);
                    case "setAttribute" -> { requestAttributes.put((String) args[0], args[1]); yield null; }
                    case "getAttribute" -> requestAttributes.get(args[0]);
                    case "setCharacterEncoding" -> null;
                    default -> null;
                }
        );

        final HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setStatus" -> { status[0] = (Integer) args[0]; yield null; }
                    case "getStatus" -> status[0];
                    case "getWriter" -> writer;
                    case "setContentType", "setCharacterEncoding" -> null;
                    default -> null;
                }
        );

        int getStatus() {
            return status[0];
        }

        String getBody() {
            writer.flush();
            return out.toString();
        }
    }
}

