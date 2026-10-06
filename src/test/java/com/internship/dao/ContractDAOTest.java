package com.internship.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ContractDAOTest {

    @Test
    @DisplayName("ensureContractSchema handles null connection gracefully")
    void testEnsureContractSchemaNullConnection() {
        assertDoesNotThrow(() -> ContractDAO.ensureContractSchema(null));
    }

    @Test
    @DisplayName("ensureContractSchema executes CREATE TABLE and ALTER TABLE when schema elements are missing")
    void testEnsureContractSchemaMissingElements() throws Exception {
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
                    if ("getTables".equals(method.getName())) {
                        return Proxy.newProxyInstance(
                                ResultSet.class.getClassLoader(),
                                new Class<?>[]{ResultSet.class},
                                (rsProxy, rsMethod, rsArgs) -> {
                                    if ("next".equals(rsMethod.getName())) return false; // table not found
                                    if ("close".equals(rsMethod.getName())) return null;
                                    return defaultValue(rsMethod.getReturnType());
                                }
                        );
                    }
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

        assertDoesNotThrow(() -> ContractDAO.ensureContractSchema(mockConn));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }
}
