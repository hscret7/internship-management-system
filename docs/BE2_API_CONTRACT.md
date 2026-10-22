# BE2 – Intern Profiles, Documents & Contract Management

## Tổng quan phạm vi BE2 (Sprint 1 & Sprint 2)

BE2 chịu trách nhiệm:
- **Intern Profile (Hồ sơ TTS)**: Tạo lập hồ sơ (Sprint 1), HR chỉnh sửa hồ sơ TTS theo whitelist an toàn (US02 - Sprint 2), HR tìm kiếm và lọc danh sách TTS theo trường/ngành/tên (US03 - Sprint 2).
- **Document Management (Quản lý tài liệu)**: TTS tải lên tài liệu (CV, bảng điểm, bằng cấp, CMND) định dạng PDF/DOC/DOCX tối đa 5MB (Sprint 1/US04), HR duyệt (`APPROVED`) và từ chối (`REJECTED` kèm lý do bắt buộc) tài liệu của TTS (US05 - Sprint 2), ghi nhận người duyệt và thời gian duyệt.
- **Contract Management (Quản lý hợp đồng)**: HR upload hợp đồng đính kèm đơn ứng tuyển `APPROVED` (US09 - Sprint 2, PDF/DOC/DOCX tối đa 5MB, cho phép thay thế khi chưa xác nhận, chặn ghi đè khi đã `CONFIRMED`), TTS xem và xác nhận hợp đồng (US10 - Sprint 2, chỉ INTERN sở hữu mới được xác nhận, ghi nhận `confirmed_at`).

---

## Schema SQL & Migration

### Thứ tự chạy khởi tạo DB mới:
1. `database/schema.sql` (BE1 – users, roles)
2. `sql/schema_be2.sql` (BE2 – intern_profiles, applications, documents, contracts)
3. `sql/schema_be3.sql` (BE3 – departments, internship_programs, mentors, program_enrollments, mentor_assignments)

### Migration cho Database hiện có (Baseline develop `b207e52` / `8af20bc`):
Chạy script:
```sql
sql/migrate_be2_documents.sql
```
Script này bổ sung an toàn 3 cột vào bảng `documents`:
- `reviewed_by` BIGINT (FK -> `users(user_id)`)
- `reviewed_at` DATETIME
- `reject_reason` TEXT
*(Bảng `contracts` đã có sẵn `confirmed_at` và ràng buộc cần thiết từ Sprint 1).*

---

## API Contract chi tiết (Dành cho FE1, FE2, BE3, BE4, QA)

### 1. US02: HR Chỉnh sửa hồ sơ TTS

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/interns/edit` |
| **Method** | `POST` |
| **Quyền** | HR hoặc ADMIN (qua session `currentUser`) |
| **Request Parameters** | `profileId` (bắt buộc, int), `fullName` (bắt buộc, string), `dob` (bắt buộc, yyyy-MM-dd), `gender` (bắt buộc: Male/Female/Other), `phone` (bắt buộc, format VN), `email` (bắt buộc, email format), `university` (bắt buộc, string), `major` (bắt buộc, string), `gpa` (tùy chọn, float [0.0 - 4.0]), `skills` (tùy chọn, string) |
| **Bảo mật & Whitelist** | Server chỉ cập nhật các trường thông tin học vấn/cá nhân. Tuyệt đối KHÔNG nhận `userId`, `role`, hay trạng thái của `Application` từ request của form. `profileId` phải tồn tại. |
| **Response** | Redirect → `/hr/intern-detail?id=<profileId>` kèm `successMessage` hoặc `errorMessage` trong session |
| **Mã lỗi** | 403 nếu không phải HR/Admin; Redirect kèm `errorMessage` nếu vi phạm validation format |

---

### 2. US03: HR Tìm kiếm và Lọc danh sách TTS

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/interns` hoặc `/hr/interns/search` |
| **Method** | `GET` |
| **Quyền** | HR hoặc ADMIN |
| **Query Parameters** | `keyword` (tìm chung), `name` (tìm theo tên), `university` (lọc theo trường đại học), `major` (lọc theo chuyên ngành) |
| **Request Attributes (JSP)** | `profiles` (List\<InternProfile\>), `searchName` (String), `searchUniversity` (String), `searchMajor` (String), `keyword` (String) |
| **Response** | Forward → `/hr/intern-list.jsp` |

---

### 3. HR Xem chi tiết hồ sơ & Hợp đồng của TTS

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/intern-detail` |
| **Method** | `GET` |
| **Quyền** | HR hoặc ADMIN |
| **Query Parameters** | `id=<profileId>` (bắt buộc, int) |
| **Request Attributes (JSP)** | `profile` (InternProfile), `documents` (List\<Document\>), `application` (Application - đơn ứng tuyển mới nhất), `contract` (Contract - nếu đã upload hợp đồng) |
| **Response** | Forward → `/hr/intern-detail.jsp` |

---

### 4. US05: HR Duyệt / Từ chối tài liệu của TTS

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/document/review` (hoặc `/hr/document/review`, `/hr/documents/review`) |
| **Method** | `POST` |
| **Quyền** | HR hoặc ADMIN |
| **Request Parameters** | `action` (`approve` hoặc `reject`), `documentId` (int, bắt buộc), `rejectReason` (bắt buộc khi action là `reject`), `profileId` (tùy chọn, phục vụ redirect) |
| **Hỗ trợ định dạng** | - **HTML Form**: Redirect về `/hr/intern-detail?id=<profileId>` với session message.<br>- **AJAX / JSON** (gửi `Accept: application/json` hoặc header `X-Requested-With: XMLHttpRequest`): Trả JSON `{"success": true, "message": "..."}` hoặc `{"success": false, "message": "..."}` với HTTP 200/400/403. |
| **Validation** | - Trạng thái chuyển: `PENDING` → `APPROVED` hoặc `REJECTED`.<br>- Từ chối bắt buộc phải có `rejectReason`.<br>- Ghi nhận `reviewed_by = currentUser.userId` và `reviewed_at = NOW()`.<br>- Duyệt/từ chối tài liệu KHÔNG tự ý thay đổi trạng thái của `Application`. |

---

### 5. US04: TTS Upload tài liệu

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/document/upload` |
| **Method** | `POST` (enctype: `multipart/form-data`) |
| **Quyền** | INTERN (tải cho hồ sơ chính mình) |
| **Parameters** | `file` (Part), `docType` (`CV`, `TRANSCRIPT`, `CERTIFICATE`, `IDENTITY_CARD`) |
| **Ràng buộc** | File định dạng `.pdf`, `.doc`, `.docx`. Dung lượng tối đa: **5 MB**. Tài liệu mới tải lên luôn có trạng thái `PENDING`. Khi tải lại, giữ nguyên lịch sử tài liệu cũ bị từ chối. |
| **Response** | Redirect → `/intern/documents` kèm thông báo |

---

### 6. Xem và Tải tài liệu (Download an toàn)

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/document/download` |
| **Method** | `GET` |
| **Query Parameters** | `id=<documentId>` (bắt buộc, int) |
| **Kiểm tra quyền** | - HR và Admin được tải/xem mọi tài liệu.<br>- INTERN chỉ được tải tài liệu thuộc hồ sơ của chính mình (kiểm tra qua `profileId` gắn với `currentUser.userId`). |
| **Mã lỗi** | 401 nếu chưa đăng nhập; 403 nếu truy cập trái phép; 404 nếu file không tồn tại |

---

### 7. US09: HR Tải lên và Quản lý Hợp đồng

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/contract/upload` |
| **Method** | `POST` (enctype: `multipart/form-data`) |
| **Quyền** | HR hoặc ADMIN |
| **Request Parameters** | `file` (Part hợp đồng: `.pdf`, `.doc`, `.docx`, tối đa 5MB), `applicationId` (bắt buộc, int), `profileId` (tùy chọn, để redirect) |
| **Quy ước & Ràng buộc nghiệp vụ Sprint 2** | 1. Đơn ứng tuyển (`Application`) phải có trạng thái **`APPROVED`** mới được tải hợp đồng.<br>2. Trạng thái ban đầu: **`PENDING_CONFIRMATION`**.<br>3. Cho phép HR tải file mới để thay thế khi hợp đồng đang ở `PENDING_CONFIRMATION`.<br>4. **Không cho phép ghi đè**: Nếu hợp đồng đã được TTS xác nhận (**`CONFIRMED`**), hệ thống chặn ghi đè và ném lỗi/thông báo lỗi. |
| **Response** | Redirect → `/hr/intern-detail?id=<profileId>` (hoặc `/contract/view?id=<contractId>`) với thông báo |

---

### 8. US10: TTS Xem và Xác nhận Hợp đồng

| Thuộc tính | Giá trị |
|---|---|
| **Xem hợp đồng** | `GET /contract/view` (TTS xem hợp đồng của mình, HR xem theo `id=<contractId>`) |
| **Xác nhận hợp đồng** | `POST /contract/confirm` |
| **Query/Body Params** | `contractId=<contractId>` (hoặc `id=<contractId>`) |
| **Quyền** | **Chỉ INTERN sở hữu hợp đồng** mới có quyền xác nhận (kiểm tra `currentUser.role == INTERN` và hợp đồng thuộc về `profileId` của user) |
| **Behavior** | Chuyển trạng thái `status`: `PENDING_CONFIRMATION` → **`CONFIRMED`**. Ghi nhận thời điểm xác nhận vào cột **`confirmed_at`** (CURRENT_TIMESTAMP). Idempotent nếu đã xác nhận trước đó. |
| **Response** | Redirect → `/contract/view` với `successMessage = "Hợp đồng đã được xác nhận thành công!"` |
| **Lỗi** | 403 nếu không phải TTS sở hữu; 404 nếu hợp đồng không tồn tại |

---

### 9. Tải xuống Hợp đồng (Download an toàn)

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/contract/download` |
| **Method** | `GET` |
| **Query Parameters** | `id=<contractId>` |
| **Kiểm tra quyền** | HR/Admin hoặc INTERN sở hữu hợp đồng. Người dùng khác nhận HTTP 403. |
| **Header Response** | `Content-Type: application/pdf` (hoặc mime-type tương ứng), `Content-Disposition: inline/attachment` |

---

## Tóm tắt trạng thái và Vòng đời dữ liệu

```
[Document Lifecycle]
                 ┌───> APPROVED (by HR/Admin)
 PENDING (Upload)┤
                 └───> REJECTED (by HR/Admin, bắt buộc reject_reason)
 (Re-upload tạo bản ghi PENDING mới, lưu lại lịch sử bản REJECTED)

[Contract Lifecycle]
 Application: APPROVED
         │
         ▼
 PENDING_CONFIRMATION (HR Upload) ──(TTS Confirms)──> CONFIRMED (Lưu confirmed_at, Khóa sửa/ghi đè)
         ▲                │
         └──(HR Re-upload)┘
```

---

## BE2 Sprint 3 – Chấm công, Phụ cấp, Yêu cầu hỗ trợ & Handoff US34

### 1. Schema SQL Sprint 3 (`sql/schema_be2_sprint3.sql`)

Bổ sung 3 bảng:
1. `attendances`:
   - `attendance_id` INT AUTO_INCREMENT PRIMARY KEY
   - `enrollment_id` INT NOT NULL (FK -> `program_enrollments(enrollment_id)`)
   - `work_date` DATE NOT NULL
   - `check_in_time` DATETIME NOT NULL
   - `check_out_time` DATETIME NULL
   - `notes` TEXT NULL
   - `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
2. `allowances`:
   - `allowance_id` INT AUTO_INCREMENT PRIMARY KEY
   - `enrollment_id` INT NOT NULL
   - `allowance_type` VARCHAR(50) NOT NULL
   - `period` VARCHAR(20) NOT NULL
   - `amount` DECIMAL(12,2) NOT NULL
   - `status` VARCHAR(20) DEFAULT 'PENDING'
   - `paid_at` DATETIME NULL
   - `created_by` BIGINT NOT NULL
3. `support_requests`:
   - `request_id` INT AUTO_INCREMENT PRIMARY KEY
   - `user_id` BIGINT NOT NULL
   - `request_type` VARCHAR(50) NOT NULL
   - `title` VARCHAR(255) NOT NULL
   - `content` TEXT NOT NULL
   - `status` VARCHAR(20) DEFAULT 'PENDING'
   - `response` TEXT NULL
   - `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

---

### 2. Dữ liệu chuyên cần & Handoff cho BE4 (US34: Xuất dữ liệu chuyên cần)

BE4 cần nguồn dữ liệu `AttendanceService/schema/filter` để thực hiện US34 (Xuất dữ liệu chuyên cần XLSX/PDF) hoặc tích hợp vào báo cáo cuối kỳ.

#### A. Service Contract (`com.internship.service.AttendanceService`)

```java
// Khởi tạo
AttendanceService attendanceService = new AttendanceService();

// 1. Lấy danh sách chi tiết các ca chấm công
List<AttendanceReportRow> rows = attendanceService.getReport(
    User actor,             // Bắt buộc (HR, ADMIN, MENTOR, hoặc INTERN)
    Integer enrollmentId,   // Tùy chọn (null hoặc <= 0 để xem tất cả)
    Integer programId,      // Tùy chọn (null hoặc <= 0 để xem tất cả)
    LocalDate fromDate,     // Tùy chọn (null sẽ tự động lùi 30 ngày)
    LocalDate toDate        // Tùy chọn (null sẽ lấy ngày hiện tại)
);

// 2. Lấy danh sách kèm lọc theo trạng thái
List<AttendanceReportRow> filtered = attendanceService.getReport(
    actor, enrollmentId, programId, fromDate, toDate,
    String statusFilter     // "PRESENT", "ABSENT", "LEAVE", "MISSING_CHECKOUT", hoặc null
);

// 3. Lấy tổng kết số liệu chuyên cần (phục vụ thống kê / tổng kết cuối kỳ)
AttendanceSummary summary = attendanceService.getSummary(
    actor, enrollmentId, programId, fromDate, toDate
);
```

#### B. Cấu trúc đối tượng `AttendanceReportRow`

Mỗi bản ghi đại diện cho một ca làm việc đã được lên lịch (từ `work_schedules`) đối chiếu với dữ liệu quẹt thẻ thực tế (`attendances`) và đơn nghỉ phép (`leave_requests`):

| Trường | Kiểu dữ liệu | Getter | Ý nghĩa |
|---|---|---|---|
| `enrollmentId` | `int` | `getEnrollmentId()` | Mã ghi danh của thực tập sinh |
| `internName` | `String` | `getInternName()` | Họ và tên thực tập sinh |
| `programName` | `String` | `getProgramName()` | Tên chương trình thực tập |
| `workDate` | `LocalDate` | `getWorkDate()` | Ngày làm việc |
| `startTime` | `LocalTime` | `getStartTime()` | Giờ bắt đầu ca |
| `endTime` | `LocalTime` | `getEndTime()` | Giờ kết thúc ca |
| `checkInTime` | `LocalDateTime` | `getCheckInTime()` | Thời điểm check-in (null nếu không check-in) |
| `checkOutTime` | `LocalDateTime` | `getCheckOutTime()` | Thời điểm check-out (null nếu chưa/quên check-out) |
| `approvedLeave`| `boolean` | `isApprovedLeave()` | `true` nếu ca này có đơn nghỉ phép APPROVED |
| `status` | `String` | `getStatus()` | Trạng thái: `PRESENT`, `MISSING_CHECKOUT`, `LEAVE`, `ABSENT` |
| `workedHours` | `double` | `getWorkedHours()` | Số giờ làm thực tế (làm tròn 2 chữ số thập phân, 0.0 nếu chưa hoàn thành ca) |

#### C. Cấu trúc đối tượng `AttendanceSummary`

```java
public record AttendanceSummary(
    int totalSchedules,        // Tổng số ca làm việc được phân công
    int presentCount,          // Số ca có mặt đầy đủ (check-in & check-out)
    int absentCount,           // Số ca vắng mặt không phép
    int leaveCount,            // Số ca nghỉ phép có duyệt
    int missingCheckoutCount,  // Số ca quên check-out
    double totalWorkedHours    // Tổng số giờ làm việc tích lũy (hours)
) {}
```

#### D. Quy tắc xử lý bộ lọc và ngoại lệ (Validation)

1. **Quyền truy cập (`actor`):**
   - INTERN chỉ xem được ca làm việc của chính mình (hệ thống tự động ép `ownerId = actor.userId`).
   - HR và ADMIN được xem toàn bộ theo bộ lọc.
   - MENTOR được phân quyền xem mentees.
   - Actor null hoặc role không hợp lệ -> ném `IllegalArgumentException`.
2. **Khoảng thời gian (`fromDate`, `toDate`):**
   - Cả hai null: Mặc định 30 ngày gần nhất tính đến hôm nay.
   - Một trong hai null: Tự động suy ra đầu mút còn lại trong phạm vi 30 ngày.
   - `fromDate > toDate`: Ném `IllegalArgumentException("Từ ngày (fromDate) không được sau đến ngày (toDate).")`.
   - Khoảng cách > 367 ngày: Ném `IllegalArgumentException("Khoảng thời gian tra cứu không được vượt quá 367 ngày.")`.
3. **Dữ liệu rỗng:** Trả về `List.of()` rỗng hoặc `AttendanceSummary` với các chỉ số = 0, tuyệt đối không gây lỗi `NullPointerException`.
4. **Export (US34):** BE4 có thể gọi trực tiếp `attendanceService.getReport(...)` hoặc `attendanceService.getSummary(...)` để ghi ra file Excel (`XSSFWorkbook`) hoặc PDF (`PDDocument`), tương tự như `ProgramStatisticsExport` và `EndTermExport`.

---

### 3. HTTP Endpoints Sprint 3

| Endpoint | Method | Role | Mô tả |
|---|---|---|---|
| `/intern/attendance` | `GET` | INTERN | Xem lịch sử và báo cáo chấm công của bản thân |
| `/intern/attendance` | `POST` | INTERN | Thực hiện `checkin` hoặc `checkout` (yêu cầu CSRF token) |
| `/hr/attendance` | `GET` | HR/ADMIN | Báo cáo chấm công toàn công ty theo bộ lọc ngày, enrollment, program |
| `/intern/allowances` | `GET` | INTERN | Xem lịch sử các khoản phụ cấp cá nhân |
| `/hr/allowances` | `GET` | HR/ADMIN | Danh sách phụ cấp của toàn bộ TTS |
| `/hr/allowances` | `POST` | HR/ADMIN | Tạo mới phụ cấp (`action=create`) hoặc cập nhật trạng thái chi trả (`action=update_status`) |
| `/intern/support` | `GET` | INTERN | Danh sách các yêu cầu hỗ trợ của cá nhân |
| `/intern/support` | `POST` | INTERN | Gửi yêu cầu hỗ trợ mới (`requestType`, `title`, `content`) |
| `/hr/support` | `GET` | HR/ADMIN | Danh sách toàn bộ ticket hỗ trợ cần xử lý |
| `/hr/support` | `POST` | HR/ADMIN | Trả lời và cập nhật trạng thái ticket (`requestId`, `status`, `response`) |

