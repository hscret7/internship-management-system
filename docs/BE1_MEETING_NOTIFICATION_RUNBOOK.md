# Task 35.2 — triển khai và kiểm tra

Trạng thái: implementation nháp, chưa đủ điều kiện merge cho đến khi JDBC/MySQL và Tomcat được kiểm tra. Nhánh notifications dựa trên35.1, PR nhắm vào nhánh35.1 để diff chỉ chứa35.2. Không sửa BE2/BE3/BE4, FE, auth/filter, EmailService/SmtpEmailService hay pom.xml.

## Những gì đã thêm
- Migration thủ công tạo meeting_recipient_snapshots và meeting_notification_outbox, FK giữ dữ liệu, unique meeting/revision/event/user chống trùng bản ghi.
- MeetingService gọi hook outbox trên cùng connection trước commit; khi tắt enqueue hook không ghi bảng mới. Capture người nhận trước thay participant; email đã lưu của người bị loại được giữ nguyên. Cuộc họp cũ chưa có snapshot dùng users.email hiện tại trước lần thay đổi đầu tiên.
- CREATE/CHANGE/CANCEL và REMINDER, mặc định nhắc30 phút; trong30 phút thì nhắc ngay, sau giờ bắt đầu không gửi nhắc. Subject/body là text UTF-8, giờ hiển thị Asia/Ho_Chi_Minh. Mỗi email chỉ có một người nhận.
- Claim từng việc bằng FOR UPDATE SKIP LOCKED, transaction READ_COMMITTED; commit/đóng connection trước SMTP. Claim token/lease kiểm tra khi xác nhận. Events cùng meeting/user có thứ tự; reminder cũ bị SUPERSEDED khi đổi/hủy. Worker lease hết hạn có thể nhận lại.
- Retry tối đa5 lần, backoff30/60/120/240 giây… tối đa3600; lưu mã lỗi ngắn thay raw SMTP exception. EmailException được retry có giới hạn (bao gồm một số lỗi SMTP cố định vì interface cũ không phân loại chi tiết). Lỗi input là FAILED.
- Listener dừng executor khi undeploy. Không tạo endpoint gửi mail mới và không có runtime DDL.

## Cấu hình (JVM -D, mặc định tắt)
meeting.notifications.enqueue.enabled=false
meeting.notifications.send.enabled=false
meeting.notifications.reminder.minutes=30
meeting.notifications.max.attempts=5
meeting.notifications.lease.seconds=600
meeting.notifications.poll.seconds=10

Chỉ sau migration mới bật enqueue. send=true bắt buộc enqueue=true và mail.properties hợp lệ; SMTP timeouts phải1..30000ms. Không bật send khi test trên hệ thống thực. Thiếu mail.properties không ảnh hưởng listener khi send=false. Không ghi mật khẩu vào git/PR/log.

## Test MySQL dùng riêng
Từ repository, chạy:

```bash
bash scripts/test-be1-meeting-notifications.sh
```

Script cần Docker và Maven/Java21. Tạo container MySQL8.0.46 riêng, port chỉ127.0.0.1:13318, không volume, DB be1_notification_test; chỉ container mới đó bị dọn khi script thoát. Nếu13318 bị chiếm, docker run thất bại; script không dừng container khác. Không dùng internship_mysql, không đọc db.properties.

JDBC test chỉ chấp nhận URL cố định jdbc:mysql://127.0.0.1:13318/be1_notification_test?connectionTimeZone=UTC và root không mật khẩu của container tạm. Nó xóa/tạo fixture trong DB test này. Không chạy opt-in này với DB khác. Server log được lưu target/be1-notification-mysql.log. Surefire XML cung cấp kết quả test.

## Deploy local — chưa thực hiện
1. Đọc và áp dụng sql/schema_be1_meeting_notifications.sql thủ công sau schema35.1 vào DB local đã sao lưu. Đừng áp dụng vào DB nhóm.
2. Full mvn clean package và bộ JDBC riêng pass.
3. Triển khai WAR vào Tomcat test riêng; mặc định enqueue/send vẫnfalse, smoke regressions35.1.
4. Bật enqueue=true, send=false trong JVM của Tomcat test; tạo/sửa/hủy cuộc họp thử, kiểm tra outbox/snapshot và không có SMTP. Cần migration trước bước này.
5. Kiểm thử gửi chỉ qua injected fake sender hoặc môi trường mail capture; bật SMTP thật chỉ sau khi người dùng cho phép. Chưa có UI quản trị queue/retry thủ công.

## Giới hạn cần biết
- Không hứa exactly-once SMTP: crash sau gửi trước ack hoặc lease hết có thể gửi trùng; unique queue không ngăn mọi email trùng.
- Cuộc họp có thể đổi trong lúc SMTP đang gửi, một nhắc cũ đang in-flight vẫn có thể đến; bản ghi cũ không được ack khi token bị hủy.
- SMTP timeout không chặn tuyệt đối mọi độ trễ DNS/provider. Lease giảm rủi ro, không tạo bảo đảm exactly-once.
- Worker xử lý một mail mỗi poll để không làm lease của batch hết hạn; throughput mặc định6 mail/phút. Có thể giảm poll, nhưng tải lớn cần thiết kế bổ sung.
- Sau retry hết, FAILED không tự gửi lại và không có UI; admin cần xem trạng thái DB. Không ghi nhận FAILED là SENT.
- Notification recipients chỉ là Intern participants, chưa copy organizer/Mentor. Địa chỉ sai khiến thao tác có enqueue bị rollback (kể cả hủy nếu phải capture địa chỉ legacy sai); cần sửa dữ liệu email trước khi thực hiện lại.
- Tắt enqueue nghĩa là cuộc họp không sinh sự kiện; bật lại không tự backfill/remind mọi cuộc họp cũ. Lần thay đổi tiếp theo mới capture người nhận legacy.
- Các lần sửa/hủy có thể contend với worker và các thao tác lifecycle; cần kiểm thử MySQL đồng thời và Tomcat, không coi unit tests là bằng chứng đã hoàn thành.

## Bằng chứng trong môi trường review
- Java21 compile: PASS.
-42 test unit/service/servlet/transaction hooks: PASS,0 skipped/failed.
- WSL/MySQL8.0.46 tại commit6a7bc69:54 tests,53 pass,1 fail,0 errors/skipped. Ca twoWorkersCannotClaimSameEventConcurrently thất bại ở assertion yêu cầu đúng một claim thành công trong một lần poll. Log cũ chưa phân biệt hai claim rỗng với hai claim thành công.
- Test concurrency được sửa để cấm hai claim thành công, kiểm tra claim được sau khi hai transaction đóng nếu cả hai trả về rỗng, attempts=1, đúng một IN_FLIGHT, không claim lại lease đang hoạt động và ack đúng token chỉ một lần. SKIP LOCKED có thể trả về rỗng khi có contention; một poll không bảo đảm tiến triển. Chưa xác nhận nguyên nhân của lần fail cũ; cần chạy lại script trên WSL trước khi coi JDBC là PASS.
- Java21 compile test mới và42 test không cần DB: PASS. Không chạy được MySQL tại môi trường review do giới hạn socket.
- Không gửi email, không đổi DB local của người dùng, không merge. Full Maven/Tomcat cho35.2 chưa có bằng chứng.
