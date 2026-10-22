package com.internship.controller.auth;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.internship.dao.RoleDAO;
import com.internship.dao.UserDAO;
import com.internship.model.User;
import com.internship.service.UserService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterServletTest {

    @Test
    void initDoesNotCrashAndInitializesUserService() throws Exception {
        RegisterServlet servlet = new RegisterServlet();
        assertDoesNotThrow(() -> {
            servlet.init();
        }, "RegisterServlet init() should not depend on SMTP configuration");

        Field userServiceField = RegisterServlet.class.getDeclaredField("userService");
        userServiceField.setAccessible(true);
        Object userServiceInstance = userServiceField.get(servlet);
        assertNotNull(userServiceInstance, "userService must be initialized after init()");
    }

    @Test
    void doGetForwardsToRegisterJsp() throws Exception {
        RegisterServlet servlet = new RegisterServlet();
        FakeHttp http = new FakeHttp();

        servlet.doGet(http.request, http.response);

        assertTrue(http.forwarded, "doGet must forward to register view");
        assertEquals("/register.jsp", http.forwardTarget);
    }

    @Test
    void doPostWithValidationErrorSetsErrorMessageAndForwardsToRegisterJsp() throws Exception {
        UserService userService = new UserService(new UserDAO(), new RoleDAO()) {
            @Override
            public User registerIntern(String username, String email, String fullName, String password, String confirmPassword, String phone, String university, String major, String gpa) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
            }
        };

        RegisterServlet servlet = new RegisterServlet(userService);
        FakeHttp http = new FakeHttp();
        http.params.put("username", "testuser");
        http.params.put("email", "test@example.com");
        http.params.put("fullName", "Test User");
        http.params.put("password", "secret123");
        http.params.put("confirmPassword", "different123");

        servlet.doPost(http.request, http.response);

        assertEquals("Mật khẩu xác nhận không khớp.", http.requestAttributes.get("errorMessage"));
        assertTrue(http.forwarded, "Must forward back to register.jsp on validation failure");
        assertEquals("/register.jsp", http.forwardTarget);
        assertNull(http.redirectLocation, "Must not redirect on validation failure");
    }

    @Test
    void doPostSuccessSetsFlashAndRedirectsToLogin() throws Exception {
        AtomicBoolean registered = new AtomicBoolean(false);
        UserService userService = new UserService(new UserDAO(), new RoleDAO()) {
            @Override
            public User registerIntern(String username, String email, String fullName, String password, String confirmPassword, String phone, String university, String major, String gpa) {
                assertEquals("0912345678",phone);assertEquals("ICTU",university);
                assertEquals("ATTT",major);assertEquals("3.75",gpa);
                registered.set(true);
                User user = new User();
                user.setUserId(1L);
                return user;
            }
        };

        RegisterServlet servlet = new RegisterServlet(userService);
        FakeHttp http = new FakeHttp();
        http.params.put("phone", "0912345678");http.params.put("university", "ICTU");
        http.params.put("major", "ATTT");http.params.put("gpa", "3.75");
        http.params.put("username", "intern_john");
        http.params.put("email", "john@example.com");
        http.params.put("fullName", "John Intern");
        http.params.put("password", "Pass12345");
        http.params.put("confirmPassword", "Pass12345");

        servlet.doPost(http.request, http.response);

        assertTrue(registered.get(), "UserService.registerIntern must be called");
        assertEquals("Đăng ký tài khoản thành công. Vui lòng đăng nhập.",
                http.sessionAttributes.get("flashSuccess"));
        assertEquals("/test/auth/login", http.redirectLocation);
        assertFalse(http.forwarded, "Must not forward on successful registration");
    }

    private static final class FakeHttp {
        final Map<String, Object> sessionAttributes = new HashMap<>();
        final Map<String, Object> requestAttributes = new HashMap<>();
        final Map<String, String> params = new HashMap<>();

        String forwardTarget;
        boolean forwarded = false;
        String redirectLocation;

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
                    case "getContextPath" -> "/test";
                    case "getParameter" -> params.get(args[0]);
                    case "setAttribute" -> { requestAttributes.put((String) args[0], args[1]); yield null; }
                    case "getAttribute" -> requestAttributes.get(args[0]);
                    case "setCharacterEncoding" -> null;
                    case "getRequestDispatcher" -> {
                        forwardTarget = (String) args[0];
                        yield (RequestDispatcher) Proxy.newProxyInstance(
                                RequestDispatcher.class.getClassLoader(),
                                new Class<?>[]{RequestDispatcher.class},
                                (dispProxy, dispMethod, dispArgs) -> {
                                    if ("forward".equals(dispMethod.getName())) {
                                        forwarded = true;
                                    }
                                    return null;
                                }
                        );
                    }
                    default -> null;
                }
        );

        final HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "sendRedirect" -> { redirectLocation = (String) args[0]; yield null; }
                    default -> null;
                }
        );
    }
}

