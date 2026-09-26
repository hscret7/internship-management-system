package com.internship.service;

import com.internship.util.ConnectionProvider;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** JDBC transaction double; DAOs are stubbed separately in service tests. */
final class TestConnections implements ConnectionProvider {
    final AtomicInteger commits = new AtomicInteger();
    final AtomicInteger rollbacks = new AtomicInteger();
    private final ThreadLocal<List<Runnable>> onFinish = ThreadLocal.withInitial(ArrayList::new);

    void onFinish(Runnable action) {
        onFinish.get().add(action);
    }

    private void finish() {
        List<Runnable> actions = onFinish.get();
        onFinish.remove();
        for (int i = actions.size() - 1; i >= 0; i--) actions.get(i).run();
    }

    @Override
    public Connection getConnection() {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "commit" -> { commits.incrementAndGet(); finish(); }
                        case "rollback" -> { rollbacks.incrementAndGet(); finish(); }
                        case "close" -> finish();
                        case "toString" -> { return "TestConnection"; }
                        case "isClosed" -> { return false; }
                        case "getAutoCommit" -> { return false; }
                        case "getTransactionIsolation" -> { return Connection.TRANSACTION_READ_COMMITTED; }
                        default -> { }
                    }
                    return null;
                });
    }
}
