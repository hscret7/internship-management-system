package com.internship.service.meeting;

import com.internship.model.Meeting;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.AddressException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** One recipient and plain text, never a client-provided address or attendee email list. */
public final class MeetingMail {
    private MeetingMail() { }
    public static String mailbox(String email) {
        if(email==null || email.isBlank() || email.length()>255 || email.indexOf('\r')>=0 || email.indexOf('\n')>=0)
            throw new IllegalArgumentException("Invalid meeting recipient email");
        try {
            InternetAddress[] parsed=InternetAddress.parse(email.trim(),true);
            if(parsed.length!=1 || parsed[0].isGroup() || parsed[0].getPersonal()!=null)
                throw new IllegalArgumentException("Invalid meeting recipient email");
            parsed[0].validate();
            if(!parsed[0].getAddress().equals(email.trim()) || !email.contains("@"))
                throw new IllegalArgumentException("Invalid meeting recipient email");
            return parsed[0].getAddress();
        } catch(AddressException e) {throw new IllegalArgumentException("Invalid meeting recipient email");}
    }
    public record Text(String subject,String body) { }
    public static Text render(String event,Meeting m,boolean removed,boolean added) {
        String label=switch(event) {
            case "CREATE" -> "Lời mời cuộc họp";
            case "CHANGE" -> removed?"Đã kết thúc tham gia cuộc họp":added?"Lời mời cuộc họp":"Thay đổi cuộc họp";
            case "CANCEL" -> "Hủy cuộc họp";
            case "REMINDER" -> "Nhắc lịch cuộc họp";
            default -> throw new IllegalArgumentException("Invalid notification event");
        };
        String title=m.title().replace('\r',' ').replace('\n',' ');
        String subject=label+": "+title;
        if(subject.length()>255)subject=subject.substring(0,255);
        DateTimeFormatter fmt=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm XXX").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        String body=label+"\nMã cuộc họp: "+m.id()+"\nPhiên bản: "+m.revision()+"\nTiêu đề: "+m.title()
                +"\nBắt đầu: "+fmt.format(m.startsAt())+"\nKết thúc: "+fmt.format(m.endsAt())
                +"\nĐịa điểm: "+m.location()+"\n"
                +(removed?"Bạn không còn trong danh sách tham gia cuộc họp này.\n":"")
                +(m.description()==null?"":"Nội dung: "+m.description()+"\n");
        return new Text(subject,body);
    }
}
