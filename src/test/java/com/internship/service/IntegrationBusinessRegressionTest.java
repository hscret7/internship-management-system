package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationBusinessRegressionTest {
    private User actor(String role) {User u=new User();u.setUserId(11);Role r=new Role();r.setRoleName(role);u.setRole(r);return u;}
    private InternProfile profile(long owner) {
        InternProfile p=new InternProfile();p.setProfileId(22);p.setUserId(owner);p.setFullName("Test Intern");p.setEmail("test@example.test");p.setPhone("0912 345 678");p.setUniversity("ICTU");p.setMajor("IT");p.setStatus("INACTIVE");return p;
    }
    @Test void profileOwnerAndStatusAreProtectedAndNaNRejected() throws Exception {
        InternProfile existing=profile(11);boolean[] saved={false};
        var service=new InternProfileService(new InternProfileDAO(){
            @Override public InternProfile getProfileById(int id){return existing;}
            @Override public boolean updateProfile(InternProfile p){saved[0]=true;return true;}
        });
        assertThrows(IllegalArgumentException.class,()->service.updateProfile(profile(99)));assertFalse(saved[0]);
        InternProfile input=profile(11);input.setStatus("ACTIVE");assertTrue(service.updateProfile(input));assertEquals("INACTIVE",input.getStatus());assertEquals("0912345678",input.getPhone());
        input.setGpa(Double.NaN);assertFalse(service.validateProfile(input).isEmpty());
    }
    @Test void twoShiftsCountEightNotEighteenHours() throws Exception {
        var date=LocalDate.of(2026,10,13);
        var schedules=new WorkScheduleDAO(){@Override public List<WorkSchedule> list(Long owner,Integer e,Integer p,LocalDate from,LocalDate to,Connection c){return List.of(
            new WorkSchedule(1,1,date,LocalTime.of(8,0),LocalTime.of(12,0),"","ACTIVE","Intern","P",false),
            new WorkSchedule(2,1,date,LocalTime.of(13,0),LocalTime.of(17,0),"","ACTIVE","Intern","P",false));}};
        Attendance attendance=new Attendance();attendance.setEnrollmentId(1);attendance.setWorkDate(date);attendance.setCheckInTime(date.atTime(8,0));attendance.setCheckOutTime(date.atTime(17,0));
        var dao=new AttendanceDAO(){@Override public List<Attendance> getAttendanceReport(Long owner,int p,int e,LocalDate from,LocalDate to,Connection c){return List.of(attendance);}};
        var service=new AttendanceService(dao,schedules,this::connection);
        var rows=service.getReport(actor("HR"),null,null,date,date);assertEquals(2,rows.size());assertEquals(4,rows.get(0).workedHours);assertEquals(4,rows.get(1).workedHours);
        assertEquals(8,service.getSummary(actor("HR"),null,null,date,date).totalWorkedHours());
        assertThrows(IllegalArgumentException.class,()->service.getReport(actor("MENTOR"),null,null,date,date));
    }
    @Test void utcClockUsesVietnamBusinessDate() throws Exception {
        LocalDate[] used={null};LocalDateTime[] wall={null};
        AttendanceDAO dao=new AttendanceDAO(){
            @Override public void lockOwnedEnrollment(int e,long u,Connection c){}
            @Override public Attendance findByEnrollmentAndDate(int e,LocalDate d,Connection c){return null;}
            @Override public void checkIn(int e,LocalDate d,Timestamp t,Connection c){used[0]=d;wall[0]=t.toLocalDateTime();}
        };
        WorkScheduleDAO schedules=new WorkScheduleDAO(){@Override public List<WorkSchedule> list(Long u,Integer e,Integer p,LocalDate from,LocalDate to,Connection c){return List.of(new WorkSchedule(1,1,from,LocalTime.of(8,0),LocalTime.of(17,0),"","ACTIVE","Test","Test",false));}};
        var service=new AttendanceService(dao,schedules,this::connection,Clock.fixed(Instant.parse("2026-10-12T18:00:00Z"),ZoneOffset.UTC));
        service.checkIn(actor("INTERN"),1);assertEquals(LocalDate.of(2026,10,13),used[0]);assertEquals(LocalDateTime.of(2026,10,13,1,0),wall[0]);
    }
    private Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->null);}
}
