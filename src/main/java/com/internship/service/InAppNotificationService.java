package com.internship.service;

import com.internship.dao.InAppNotificationDAO;
import com.internship.model.InAppNotification;
import com.internship.util.DBConnection;
import java.sql.*;
import java.util.*;

public class InAppNotificationService {
    @FunctionalInterface public interface Connections {Connection open() throws SQLException;}
    private final Connections connections;
    private final InAppNotificationDAO dao;
    private final boolean enabled;
    public static boolean configured() {return Boolean.getBoolean("inapp.notifications.enabled");}
    public InAppNotificationService() {this(DBConnection::getConnection,new InAppNotificationDAO(),configured());}
    public InAppNotificationService(Connections connections,InAppNotificationDAO dao,boolean enabled) {
        this.connections=Objects.requireNonNull(connections);this.dao=Objects.requireNonNull(dao);this.enabled=enabled;
    }
    public static class Disabled extends IllegalStateException {public Disabled(){super("Inbox disabled");}}
    public static class NotFound extends IllegalArgumentException {public NotFound(){super("Notification unavailable");}}
    public record Page(List<InAppNotification> items,long unreadCount,int page,int size) {
        public Page {items=List.copyOf(items);}
    }
    public Page list(long actor,boolean unread,int page,int size) throws SQLException {
        if(page<1 || page>1000000 || size<1 || size>100)throw new IllegalArgumentException("Invalid pagination");
        return transaction(actor,c->new Page(dao.list(c,actor,unread,page,size),dao.unread(c,actor),page,size));
    }
    public void markRead(long actor,long id) throws SQLException {
        if(id<=0)throw new IllegalArgumentException("Invalid ID");
        transaction(actor,c->{if(!dao.markRead(c,actor,id))throw new NotFound();return null;});
    }
    @FunctionalInterface private interface Work<T>{T run(Connection c)throws SQLException;}
    private <T>T transaction(long actor,Work<T> work)throws SQLException {
        if(actor<=0)throw new IllegalArgumentException("Invalid actor");
        if(!enabled)throw new Disabled();
        try(Connection c=connections.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
            try {
                try(Statement s=c.createStatement()){s.execute("SET SESSION time_zone = '+00:00'");}
                if(!dao.active(c,actor))throw new SecurityException("Active user required");
                T result=work.run(c);c.commit();return result;
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException r){e.addSuppressed(r);}throw e;}
        }
    }
}
