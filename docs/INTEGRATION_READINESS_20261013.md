# Integration bugfix / chuẩn bị demo — 13/10/2026

## Phạm vi và trạng thái

Sửa các lỗi đã xác định trong audit BE1–BE4 và FE trên baseline develop `102c8f8`,
giữ bản sửa Application timezone của PR #77. Đây là nhánh integration bugfix;
không sửa trực tiếp main/develop và không thay lịch sử tác giả các feature cũ.
Không coi kiểm thử bỏ qua là đạt; không khẳng định hệ thống không còn mọi lỗi có thể xảy ra.

## Những gì đã sửa

| Nhóm | Kết quả |
|---|---|
| BE1 / bảo mật | Phân quyền dùng path mà container đã ánh xạ; endpoint tạo tài khoản có guard ADMIN ACTIVE. Login kiểm tra token CSRF. Mật khẩu mới giới hạn 72 byte UTF-8 để tránh bcrypt cắt suffix. |
| BE2 / hồ sơ | Kiểm tra owner trong service và SQL; TTS không tự đổi INACTIVE thành ACTIVE. Không bỏ qua ngày/GPA sai; chặn NaN, giới hạn trường và chuẩn hóa điện thoại. Lọc tên/email, chương trình thật, trạng thái hồ sơ; giữ giá trị bộ lọc. |
| BE2 / tài liệu | CSRF filter phủ các alias POST của profile/document/contract; các form HR có token. File mới nằm ngoài WAR, tải qua endpoint kiểm tra owner, chặn static uploads. Đường file legacy chỉ đọc qua endpoint đã kiểm tra quyền. |
| BE2 / quyết định | Document chỉ PENDING -> APPROVED/REJECTED, request stale trả conflict. Hợp đồng upload khóa application/contract trong transaction; bản CONFIRMED không ghi đè. Xác nhận có fileVersion và SQL guard file_path để trang cũ không xác nhận nhầm file thay mới; không ghi lại mốc xác nhận khi lặp. |
| BE2 / chấm công | Clock/ngày theo Việt Nam; DATETIME wall time đọc/ghi không qua JVM-default instant conversion. Khóa parent chống check-in đồng thời, checkout có guard. Unique enrollment/date cho schema mới và migration không tự xóa bản ghi trùng. |
| BE2 / phụ cấp, hỗ trợ | Validate loại, kỳ, số tiền, trường bắt buộc, trạng thái/transition. Không ghi lại paid_at hay mở lại yêu cầu đã kết thúc. Hiển thị lỗi support sau redirect; không trả lỗi SQL chi tiết cho người dùng. |
| BE3 / thời gian và ứng tuyển | Giữ epoch projection và ISO/null contract của PR #77. Leave metadata dùng cùng quy tắc epoch. Luồng mới vẫn PENDING, HR phải duyệt, sau đó ghi danh riêng. Sửa nhãn profile ACTIVE thành “Hồ sơ hiệu lực”, không gọi nhầm là “Đang thực tập”. |
| BE4 / báo cáo | Metadata TIMESTAMP của report/progress/feedback/assignment/evaluation đọc từ SQL epoch; JSON trả ISO Instant có Z. Nháp mới dùng contract Sprint 3 và validate tuần, giữ khả năng đọc/sửa nháp legacy. |
| DB chung | BE1/BE2/BE3/BE4 cùng cấu hình DBConnection; lỗi kết nối không fallback sang DB/root defaults khác. Môi trường dùng DB_URL hoặc DB_NAME phải cung cấp DB_USER/DB_PASS rõ ràng. |
| FE | Sửa 3 JSP HR compile fail và JS async/duplicate declaration; sửa body.data của leave list, bulk validation không bắt ID cá nhân. Meeting nhập/hiện theo Việt Nam độc lập timezone browser. Có màn lịch/nghỉ TTS, link menu thật, first-contract form và link tải hợp đồng HR. Bộ lọc mặc định HR gồm tương lai để không giấu ca/đơn vừa tạo. |

## Quy tắc nghiệp vụ / compatibility cần biết

- Lưu hồ sơ và nộp đơn là hai thao tác khác nhau. Profile ACTIVE chỉ là hồ sơ có hiệu lực.
  Không tự duyệt đơn, không tự ghi danh khi lưu profile hoặc khi HR duyệt.
- Giờ công trong báo cáo là phần check-in/check-out giao với ca làm được giao;
  ca 08–12 và 13–17, có mặt 08–17 => 4 + 4 = 8 giờ theo ca, không phải 18 giờ.
  HR/QA cần xác nhận quy tắc giờ theo ca này phù hợp quy định nhóm; nếu tính cả nghỉ trưa,
  cần đổi contract ngày/ca cùng backend/export, không sửa bằng phép chia trên FE.
- Attendance report service hiện chỉ nhận HR/Admin hoặc chính INTERN. Không mở MENTOR
  đọc toàn bộ khi chưa có owner/mentees filter; các HTTP route hiện vẫn HR/INTERN.
- LocalDateTime getter cũ của các model BE4 được giữ tương thích source, nhưng mapper
  metadata giờ trả **UTC components**, được encoder chuyển thành Instant/Z. DATE/TIME của
  lịch, hạn task và DATETIME wall-clock chấm công không được áp dụng quy tắc này.
- POST tạo nháp báo cáo mới phải có results/difficulties/nextPlan. Draft có thể để trống
  nội dung để sửa tiếp; submit mới yêu cầu đủ nội dung. Content đơn trường chỉ giữ cho nháp cũ.
- Contract confirm form gửi fileVersion cùng contractId và csrfToken. Form cũ cần tải lại.
- Password >72 byte bị từ chối ở hash/login, không đổi hash DB. Tài khoản legacy thực sự
  đã dùng password quá giới hạn cần admin hỗ trợ cấp lại mật khẩu hợp lệ; không âm thầm
  truncate hoặc chuyển toàn bộ hash. Không bổ sung password-reset module ngoài backlog.
- File thay thế/failed-save được giữ ở private storage để tránh xóa file của một commit
  chưa xác định kết quả khi kết nối lỗi. Orphan cleanup phải là thao tác quản trị có đối chiếu.

## Kiểm chứng trên bản cuối

- Maven full build với MySQL opt-in: **564 detected, 489 passed, 75 skipped, 0 fail/error**.
  75 skipped là các kiểm thử/môi trường khác chưa bật; không tính là pass.
- 17 test MySQL thật trong tổng số trên: Application timezone (8), BE3 integration (2),
  integration readiness (7). Database UUID riêng; được dọn sau test. Không gửi email thật.
- Compile toàn bộ **64 JSP** bằng Jasper: **0 errors**; không chỉ dựa vào WAR build.
- JavaScript syntax và regression timezone Việt Nam / bulk / CSRF / first-contract: đạt.
- Native Tomcat authorization probe: 3 path chuẩn/encoded/matrix đều 403 và 0 lần tạo ADMIN.
- **67 kiểm tra HTTP** trên Tomcat và DB fixture riêng: các trang 5 role, pending/approve/reject,
  không tự ghi danh, IDOR/CSRF, private upload/download, document conflict, first contract/confirm,
  leave, task/progress, report/feedback/assessment, xuất end-term/attendance/statistics Excel/PDF: đạt.
- Kiểm tra trình duyệt: dashboard hiển thị số liệu; leave list hiển thị row sau lọc.
  Qua kiểm tra này sửa thêm mặc định lọc tương lai, menu HR còn thiếu và tương phản
  dark mode: Bootstrap text-muted/table/bg-light dùng cùng theme với app shell.

## Database và triển khai

1. Sao lưu DB và dừng writer trước DDL; không chạy toàn bộ migration tự động lúc startup.
2. Áp dụng schema schedule/leave, lifecycle, BE2 Sprint3, BE4 Sprint3/export,
   BE1 audit/meetings/outbox/inapp theo đúng dependency.
3. Chạy `sql/migrate_attendance_unique.sql`: xem duplicate report trước; ALTER UNIQUE sẽ
   fail nếu còn trùng. Không tự DELETE/dedupe các hàng đó.
4. Backup/copy file upload legacy sang private directory trước khi thay WAR, giữ bản gốc;
   không trông chờ uploads trong exploded WAR sống qua redeploy. Chỉ giải quyết path qua
   download endpoint có owner check. Có thể cấu hình INTERNSHIP_UPLOAD_DIR bên ngoài WAR.
5. Build/deploy trên môi trường test, chạy regression từng role và review PR trước merge develop.

Trên máy đang triển khai: database đã được backup, bổ sung các schema còn thiếu và đối chiếu
fingerprint toàn bộ hàng/cột cũ trước/sau, xác nhận không thay đổi dữ liệu cũ. Không sửa
submitted_at/reviewed_at, không cộng cứng giờ, không xóa hồ sơ/tài liệu/hợp đồng.
Backup và credential chỉ nằm local, không thuộc Git.

Lệnh kiểm tra từ repository (không chạy các MySQL opt-in song song):

```text
mvn clean package -Dbe3.mysql=true -Dintegration.mysql=true
node src/test/frontend/integration-regression.cjs
```

MySQL tests yêu cầu db.properties local và quyền tạo/drop DB fixture; chỉ drop tên UUID
đã tạo bởi test. Kiểm tra JSP cần Tomcat/Jasper và Ant, đầu ra phải có `0 errors`.

## Chưa nghiệm thu / ngoài khả năng xác nhận trong lượt này

- Gửi/nhận SMTP với hộp thư đã thống nhất. Bản thử tắt SMTP thật; đã kiểm tra quyết định
  vẫn lưu khi email bị tắt/lỗi, không coi đó là bằng chứng inbox nhận thư.
- US38 QR/thẻ: chờ thiết bị/API và đầu mối, không gắn nhãn hoàn thành.
- QA/UAT đầy đủ, các môi trường khác và 75 test chưa bật; xác nhận chính sách giờ công theo ca.
- Kiểm tra dữ liệu/file legacy từng máy triển khai, tránh coi bản preview fixture là dữ liệu thật.
- PR cần leader review/merge vào develop; không merge trực tiếp main.
