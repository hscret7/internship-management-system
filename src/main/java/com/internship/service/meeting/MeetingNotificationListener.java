package com.internship.service.meeting;

import com.internship.service.email.SmtpEmailService;
import com.internship.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.Clock;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.logging.Logger;

/** Disabled by default; enabling real sends requires an explicit deployment decision. */
@WebListener
public class MeetingNotificationListener implements ServletContextListener {
    private static final Logger LOG=Logger.getLogger(MeetingNotificationListener.class.getName());
    private ScheduledExecutorService executor;
    @Override public void contextInitialized(ServletContextEvent event) {
        var settings=MeetingNotificationSettings.system();
        if(!settings.send())return;
        // Bound configured SMTP timeouts well below the claim lease; no credential logging.
        validateTimeouts();
        var worker=new MeetingNotificationWorker(DBConnection::getConnection,new SmtpEmailService(),Clock.systemUTC(),settings);
        executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"be1-meeting-mail");t.setDaemon(true);return t;});
        executor.scheduleWithFixedDelay(()->{
            try{worker.runOnce();}catch(Exception e){LOG.warning("Meeting notification poll failed; inspect DB/configuration. No mail details logged.");}
        },0,settings.pollSeconds(),TimeUnit.SECONDS);
    }
    private void validateTimeouts() {
        Properties p=new Properties();
        try(var input=getClass().getClassLoader().getResourceAsStream("mail.properties")) {
            if(input==null)throw new IllegalStateException("Meeting sender requires mail configuration");
            p.load(input);
            for(String key:new String[]{"mail.smtp.connectiontimeout","mail.smtp.timeout","mail.smtp.writetimeout"}) {
                int n=Integer.parseInt(p.getProperty(key,"10000"));
                if(n<1 || n>30000)throw new IllegalArgumentException("Meeting SMTP timeouts must be 1..30000ms");
            }
        }catch(java.io.IOException e){throw new IllegalStateException("Unable to validate meeting SMTP configuration");}
    }
    @Override public void contextDestroyed(ServletContextEvent event) {
        if(executor!=null) {
            executor.shutdownNow();
            try{if(!executor.awaitTermination(35,TimeUnit.SECONDS))LOG.warning("Meeting worker has not terminated; delivery lease will expire.");}
            catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
    }
}
