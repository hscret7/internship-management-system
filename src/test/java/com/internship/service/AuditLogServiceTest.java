package com.internship.service;
import com.internship.dao.AuditLogDAO;
import com.internship.model.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuditLogServiceTest {
 @Test void invalidCodesNeverReachDao() {
  var service=new AuditLogService(new AuditLogDAO(){ @Override public void insert(Long a,String b,String c,Long d,String e){fail("DAO called");} });
  assertThrows(IllegalArgumentException.class,()->service.record(1L,"password=secret","USER",2L,"SUCCESS"));
  assertThrows(IllegalArgumentException.class,()->service.record(-1L,"LOGIN","USER",null,"FAILURE"));
 }
 @Test void recordPreservesNullableActorAndObject() throws Exception {
  boolean[] called={false};
  var s=new AuditLogService(new AuditLogDAO(){@Override public void insert(Long a,String b,String c,Long d,String e){assertNull(a);assertNull(d);assertEquals("FAILURE",e);called[0]=true;}});
  s.record(null,"LOGIN","USER",null,"FAILURE");assertTrue(called[0]);
 }
 @Test void failureIsObservableWithoutBreakingPrimaryOperation() {
  var s=new AuditLogService(new AuditLogDAO(){@Override public void insert(Long a,String b,String c,Long d,String e)throws SQLException{throw new SQLException("secret detail","08001");}});
  assertFalse(s.recordBestEffort(1L,"LOGIN","USER",1L,"SUCCESS"));
 }
 @Test void pagingAndDateRangeAreBounded() {
  assertThrows(IllegalArgumentException.class,()->new AuditLogService.Query(null,null,null,null,null,null,0,20));
  assertThrows(IllegalArgumentException.class,()->new AuditLogService.Query(null,null,null,null,null,null,1,101));
  var now=Instant.now();assertThrows(IllegalArgumentException.class,()->new AuditLogService.Query(null,null,null,null,now,now,1,20));
 }
 @Test void actorEmailIsExactValidatedAndTrimmed() {
  var q=new AuditLogService.Query(null,null,null,null,null,null,1,20," Person+qa@Example.com ");
  assertEquals("Person+qa@Example.com",q.actorEmail());
  for(String email:List.of("bad","a b@example.com","a@example.com%".repeat(30)))
   assertThrows(IllegalArgumentException.class,()->new AuditLogService.Query(null,null,null,null,null,null,1,20,email));
 }
 @Test void onlyActiveAdminCanQuery() throws Exception {
  var s=new AuditLogService(new AuditLogDAO(){@Override public AuditLogService.Page search(AuditLogService.Query q){return new AuditLogService.Page(List.of(),0,q.page(),q.size());}});
  var q=new AuditLogService.Query(null,null,null,null,null,null,1,20);
  assertThrows(SecurityException.class,()->s.search(null,q));
  User u=new User();Role r=new Role();r.setRoleName("HR");u.setRole(r);u.setStatus(UserStatus.ACTIVE);
  assertThrows(SecurityException.class,()->s.search(u,q));r.setRoleName("ADMIN");u.setStatus(UserStatus.LOCKED);
  assertThrows(SecurityException.class,()->s.search(u,q));u.setStatus(UserStatus.ACTIVE);assertEquals(0,s.search(u,q).total());
 }
}
