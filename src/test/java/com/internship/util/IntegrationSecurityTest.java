package com.internship.util;

import com.internship.filter.*;
import com.internship.model.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationSecurityTest {
    @ParameterizedTest @ValueSource(strings={"/admin/users","/admin;probe/users","/%61dmin/users"})
    void internCannotUseAnyAdminPath(String uri) throws Exception {
        Map<String,Object> attributes=new HashMap<>();
        User intern=new User(); Role role=new Role();role.setRoleName("INTERN");intern.setRole(role);attributes.put("currentUser",intern);
        var session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->attributes.get(a==null?null:a[0]));
        var req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){
            case "getServletPath"->"/admin/users";case "getRequestURI"->"/ims"+uri;case "getContextPath"->"/ims";case "getSession"->session;default->null;});
        int[] status={0};boolean[] called={false};
        var resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("sendError"))status[0]=(Integer)a[0];return null;});
        new AuthorizationFilter().doFilter(req,resp,(a,b)->called[0]=true);
        assertEquals(403,status[0]);assertFalse(called[0]);
    }
    @Test void passwordLimitCountsUtf8BytesNotCharacters() {
        assertThrows(IllegalArgumentException.class,()->PasswordUtil.hashPassword("a".repeat(73)));
        assertThrows(IllegalArgumentException.class,()->PasswordUtil.hashPassword("ế".repeat(25)));
        String password="a".repeat(72),hash=PasswordUtil.hashPassword(password);
        assertTrue(PasswordUtil.verifyPassword(password,hash));
        assertFalse(PasswordUtil.verifyPassword(password+"X",hash));
    }
    @Test void privateFilesRejectTraversalAndReadLegacyWithoutPublicUrl(@TempDir Path temp) throws Exception {
        String old=System.getProperty("internship.uploadDir");System.setProperty("internship.uploadDir",temp.resolve("private").toString());
        try {
            Path web=temp.resolve("web");Files.createDirectories(web.resolve("uploads/documents"));
            Path fixture=web.resolve("uploads/documents/example.pdf");Files.writeString(fixture,"test-only");
            assertEquals(fixture,PrivateFileStorage.resolve("uploads/documents/example.pdf",web.toString()));
            assertThrows(java.io.IOException.class,()->PrivateFileStorage.resolve("../secret",web.toString()));
        }finally{if(old==null)System.clearProperty("internship.uploadDir");else System.setProperty("internship.uploadDir",old);}
    }
    @Test void csrfMissingBlocksBeforeChain() throws Exception {
        var req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->m.getName().equals("getMethod")?"POST":null);
        int[] status={0};boolean[] called={false};
        var resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("sendError"))status[0]=(Integer)a[0];return null;});
        new ProfileDocumentCsrfFilter().doFilter(req,resp,(a,b)->called[0]=true);
        assertEquals(403,status[0]);assertFalse(called[0]);
    }
}
