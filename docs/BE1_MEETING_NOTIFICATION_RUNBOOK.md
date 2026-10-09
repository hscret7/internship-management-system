# Task 35.2 — triển khai và kiểm tra

Trạng thái: đã merge PR #54 vào develop sau PR #53. Build/JDBC sau merge và smoke SMTP qua Mailpit đã đạt; FE/BE3 acceptance, SMTP internet có TLS/auth và kiểm thử tải/lifecycle vẫn chưa xác nhận.

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

## Quy trình deploy local
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
- Test concurrency được sửa để cấm hai claim thành công, kiểm tra claim được sau khi hai transaction đóng nếu cả hai trả về rỗng, attempts=1, đúng một IN_FLIGHT, không claim lại lease đang hoạt động và ack đúng token chỉ một lần. SKIP LOCKED có thể trả về rỗng khi có contention; một poll không bảo đảm tiến triển. Chưa xác nhận nguyên nhân của lần fail cũ; bộ test sửa đã chạy lại và PASS trên WSL, xem bằng chứng mới dưới đây.
- Java21 compile test mới và42 test không cần DB: PASS. Không chạy được MySQL tại môi trường review do giới hạn socket.
- Các ghi nhận compile/unit phía trên là bằng chứng lịch sử trước merge; bằng chứng mới được tách bên dưới.


## Bằng chứng cập nhật — 2026-10-09
- PR #53 merge commit 016f6f4; PR #54 merge commit 376adfe. Người dùng đã phê duyệt từng merge.
- WSL develop 376adfe: mvn clean package PASS, 266 tests / 240 pass / 26 skipped / 0 failures/errors. Skipped gồm 1 BE1 DAO JDBC, 2 BE3 MySQL, 11 BE4 MySQL, 12 notification JDBC do thiếu opt-in properties.
- Chạy riêng script disposable MySQL 8.0.46 tại 376adfe: 54/54 PASS, 0 skipped, gồm 12 notification JDBC. Không coi 13 integration tests BE3/BE4 là đã pass.
- Tomcat test 18082 dùng WAR ce3f003 đã build trước merge: default-disabled login/read; enqueue-only create/update/cancel/repeated cancel đều đạt. Snapshot giữ email người bị loại, CHANGE gửi union người cũ/mới; reminder cũ SUPERSEDED.
- SMTP loopback Mailpit: CREATE/CHANGE/CANCEL được capture; cuộc họp 2 CREATE/REMINDER SENT attempts=1. Pause Mailpit khi tạo cuộc họp 3: CREATE PENDING attempts=2 EMAIL_FAILURE; unpause -> SENT attempts=3, lỗi xóa. Reminder cuộc họp 3 đến hạn -> SENT attempts=1.
- DB cuối: 8 SENT, 2 SUPERSEDED. SMTP runtime này là ce3f003, không phải triển khai lại WAR post-merge; không có thử internet/TLS/auth/load.
- Người dùng lưu SQL, outbox, Mailpit message listing, Surefire (54-test run), commit và log trong thư mục bằng chứng local rồi dừng đúng Tomcat test và hai container có label. Cổng 18082/18025/11025/13318 không còn listener. Không redeploy Tomcat 8082 hoặc migrate DB chung.
- Chi tiết bằng chứng: https://github.com/hscret7/internship-management-system/pull/53 và https://github.com/hscret7/internship-management-system/pull/54.
