package com.internship.service.meeting;

/** Explicit opt-in JVM settings; no SMTP initialization or runtime schema changes. */
public record MeetingNotificationSettings(boolean enqueue, boolean send, int reminderMinutes,
                                           int maxAttempts, int leaseSeconds, int pollSeconds) {
    public MeetingNotificationSettings {
        if(send && !enqueue) throw new IllegalArgumentException("Sending requires enqueue enabled");
        if(reminderMinutes<1 || reminderMinutes>10080 || maxAttempts<1 || maxAttempts>20
                || leaseSeconds<120 || leaseSeconds>3600 || pollSeconds<1 || pollSeconds>300)
            throw new IllegalArgumentException("Invalid meeting notification settings");
    }
    public static MeetingNotificationSettings system() {
        return from(System.getProperties());
    }
    public static MeetingNotificationSettings from(java.util.Properties p) {
        return new MeetingNotificationSettings(flag(p,"enqueue.enabled"),flag(p,"send.enabled"),
                number(p,"reminder.minutes",30),number(p,"max.attempts",5),
                number(p,"lease.seconds",600),number(p,"poll.seconds",10));
    }
    private static boolean flag(java.util.Properties p,String key) {
        String value=p.getProperty("meeting.notifications."+key,"false");
        if(!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Invalid boolean setting");
        return Boolean.parseBoolean(value);
    }
    private static int number(java.util.Properties p,String key,int fallback) {
        return Integer.parseInt(p.getProperty("meeting.notifications."+key,String.valueOf(fallback)));
    }
}
