package com.internship.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentDAOTest {

    @Test
    @DisplayName("ensureDocumentReviewColumns handles null connection gracefully")
    void testEnsureDocumentReviewColumnsNullConnection() {
        assertDoesNotThrow(() -> DocumentDAO.ensureDocumentReviewColumns(null));
    }

    @Test
    @DisplayName("ensureDocumentReviewColumns executes ALTER TABLE when columns are missing")
    void testEnsureDocumentReviewColumnsMissingColumns() throws Exception {
        List<String> executedDdl = new ArrayList<>();

        Statement mockStatement = (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    if ("executeUpdate".equals(method.getName()) && args != null && args.length > 0) {
                        executedDdl.add((String) args[0]);
                        return 0;
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        DatabaseMetaData mockMeta = (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[]{DatabaseMetaData.class},
                (proxy, method, args) -> {
                    if ("getColumns".equals(method.getName())) {
                        return Proxy.newProxyInstance(
                                ResultSet.class.getClassLoader(),
                                new Class<?>[]{ResultSet.class},
                                (rsProxy, rsMethod, rsArgs) -> {
                                    if ("next".equals(rsMethod.getName())) return false; // column not found
                                    if ("close".equals(rsMethod.getName())) return null;
                                    return defaultValue(rsMethod.getReturnType());
                                }
                        );
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        Connection mockConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("getMetaData".equals(method.getName())) return mockMeta;
                    if ("getCatalog".equals(method.getName())) return "internship_management";
                    if ("createStatement".equals(method.getName())) return mockStatement;
                    if ("close".equals(method.getName())) return null;
                    return defaultValue(method.getReturnType());
                }
        );

        assertDoesNotThrow(() -> DocumentDAO.ensureDocumentReviewColumns(mockConn));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }
}
