# BE3 Sprint 3 — US23 lịch làm việc / US24 nghỉ phép

## Phạm vi và bàn giao

- BE3: migration, model, DAO, transaction, quyền và JSON API.
- FE1: màn Intern xem lịch ca, tạo/xem đơn nghỉ, đánh dấu nghỉ đã được duyệt.
- FE2: màn HR thiết lập lịch cá nhân/nhóm, duyệt/từ chối nghỉ và bộ lọc.
- BE2: dùng `work_schedules` / `leave_requests` khi tích hợp chấm công; không sửa lịch sử chấm công khi đổi lịch.
- Không sửa markup US14, menu chung, User/Role, auth filters, EmailService hoặc module BE4.

Chạy `sql/schema_be3_schedule_leave.sql` sau schema BE1/BE2/BE3. File chỉ bổ sung hai bảng,
không xóa dữ liệu. Máy đã có bảng tự tạo cần đối chiếu cấu trúc trước khi chạy;
`CREATE TABLE IF NOT EXISTS` không tự sửa bảng sai cấu trúc. Không chạy migration vào DB khác.

## Quy tắc nghiệp vụ

1. Lịch/đơn nghỉ dùng `enrollment_id`, không dùng `mentor_assignments.assignment_id`.
2. Ngày và giờ ca là thời gian địa phương `Asia/Ho_Chi_Minh`. Không chuyển thành UTC trên UI.
   Metadata `createdAt` / `reviewedAt` của đơn nghỉ trả ISO instant UTC; UI đổi sang múi giờ Việt Nam.
3. Chỉ ghi danh và chương trình `ACTIVE` nhận dữ liệu mới. Ngày phải nằm trong chương trình.
4. Ca bắt đầu trước khi kết thúc; không hỗ trợ ca qua đêm hay độ chính xác nhỏ hơn giây.
   Cặp ca 09:00–12:00 và 12:00–13:00 hợp lệ. Ca chồng hoặc trùng bị chặn.
5. Không tạo/sửa/hủy ca đã bắt đầu theo đồng hồ Việt Nam. Hủy ca chuyển `CANCELLED`, không xóa.
   Không cập nhật bất kỳ bảng chấm công nào. Đây là chính sách bảo toàn lịch sử của bản triển khai này.
6. Đơn nghỉ là cả ngày, hai đầu mút đều tính. `fromDate <= toDate`, không gửi cho ngày đã qua.
   Đơn `PENDING` hoặc `APPROVED` chặn khoảng nghỉ chồng; `REJECTED` không chặn gửi lại.
7. HR/Admin quyết định một lần, lưu người/thời điểm; từ chối bắt buộc có lý do.
   Không quyết định đơn cho ghi danh/chương trình đã kết thúc/hủy.
8. Intern chỉ đọc lịch/đơn của chính tài khoản trong session. Có thể xem lịch sử enrollment cũ.
   Sửa ID trên URL không cho phép xem hoặc tạo đơn của TTS khác.
9. `approvedLeave=true` chỉ khi ca có ngày thuộc đơn `APPROVED`. `PENDING` không đánh dấu đã nghỉ.
   Ca vẫn tồn tại để BE2 phân biệt nghỉ phép với vắng không phép; UI phân biệt ca `CANCELLED`.
10. Ghi theo nhóm dùng danh sách ID mà HR chọn, mỗi ID có kết quả riêng; không tự chọn thêm TTS.
    Tối đa 100 ID khác nhau. Nếu chỉ một phần lưu được, trả HTTP 207 / `success=false` và giữ các kết quả thành công.

Không thêm quy tắc cuối tuần/ngày lễ/số ngày phép, vì backlog chưa cung cấp lịch nghỉ hay hạn mức.
Nếu nhóm thay đổi chính sách thời gian/ca, cập nhật contract trước khi tích hợp FE/chấm công.

## Các endpoint

Mọi URL dưới đây bắt đầu sau context path của ứng dụng. Dùng session `currentUser` của BE1.
GET trả JSON, POST nhận `application/x-www-form-urlencoded` UTF-8 (không nhận JSON body).
POST phải có `csrfToken` lấy từ GET thành công cùng session. Gửi cookie session khi gọi API.

| URL | GET | POST | Quyền |
| --- | --- | --- | --- |
| `/api/intern/work-schedules` | Lịch của mình | Không hỗ trợ | INTERN |
| `/api/hr/work-schedules` | Lịch theo bộ lọc | create, bulk-create, update, cancel | HR, ADMIN |
| `/api/intern/leave-requests` | Đơn của mình | create | INTERN |
| `/api/hr/leave-requests` | Đơn theo bộ lọc | approve, reject | HR, ADMIN |

### GET lịch/đơn nghỉ

Tham số chung: `from=yyyy-MM-dd`, `to=yyyy-MM-dd` bắt buộc; khoảng tối đa 367 ngày.
`enrollmentId` và `programId` là bộ lọc tùy chọn, số nguyên dương.
GET đơn nghỉ nhận thêm `status=PENDING|APPROVED|REJECTED`; bỏ tham số để xem tất cả.
Intern bỏ `enrollmentId` sẽ lấy lịch/đơn của chính mình trong tất cả đợt phù hợp.

Ví dụ: `/api/intern/work-schedules?from=2026-10-01&to=2026-10-31`.

```json
{
  "success": true,
  "data": [{
    "scheduleId": 12, "enrollmentId": 8, "workDate": "2026-10-12",
    "startTime": "09:00", "endTime": "12:00", "note": "Ca sáng",
    "status": "ACTIVE", "internName": "TTS kiểm thử", "programName": "Program QA",
    "approvedLeave": false
  }],
  "csrfToken": "token-cua-session", "timezone": "Asia/Ho_Chi_Minh"
}
```

Không có dữ liệu: `data: []`, không phải lỗi. Giờ dùng ISO `HH:mm` hoặc `HH:mm:ss`.
Đơn nghỉ có các trường:
`leaveId`, `enrollmentId`, `fromDate`, `toDate`, `reason`, `status`, `reviewedBy`,
`reviewedAt`, `rejectReason`, `createdAt`, `internName`, `programName`.
Ba trường kết quả xét duyệt là `null` khi chưa xử lý; `rejectReason=null` khi APPROVED.
`reviewedBy` là user ID kiểu long; không ép sang int. Chỉ render nội dung bằng text, không innerHTML.

### POST lịch — HR/Admin

- `action=create`: `enrollmentId`, `workDate`, `startTime`, `endTime`; `note` tùy chọn, tối đa 500 ký tự.
  HTTP 201, `data: {"scheduleId":12}`.
- `action=update`: `scheduleId`, `workDate`, `startTime`, `endTime`, `note` tùy chọn.
  Không đổi enrollment của ca. HTTP 200, `data:null`.
- `action=cancel`: `scheduleId`. HTTP 200, `data:null`.
- `action=bulk-create`: `enrollmentIds=8,9,10`, `workDate`, `startTime`, `endTime`, `note` tùy chọn.
  HTTP 200 khi tất cả đạt; HTTP 207 khi có bất kỳ lỗi. FE2 bắt buộc hiển thị từng kết quả,
  không bỏ qua phần `data` chỉ vì `success=false`.

```json
{
  "success": false,
  "data": {
    "results": [
      {"enrollmentId":8,"scheduleId":12,"success":true,"errorCode":null},
      {"enrollmentId":9,"scheduleId":null,"success":false,"errorCode":"SCHEDULE_OVERLAP"}
    ],
    "successCount":1,"failureCount":1,"allSucceeded":false
  },
  "csrfToken":"token-cua-session","timezone":"Asia/Ho_Chi_Minh"
}
```

### POST nghỉ phép

- INTERN: `action=create`, `enrollmentId`, `fromDate`, `toDate`, `reason` (bắt buộc, tối đa 1000 ký tự).
  HTTP 201, `data:{"leaveId":5}`.
- HR/Admin: `action=approve`, `leaveId`. HTTP 200, `data:null`.
- HR/Admin: `action=reject`, `leaveId`, `rejectReason` (bắt buộc, tối đa 1000 ký tự).
  HTTP 200, `data:null`.

Sau ghi thành công, FE gọi lại GET để lấy trạng thái thật. Không tự đánh dấu APPROVED trước khi server lưu.
LeaveRequest không gửi email; phối hợp EmailService BE1 ở luồng Application là việc riêng.

## Lỗi và hết session

```json
{"success":false,"error":{"code":"LEAVE_OVERLAP","message":"Khoảng nghỉ trùng đơn đang chờ hoặc đã duyệt."}}
```

| HTTP | Code | Ý nghĩa |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | Thiếu/sai tham số, ID không dương, ngày/giờ/nội dung sai |
| 403 | FORBIDDEN | Sai role hoặc enrollment của người khác |
| 403 | INVALID_CSRF | Thiếu/sai token cùng session |
| 404 | NOT_FOUND | ID không tồn tại |
| 409 | ENROLLMENT_INACTIVE | Enrollment/program kết thúc hoặc hủy |
| 409 | SHIFT_ALREADY_STARTED | Ca đã bắt đầu, lịch sử không sửa |
| 409 | SCHEDULE_OVERLAP / SCHEDULE_CONFLICT | Ca chồng, hủy hoặc thay đổi |
| 409 | LEAVE_OVERLAP / LEAVE_ALREADY_REVIEWED | Đơn nghỉ trùng hoặc đã có quyết định |
| 503 | DATABASE_ERROR | Lỗi database; không trả SQL/password ra UI |

AuthenticationFilter chung hiện redirect HTTP 302 sang `/auth/login` khi chưa đăng nhập/hết session,
hoặc trả 503 HTML khi không xác minh được tài khoản. Vì vậy FE kiểm tra redirect/content-type trước
`response.json()`, đưa người dùng về login nếu cần. Servlet có fallback JSON 401 UNAUTHENTICATED,
nhưng không thay đổi hành vi filter BE1. Quyền được kiểm tra lại tại servlet/service.

## Kiểm chứng / chạy lại

- `mvn clean package`: bộ test mặc định, không gửi SMTP thật; JDBC opt-in được bỏ qua.
- `mvn -Dtest=ScheduleLeaveServiceTest,ScheduleLeaveServletTest,ApplicationServiceTest test`.
- MySQL thật: `mvn -Dtest=ScheduleLeaveJdbcTest -Dbe3.schedule.jdbc=true test`.
  Đọc credential từ `src/main/resources/db.properties` local; chỉ chấp nhận localhost/127.0.0.1,
  cổng 3306/3307. Cần quyền CREATE/DROP DATABASE. Test tạo database ngẫu nhiên riêng,
  chạy migration trên fixture rồi xóa đúng database do test tạo; không chạm `internship_management`.
- QA2 tích hợp cùng FE1/FE2: hai TTS, enrollment ACTIVE/COMPLETED, ngày biên, ca chồng/liền kề,
  thiếu CSRF, sửa ID, hai request đồng thời, quyết định lặp và đánh dấu nghỉ chỉ sau APPROVED.

Build/unit/JDBC không thay thế kiểm thử giao diện tích hợp của FE1/FE2.
