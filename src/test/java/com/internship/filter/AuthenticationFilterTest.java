package com.internship.filter;

import com.internship.dao.UserDAO;
import com.internship.model.Role;
import com.internship.model.User;
import com.internship.model.UserStatus;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationFilterTest {
    @Test void activeAccountRefreshesBothSessionAttributes() throws Exception {
        Harness h = new Harness();
        User fresh = user(UserStatus.ACTIVE, "INTERN");
        fresh.setFullName("Updated name");
        h.fresh = fresh;
        h.run();
        assertTrue(h.allowed);
        assertSame(fresh, h.attributes.get("currentUser"));
        assertEquals("INTERN", h.attributes.get("currentRole"));
        assertFalse(h.invalidated);
    }
    @Test void lockedAccountLosesExistingSession() throws Exception { revoked(user(UserStatus.LOCKED, "INTERN")); }
    @Test void inactiveAccountLosesExistingSession() throws Exception { revoked(user(UserStatus.INACTIVE, "INTERN")); }
    @Test void deletedAccountLosesExistingSession() throws Exception { revoked(null); }
    @Test void changedRoleRequiresNewLogin() throws Exception { revoked(user(UserStatus.ACTIVE, "HR")); }
    @Test void missingRoleCannotPass() throws Exception { revoked(user(UserStatus.ACTIVE, null)); }
    @Test void expiredSessionRedirectsWithoutDatabaseLookup() throws Exception {
        Harness h = new Harness(); h.hasSession = false; h.run();
        assertEquals("/ims/auth/login", h.redirect); assertFalse(h.allowed); assertEquals(0, h.lookups);
    }
    @Test void publicLoginWithRevokedSessionCanRenderLogin() throws Exception {
        Harness h = new Harness(); h.path = "/auth/login"; h.fresh = null; h.run();
        assertTrue(h.allowed); assertTrue(h.invalidated); assertNull(h.redirect);
    }
    @Test void anonymousRegistrationDoesNotQueryDatabase() throws Exception {
        Harness h = new Harness(); h.hasSession = false; h.path = "/auth/register"; h.run();
        assertTrue(h.allowed); assertEquals(0, h.lookups);
    }
    @Test void staticAssetsDoNotQueryDatabase() throws Exception {
        Harness h = new Harness(); h.path = "/assets/style.css"; h.run();
        assertTrue(h.allowed); assertEquals(0, h.lookups);
    }
    @Test void databaseFailureBlocksRequestWithoutDestroyingSession() throws Exception {
        Harness h = new Harness(); h.failure = true; h.run();
        assertEquals(503, h.error); assertFalse(h.allowed); assertFalse(h.invalidated);
    }
    @Test void malformedSessionIsRejected() throws Exception {
        Harness h = new Harness(); h.attributes.put("currentUser", "invalid"); h.run();
        assertTrue(h.invalidated); assertFalse(h.allowed); assertEquals(0, h.lookups);
    }
    private void revoked(User fresh) throws Exception {
        Harness h = new Harness(); h.fresh = fresh; h.run();
        assertTrue(h.invalidated); assertFalse(h.allowed); assertEquals("/ims/auth/login", h.redirect);
    }
    private static User user(UserStatus status, String roleName) {
        User user = new User(); user.setUserId(7); user.setStatus(status);
        if (roleName != null) { Role role = new Role(); role.setRoleName(roleName); user.setRole(role); }
        return user;
    }
    private static final class Harness {
        Map<String,Object> attributes = new HashMap<>();
        User fresh = user(UserStatus.ACTIVE, "INTERN");
        boolean hasSession = true, invalidated, allowed, failure;
        String path = "/intern/profile", redirect;
        int error, lookups;
        Harness() { attributes.put("currentUser", user(UserStatus.ACTIVE, "INTERN")); }
        HttpSession session = proxy(HttpSession.class, (method,args) -> switch(method) {
            case "getAttribute" -> attributes.get(args[0]);
            case "setAttribute" -> { attributes.put((String)args[0], args[1]); yield null; }
            case "invalidate" -> { invalidated = true; attributes.clear(); yield null; }
            default -> null;
        });
        void run() throws Exception {
            HttpServletRequest request = proxy(HttpServletRequest.class, (method,args) -> switch(method) {
                case "getContextPath" -> "/ims";
                case "getRequestURI" -> "/ims" + path;
                case "getSession" -> hasSession ? session : null;
                default -> null;
            });
            HttpServletResponse response = proxy(HttpServletResponse.class, (method,args) -> switch(method) {
                case "sendRedirect" -> { redirect = (String)args[0]; yield null; }
                case "sendError" -> { error = (Integer)args[0]; yield null; }
                default -> null;
            });
            new AuthenticationFilter(new UserDAO() {
                @Override public User findById(long id) throws SQLException {
                    assertEquals(7, id); lookups++;
                    if (failure) throw new SQLException("unavailable");
                    return fresh;
                }
            }).doFilter(request, response, (req,res) -> allowed = true);
        }
    }
    interface Call { Object invoke(String method, Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy,method,args) -> call.invoke(method.getName(), args));
    }
}
