package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import com.internship.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.ResourceLock;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** All writes are in a UUID disposable database. Never sends real email. */
@EnabledIfSystemProperty(named="integration.mysql",matches="true")
@ResourceLock("DBConnection.url")
@ResourceLock("java.util.TimeZone.default")
class IntegrationReadinessMySqlTest {
    static String database, originalUrl, adminUrl; static Field urlField; static boolean created;
    static Properties config;
    @BeforeAll static void prepare() throws Exception {
        config=new Properties();try(var in=DBConnection.class.getClassLoader().getResourceAsStream("db.properties")){
            assertNotNull(in);config.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));
        }
        urlField=DBConnection.class.getDeclaredField("url");urlField.setAccessible(true);originalUrl=(String)urlField.get(null);
        var m=java.util.regex.Pattern.compile("^(jdbc:mysql://[^/]+/)[^?]+(.*)$").matcher(originalUrl);assertTrue(m.matches());
        adminUrl=m.group(1)+m.group(2);database="ims_readiness_test_"+UUID.randomUUID().toString().replace("-","");
        try(var c=admin();var s=c.createStatement()){s.execute("CREATE DATABASE `"+database+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");created=true;}
        urlField.set(null,m.group(1)+database+m.group(2));
        try(var c=DBConnection.getConnection();var s=c.createStatement()){
            for(String file:List.of("database/schema.sql","sql/schema_be2.sql","sql/schema_be3.sql","sql/schema_be4.sql","sql/schema_be3_schedule_leave.sql","sql/schema_be2_sprint3.sql","sql/schema_be4_sprint3.sql")){
                String sql=Files.readString(Path.of(file),StandardCharsets.UTF_8).replace("internship_management",database).replaceAll("(?m)^\\s*--.*$","");
                for(String command:sql.split(";"))if(!command.isBlank())s.execute(command);
            }
            for(int id=1;id<=5;id++){
                String role=id==1?"HR":id==5?"MENTOR":"INTERN";
                s.executeUpdate("INSERT INTO users(user_id,username,email,password_hash,full_name,role_id) SELECT "+id+",'fixture"+id+"','fixture"+id+"@example.invalid','not-a-login-hash','Fixture "+id+"',role_id FROM roles WHERE role_name='"+role+"'");
            }
            for(int id=2;id<=4;id++)s.executeUpdate("INSERT INTO intern_profiles(profile_id,user_id,full_name,phone,email,university,major) VALUES("+id+","+id+",'Fixture Intern','0912345678','fixture"+id+"@example.invalid','ICTU','IT')");
            s.executeUpdate("INSERT INTO applications(application_id,intern_id,status) VALUES(101,2,'APPROVED'),(102,3,'APPROVED')");
            s.executeUpdate("INSERT INTO departments(department_id,department_name) VALUES(1,'Fixture Dept')");
            s.executeUpdate("INSERT INTO internship_programs(program_id,program_name,department_id,start_date,end_date,status) VALUES(1,'Fixture Program',1,'2026-10-01','2026-10-31','ACTIVE')");
            s.executeUpdate("INSERT INTO program_enrollments(enrollment_id,application_id,program_id,intern_profile_id) VALUES(1,101,1,2),(2,102,1,3)");
            s.executeUpdate("INSERT INTO mentors(mentor_id,user_id,department_id) VALUES(1,5,1)");
            s.executeUpdate("INSERT INTO mentor_assignments(enrollment_id,mentor_id,assigned_by) VALUES(1,1,1)");
        }
    }
    static Connection admin() throws SQLException{return DriverManager.getConnection(adminUrl,config.getProperty("db.username"),config.getProperty("db.password"));}
    @AfterAll static void cleanup() throws Exception {
        if(urlField!=null&&originalUrl!=null)urlField.set(null,originalUrl);
        if(created){assertTrue(database.matches("ims_readiness_test_[0-9a-f]{32}"));try(var c=admin();var s=c.createStatement()){s.execute("DROP DATABASE `"+database+"`");created=false;}}
    }
    User user(long id) throws SQLException{return new UserDAO().findById(id);}
    @Test void submittedApplicationWaitsForHrAndCannotEnrollBeforeApproval() throws Exception {
        var app=new ApplicationService((recipient,subject,body)->{});
        int id=app.submitApplication(4);assertEquals("PENDING",app.getApplicationById(id).getStatus());
        assertTrue(app.getPendingApplications().stream().anyMatch(a->a.getApplicationId()==id));
        assertThrows(IllegalStateException.class,()->new EnrollmentService().enrollIntern(id,1,1));
        try(var c=DBConnection.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM program_enrollments WHERE intern_profile_id=4")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
        app.approveApplication(id,1);assertEquals("APPROVED",app.getApplicationById(id).getStatus());
        assertThrows(IllegalStateException.class,()->app.approveApplication(id,1));
    }
    @Test void profileSqlOwnerGuardStatusAndEmailFilters() throws Exception {
        InternProfileDAO dao=new InternProfileDAO();var other=dao.getProfileById(3);other.setUserId(2);other.setFullName("Tampered");assertFalse(dao.updateProfile(other));
        assertNotEquals("Tampered",dao.getProfileById(3).getFullName());
        dao.updateStatus(2,"INACTIVE");var mine=dao.getProfileById(2);mine.setStatus("ACTIVE");assertTrue(new InternProfileService(dao).updateProfile(mine));assertEquals("INACTIVE",dao.getProfileById(2).getStatus());
        assertEquals(1,dao.searchProfiles("fixture2@example.invalid",null,null,1,"INACTIVE").size());
        dao.updateStatus(2,"ACTIVE");
    }
    @Test void documentsHaveOneDecisionAndContractConfirmSurvivesConcurrentReplacement() throws Exception {
        DocumentDAO docs=new DocumentDAO();Document d=new Document();d.setProfileId(2);d.setDocumentType("CV");d.setFileName("qa.pdf");d.setFilePath("documents/qa.pdf");d.setFileSize(1);d.setMimeType("application/pdf");int id=docs.addDocument(d);
        assertTrue(docs.approveDocument(id,1));assertFalse(docs.rejectDocument(id,1,"late"));assertEquals("APPROVED",docs.getDocumentById(id).getStatus());
        ContractDAO contracts=new ContractDAO();int contractId=contracts.saveOrUpdateContract(contract("first.pdf"));
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            CountDownLatch gate=new CountDownLatch(1);
            var confirm=pool.submit(()->{gate.await();return contracts.confirmContract(contractId);});
            var replace=pool.submit(()->{gate.await();try{return contracts.saveOrUpdateContract(contract("replace.pdf"));}catch(IllegalStateException expected){return -1;}});
            gate.countDown();assertTrue(confirm.get(15,TimeUnit.SECONDS));replace.get(15,TimeUnit.SECONDS);
            Contract fresh=contracts.getContractById(contractId);assertEquals("CONFIRMED",fresh.getStatus());assertNotNull(fresh.getConfirmedAt());
            Instant first=fresh.getConfirmedAt().toInstant();assertFalse(contracts.confirmContract(contractId));assertEquals(first,contracts.getContractById(contractId).getConfirmedAt().toInstant());
            assertThrows(IllegalStateException.class,()->contracts.saveOrUpdateContract(contract("after.pdf")));
        }finally{pool.shutdownNow();}
    }
    Contract contract(String name){Contract c=new Contract();c.setApplicationId(101);c.setFileName(name);c.setFilePath("contracts/"+name);c.setFileSize(1);c.setMimeType("application/pdf");c.setUploadedBy(1);return c;}
    @Test void staleContractFormCannotConfirmAReplacement() throws Exception {
        ContractDAO dao=new ContractDAO();Contract c=contract("v1.pdf");c.setApplicationId(102);int id=dao.saveOrUpdateContract(c);
        String oldVersion=dao.getContractById(id).getFileVersion();String oldPath=dao.getContractById(id).getFilePath();
        c=contract("v2.pdf");c.setApplicationId(102);dao.saveOrUpdateContract(c);
        assertFalse(dao.confirmContract(id,oldPath));
        DocumentService service=new DocumentService();
        assertThrows(IllegalArgumentException.class,()->service.confirmContract(id,user(3),oldVersion));
        assertEquals("PENDING_CONFIRMATION",dao.getContractById(id).getStatus());
        assertTrue(service.confirmContract(id,user(3),dao.getContractById(id).getFileVersion()));
    }
    @Test void allowanceAndSupportValidateInputsAndKeepTerminalDecisions() throws Exception {
        AllowanceService allowances=new AllowanceService();Allowance input=new Allowance();input.setEnrollmentId(1);input.setAllowanceType("MEAL");input.setPeriod("2026-10");input.setAmount(new java.math.BigDecimal("1000"));
        input.setPeriod("2026-13");assertThrows(IllegalArgumentException.class,()->allowances.createAllowance(user(1),input));input.setPeriod("2026-10");
        allowances.createAllowance(user(1),input);Allowance saved=allowances.getAllowances(user(2),1).get(0);
        assertThrows(IllegalArgumentException.class,()->allowances.updateStatus(user(1),saved.getAllowanceId(),"INVALID"));
        allowances.updateStatus(user(1),saved.getAllowanceId(),"PAID");LocalDateTime paid=allowances.getAllowances(user(2),1).get(0).getPaidAt();
        assertThrows(IllegalArgumentException.class,()->allowances.updateStatus(user(1),saved.getAllowanceId(),"PAID"));assertEquals(paid,allowances.getAllowances(user(2),1).get(0).getPaidAt());
        SupportRequestService support=new SupportRequestService();assertThrows(IllegalArgumentException.class,()->support.createRequest(user(2),"OTHER"," ","Test"));
        support.createRequest(user(2),"HR_INQUIRY","Test request","Test content");int id=support.getMyRequests(user(2)).get(0).getRequestId();
        support.updateResponse(user(1),id,"RESOLVED","Test resolved");assertThrows(IllegalArgumentException.class,()->support.updateResponse(user(1),id,"REJECTED","Late response"));
        assertEquals("RESOLVED",support.getMyRequests(user(2)).get(0).getStatus());
    }
    @Test void concurrentCheckInsInsertOneRowAndCheckoutIsIdempotentGuarded() throws Exception {
        var date=LocalDate.of(2026,10,20);try(var c=DBConnection.getConnection();var s=c.createStatement()){
            s.executeUpdate("INSERT INTO work_schedules(enrollment_id,work_date,start_time,end_time,created_by,updated_by) VALUES(2,'2026-10-20','08:00','17:00',1,1)");
        }
        var actor=user(3);var service=new AttendanceService(new AttendanceDAO(),new WorkScheduleDAO(),DBConnection::getConnection,Clock.fixed(date.atTime(8,0).toInstant(ZoneOffset.ofHours(7)),ZoneOffset.UTC));
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            CountDownLatch gate=new CountDownLatch(1);Callable<Boolean> task=()->{gate.await();try{service.checkIn(actor,2);return true;}catch(IllegalArgumentException expected){return false;}};
            var a=pool.submit(task);var b=pool.submit(task);gate.countDown();assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            try(var c=DBConnection.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM attendances WHERE enrollment_id=2 AND work_date='2026-10-20'")){assertTrue(r.next());assertEquals(1,r.getInt(1));}
            service.checkOut(actor,2);assertThrows(IllegalArgumentException.class,()->service.checkOut(actor,2));
        }finally{pool.shutdownNow();}
    }
    @Test void be4AndLeaveMetadataKeepSameInstantAcrossJdbcSessionJvmZones() throws Exception {
        try(var c=DBConnection.getConnection();var s=c.createStatement()){
            s.execute("SET time_zone='+00:00'");
            s.executeUpdate("INSERT INTO weekly_reports(report_id,enrollment_id,week_number,content,status,submitted_at,created_at,updated_at) VALUES(100,1,1,'Test','SUBMITTED','2026-10-12 02:00:00','2026-10-12 02:00:00','2026-10-12 02:00:00')");
            s.executeUpdate("INSERT INTO leave_requests(leave_id,enrollment_id,from_date,to_date,reason,created_at) VALUES(100,1,'2026-10-21','2026-10-21','Test','2026-10-12 02:00:00')");
            s.executeUpdate("INSERT INTO tasks(task_id,title,description,created_by_user_id) VALUES(100,'Test','Test',5)");
            s.executeUpdate("INSERT INTO task_assignments(assignment_id,task_id,enrollment_id,assigned_at,updated_at) VALUES(100,100,1,'2026-10-12 02:00:00','2026-10-12 02:00:00')");
            s.executeUpdate("INSERT INTO task_progress(progress_id,assignment_id,completion_percentage,note,created_at) VALUES(100,100,10,'Test','2026-10-12 02:00:00')");
            s.executeUpdate("INSERT INTO report_feedbacks(feedback_id,report_id,reviewer_user_id,comment,created_at) VALUES(100,100,5,'Test','2026-10-12 02:00:00')");
            s.executeUpdate("INSERT INTO evaluations(evaluation_id,enrollment_id,mentor_id,score,comment,created_at,updated_at) VALUES(100,1,1,80,'Test','2026-10-12 02:00:00','2026-10-12 02:00:00')");
        }
        TimeZone old=TimeZone.getDefault();
        try{
            for(String jvm:List.of("UTC","GMT+07:00")){TimeZone.setDefault(TimeZone.getTimeZone(jvm));for(String zone:List.of("+00:00","+07:00"))try(var c=DBConnection.getConnection();var s=c.createStatement()){
                s.execute("SET time_zone='"+zone+"'");
                var report=new WeeklyReportDAO().findById(c,100);assertEquals(LocalDateTime.of(2026,10,12,2,0),report.getSubmittedAt());
                var leave=new LeaveRequestDAO().find(100,c);assertEquals(Instant.parse("2026-10-12T02:00:00Z"),leave.createdAt());assertNull(leave.reviewedAt());
                LocalDateTime expected=LocalDateTime.of(2026,10,12,2,0);
                assertEquals(expected,new TaskAssignmentDAO().findById(c,100).getAssignedAt());
                assertEquals(expected,new TaskProgressDAO().findByAssignment(c,100).get(0).getCreatedAt());
                assertEquals(expected,new ReportFeedbackDAO().findById(c,100).getCreatedAt());
                assertEquals(expected,new EvaluationDAO().findByEnrollment(c,1).getCreatedAt());
                assertEquals(expected,new EvaluationDAO().findAllSummaries(c).get(0).createdAt());
            }}
        }finally{TimeZone.setDefault(old);}
    }
}
