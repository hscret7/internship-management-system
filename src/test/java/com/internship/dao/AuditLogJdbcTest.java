package com.internship.dao;

import com.internship.model.*;
import com.internship.service.AuditLogService;
import com.internship.service.AuditLogService.Query;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL, dedicated fixture only; intentionally uses a non-UTC JDBC timezone. */
@EnabledIfSystemProperty(named="be1.audit.jdbc",matches="true")
class AuditLogJdbcTest {
    private static final String URL="jdbc:mysql://127.0.0.1:13320/be1_audit_test?connectionTimeZone=Asia/Ho_Chi_Minh";
    private Connection open()throws SQLException{return DriverManager.getConnection(URL,"root","");}
    private AuditLogDAO dao(){return new AuditLogDAO(this::open);}
    private Query query(Long actor,String action,String type,String result,Instant from,Instant to,int page,int size){return new Query(actor,action,type,result,from,to,page,size);}
    private Query all(){return query(null,null,null,null,null,null,1,100);}
    private void sql(String command)throws SQLException{
        try(var c=open();var s=c.createStatement()){s.execute("SET SESSION time_zone = '+00:00'");s.execute(command);}
    }
    @BeforeEach void fixture()throws Exception {
        assertEquals(URL,System.getProperty("be1.audit.jdbc.url",URL),"Only the disposable audit fixture URL is accepted");
        try(var c=open();var s=c.createStatement()){
            try(var r=s.executeQuery("SELECT DATABASE()")){assertTrue(r.next());assertEquals("be1_audit_test",r.getString(1));}
            s.execute("DROP TABLE IF EXISTS audit_logs");
            String schema=Files.readString(Path.of("sql/schema_be1_audit.sql")).replaceAll("(?m)^--.*$","");
            for(String command:schema.split(";"))if(!command.isBlank())s.execute(command);
        }
    }
    @Test void insertKeepsNullableAndHistoricalIdentifiers()throws Exception {
        dao().insert(null,"LOGIN","USER",null,"FAILURE");
        dao().insert(999L,"ACCOUNT_CREATE","USER",888L,"SUCCESS");
        var rows=dao().search(all()).items();assertEquals(2,rows.size());
        var missing=rows.stream().filter(n->n.actorUserId()==null).findFirst().orElseThrow();assertNull(missing.objectId());
        var historical=rows.stream().filter(n->n.actorUserId()!=null).findFirst().orElseThrow();
        assertEquals(999L,historical.actorUserId());assertEquals(888L,historical.objectId());
    }
    @Test void timestampsRoundTripUtcDespiteNonUtcJdbcTimezone()throws Exception {
        sql("INSERT INTO audit_logs(actor_user_id,action,object_type,object_id,occurred_at,result) VALUES(7,'LOGIN','USER',7,'2026-10-07 11:24:53.246524','SUCCESS')");
        assertEquals(Instant.parse("2026-10-07T11:24:53.246524Z"),dao().search(all()).items().getFirst().occurredAt());
    }
    @Test void dateFilterIsInclusiveFromAndExclusiveToAtMicrosecondPrecision()throws Exception {
        sql("INSERT INTO audit_logs(action,object_type,occurred_at,result) VALUES ('LOGIN','USER','2026-10-07 11:24:53.246523','SUCCESS'),('LOGIN','USER','2026-10-07 11:24:53.246524','SUCCESS'),('LOGIN','USER','2026-10-07 11:24:53.246525','SUCCESS')");
        var from=Instant.parse("2026-10-07T11:24:53.246524Z");var to=Instant.parse("2026-10-07T11:24:53.246525Z");
        var page=dao().search(query(null,null,null,null,from,to,1,20));assertEquals(1,page.total());assertEquals(from,page.items().getFirst().occurredAt());
    }
    @Test void combinedFiltersUseAndAndUnknownCodeReturnsEmpty()throws Exception {
        dao().insert(7L,"LOGIN","USER",7L,"SUCCESS");dao().insert(7L,"LOGIN","USER",7L,"FAILURE");
        dao().insert(8L,"LOGIN","USER",8L,"SUCCESS");dao().insert(7L,"LOGOUT","USER",7L,"SUCCESS");
        var p=dao().search(query(7L,"LOGIN","USER","SUCCESS",null,null,1,20));assertEquals(1,p.total());assertEquals(1,p.items().size());
        var empty=dao().search(query(null,"UNKNOWN_EVENT",null,null,null,null,1,20));assertEquals(0,empty.total());assertTrue(empty.items().isEmpty());
    }
    @Test void paginationHasStableIdTieBreakAndCountsAllMatchingRows()throws Exception {
        sql("INSERT INTO audit_logs(action,object_type,occurred_at,result) VALUES ('LOGIN','USER','2026-10-07 12:00:00','SUCCESS'),('LOGIN','USER','2026-10-07 12:00:00','SUCCESS'),('LOGIN','USER','2026-10-07 12:00:00','SUCCESS')");
        var a=dao().search(query(null,null,null,null,null,null,1,2));var b=dao().search(query(null,null,null,null,null,null,2,2));
        assertEquals(3,a.total());assertEquals(3,b.total());assertEquals(List.of(3L,2L),a.items().stream().map(AuditLog::id).toList());
        assertEquals(List.of(1L),b.items().stream().map(AuditLog::id).toList());
        var beyond=dao().search(query(null,null,null,null,null,null,3,2));assertTrue(beyond.items().isEmpty());assertEquals(3,beyond.total());
    }
    @Test void databaseAssignedInsertTimeIsCurrentInstant()throws Exception {
        Instant before=Instant.now().minusSeconds(2);dao().insert(7L,"LOGIN","USER",7L,"SUCCESS");
        Instant value=dao().search(all()).items().getFirst().occurredAt();assertFalse(value.isBefore(before));assertFalse(value.isAfter(Instant.now().plusSeconds(2)));
    }
    @Test void adminServiceRejectsOtherRolesBeforeOpeningDatabase()throws Exception {
        int[] calls={0};var service=new AuditLogService(new AuditLogDAO(()->{calls[0]++;return open();}));
        User user=new User();user.setStatus(UserStatus.ACTIVE);Role role=new Role();user.setRole(role);
        for(String name:List.of("HR","MENTOR","INTERN")){role.setRoleName(name);assertThrows(SecurityException.class,()->service.search(user,all()));}
        assertThrows(SecurityException.class,()->service.search(null,all()));assertEquals(0,calls[0]);
        role.setRoleName("ADMIN");assertEquals(0,service.search(user,all()).total());assertEquals(1,calls[0]);
        user.setStatus(UserStatus.LOCKED);assertThrows(SecurityException.class,()->service.search(user,all()));assertEquals(1,calls[0]);
    }
    @Test void failedPersistenceIsExplicitAndBestEffortDoesNotInventSuccess()throws Exception {
        sql("DROP TABLE audit_logs");var service=new AuditLogService(dao());
        assertThrows(SQLException.class,()->service.record(7L,"LOGIN","USER",7L,"SUCCESS"));
        assertFalse(service.recordBestEffort(7L,"LOGIN","USER",7L,"SUCCESS"));
        assertThrows(SQLException.class,()->dao().search(all()));
    }
}
