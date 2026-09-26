# BE3 – Application Review, Enrollment & Mentor Assignment

## Tổng quan

BE3 chịu trách nhiệm:
- **Application**: Nộp và xét duyệt đơn ứng tuyển
- **Email thông báo**: Gửi kết quả duyệt/từ chối cho TTS
- **Department, InternshipProgram, Mentor**: Quản lý phòng ban, chương trình, mentor
- **ProgramEnrollment**: Ghi danh TTS vào chương trình
- **MentorAssignment**: Phân và đổi mentor

## Schema SQL

Chạy theo thứ tự:
1. `database/schema.sql` (BE1 – users, roles)
2. `sql/schema_be2.sql` (BE2 – intern_profiles, applications, documents, contracts)
3. `sql/schema_be3.sql` (BE3 – departments, internship_programs, mentors, program_enrollments, mentor_assignments)

> **Lưu ý**: `schema_be3.sql` chỉ tạo bảng mới, KHÔNG chỉnh sửa bảng `applications` hoặc bất kỳ bảng BE1/BE2 nào.
> Nếu đã chạy bản `schema_be3.sql` cũ, sao lưu DB rồi kiểm tra và chạy một lần
> `sql/migrate_be3_constraints.sql`. Không chạy migration này trên DB cài mới.
> Schema mới dùng unique generated columns để chặn Enrollment ACTIVE hoặc mentor
> hiện hành trùng nhau ngay tại DB. Service còn khóa profile/program/enrollment/mentor
> trong transaction để giữ sức chứa và trạng thái nhất quán khi có request đồng thời.

---

## API Contract (cho FE1/FE2/BE4)

### 1. Application – TTS nộp đơn

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/intern/applications` |
| **Method** | `POST` (nộp đơn), `GET` (xem danh sách) |
| **Quyền** | INTERN (qua session `currentUser`) |
| **Parameters** | Không cần – lấy userId từ session |
| **Response (GET)** | Forward → JSP, request attrs: `applications` (List\<Application\>) |
| **Response (POST)** | Redirect → `/intern/applications` với `successMessage` hoặc `errorMessage` trong session |
| **Lỗi** | 403 nếu không phải INTERN; error message nếu chưa có profile hoặc đã có đơn PENDING |

### 2. Application – HR xem & duyệt/từ chối

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/applications` |
| **GET Params** | `action=detail&id=<applicationId>` (chi tiết), `action=all` (tất cả), mặc định (PENDING) |
| **POST Params** | `action=approve&applicationId=<id>` hoặc `action=reject&applicationId=<id>&rejectReason=<text>` |
| **Quyền** | HR hoặc ADMIN (qua session `currentUser`) |
| **Request attrs (GET)** | `applications` (List), `application` (detail), `viewMode` ("list"\|"detail"), `filterAction` |
| **Response (POST)** | Redirect → `/hr/applications` với session message |
| **Lỗi** | 403 nếu không phải HR; error message nếu đơn không PENDING hoặc thiếu lý do từ chối |

### 3. Enrollment – HR ghi danh

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/enrollments` |
| **GET Params** | `action=enroll-form&applicationId=<id>` (form chọn chương trình) |
| **POST Params** | `action=enroll&applicationId=<id>&programId=<id>` |
| **Quyền** | HR hoặc ADMIN |
| **Validation** | Application phải APPROVED; chưa có enrollment; intern chưa có enrollment ACTIVE; program ACTIVE; chưa đầy |
| **Response** | Redirect → `/hr/enrollments` với session message chứa enrollmentId |

### 3a. Thiết lập danh mục – HR

`GET /hr/catalog` hiển thị phòng ban, chương trình, mentor và tài khoản có role
`MENTOR`. Các form `POST /hr/catalog` hỗ trợ `create-department`, `create-program`,
`activate-program`, `create-mentor`. Chương trình mới ở trạng thái `PLANNED`, HR
phải kích hoạt trước khi ghi danh. Mentor phải là tài khoản `MENTOR` đang hoạt động
và thuộc cùng phòng ban với chương trình khi phân công.

Các form BE3 dùng token CSRF trong session (`csrfToken`); POST thiếu/sai token trả 403.

### 4. Mentor Assignment – HR phân mentor

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/enrollments` |
| **GET Params** | `action=assign-form&enrollmentId=<id>` (form chọn mentor) |
| **POST Params** | `action=assign-mentor&enrollmentId=<id>&mentorId=<id>` |
| **Quyền** | HR hoặc ADMIN |
| **Validation** | Enrollment phải ACTIVE; chưa có mentor hiệu lực; mentor và tài khoản MENTOR ACTIVE; cùng phòng ban với chương trình; chưa đạt giới hạn mentee |
| **Response** | Redirect → `/hr/enrollments` với session message |

### 5. Mentor Change – HR đổi mentor

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/enrollments` |
| **GET Params** | `action=change-form&enrollmentId=<id>` (form đổi mentor) |
| **POST Params** | `action=change-mentor&enrollmentId=<id>&newMentorId=<id>&endReason=<text>` |
| **Quyền** | HR hoặc ADMIN |
| **Behavior** | Kết thúc assignment cũ (set ended_at + end_reason), tạo assignment mới. KHÔNG tạo enrollment mới |
| **Validation** | Phải có mentor hiện tại; endReason bắt buộc (tối đa 200 ký tự); mentor mới khác mentor cũ, tài khoản MENTOR ACTIVE, cùng phòng ban và chưa đạt giới hạn |

### 6. Enrollment – HR xem danh sách & chi tiết

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/enrollments` |
| **GET Params** | `action=detail&id=<enrollmentId>` (chi tiết + mentor history), mặc định (danh sách) |
| **Request attrs** | `enrollments` (List), `enrollment` (detail), `mentorHistory` (List\<MentorAssignment\>) |

### 7. Enrollment – TTS xem enrollment của mình

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/intern/enrollment` |
| **Method** | GET |
| **Quyền** | INTERN |
| **Request attrs** | `enrollment` (ProgramEnrollment, nullable), `currentMentor` (MentorAssignment, nullable) |

---

## Giao diện cho BE4

### Lấy enrollment_id
```java
// Qua EnrollmentService
EnrollmentService service = new EnrollmentService();
ProgramEnrollment enrollment = service.getActiveEnrollmentByIntern(profileId);
int enrollmentId = enrollment.getEnrollmentId();
```

### Lấy mentor hiện hành
```java
MentorAssignment currentMentor = service.getCurrentMentor(enrollmentId);
if (currentMentor != null) {
    int mentorId = currentMentor.getMentorId();
    // Use mentorId for task/report/evaluation
}
```

---

## Trạng thái nghiệp vụ

| Entity | Trạng thái | Ghi chú |
|---|---|---|
| Application | `PENDING` → `APPROVED` / `REJECTED` | Chỉ PENDING mới xử lý được |
| ProgramEnrollment | `ACTIVE` → `COMPLETED` / `CANCELLED` | Một TTS tối đa 1 ACTIVE |
| MentorAssignment | `ended_at IS NULL` (hiệu lực) / `ended_at IS NOT NULL` (đã kết thúc) | Một enrollment tối đa 1 hiệu lực |
| InternshipProgram | `PLANNED` / `ACTIVE` / `COMPLETED` / `CANCELLED` | Chỉ ACTIVE mới nhận ghi danh |
| Mentor | `ACTIVE` / `INACTIVE` | Chỉ ACTIVE mới được phân |

---

## Email Notification

- Sử dụng `EmailService` interface của BE1 (`com.internship.service.email.EmailService`)
- Gửi email SAU KHI quyết định duyệt/từ chối đã commit thành công
- Lỗi gửi email được log (SEVERE) nhưng KHÔNG rollback quyết định
- Test dùng stub `EmailService`, không gửi email thật
- Cấu hình SMTP trong `mail.properties` (classpath)

---

## Cấu trúc file mới

```
sql/schema_be3.sql                                    # Schema BE3
sql/migrate_be3_constraints.sql                       # Nâng cấp DB đã chạy BE3 cũ; không chạy trên DB mới
src/main/java/com/internship/
├── model/
│   ├── Application.java                              # Model đơn ứng tuyển
│   ├── Department.java                               # Model phòng ban
│   ├── InternshipProgram.java                        # Model chương trình
│   ├── Mentor.java                                   # Model mentor
│   ├── ProgramEnrollment.java                        # Model ghi danh
│   └── MentorAssignment.java                         # Model phân mentor
├── dao/
│   ├── ApplicationDAO.java                           # DAO đơn ứng tuyển
│   ├── DepartmentDAO.java                            # DAO phòng ban
│   ├── InternshipProgramDAO.java                     # DAO chương trình
│   ├── MentorDAO.java                                # DAO mentor
│   ├── ProgramEnrollmentDAO.java                     # DAO ghi danh
│   └── MentorAssignmentDAO.java                      # DAO phân mentor
├── service/
│   ├── ApplicationService.java                       # Nghiệp vụ đơn + email
│   ├── CatalogService.java                           # Thiết lập danh mục tối thiểu cho HR
│   └── EnrollmentService.java                        # Nghiệp vụ ghi danh + mentor
├── util/
│   ├── ConnectionProvider.java                       # Nguồn kết nối DB, hỗ trợ test
│   └── CsrfToken.java                                # Token bảo vệ form BE3
└── controller/
    ├── InternApplicationServlet.java                 # /intern/applications
    ├── HRApplicationServlet.java                     # /hr/applications
    ├── HREnrollmentServlet.java                      # /hr/enrollments
    ├── HRCatalogServlet.java                         # /hr/catalog
    └── InternEnrollmentServlet.java                  # /intern/enrollment
src/main/webapp/WEB-INF/views/
├── intern/applications.jsp                           # JSP tối thiểu TTS
├── intern/enrollment.jsp                             # JSP tối thiểu TTS
├── hr/applications.jsp                               # JSP tối thiểu HR
├── hr/enrollments.jsp                                # JSP tối thiểu HR
└── hr/catalog.jsp                                    # Thiết lập danh mục tối thiểu
src/test/java/com/internship/service/
├── ApplicationServiceTest.java                       # Unit test ApplicationService
└── EnrollmentServiceTest.java                        # Unit test EnrollmentService
```

## Chạy test

```bash
mvn clean test
```

> **Lưu ý**: Unit test gọi các Service thực với DAO stub và Connection double;
> có test hai request đồng thời trên stub. Chúng không thay thế kiểm thử khóa SQL,
> generated unique columns, servlet/JSP và transaction trên MySQL/Tomcat thật.

## Smoke test trước khi merge

Trên một database test riêng, chạy `database/schema.sql`, `sql/schema_be2.sql`,
`sql/schema_be3.sql` theo thứ tự và cấu hình `db.properties` local (không commit).
Đăng nhập bằng tài khoản HR/INTERN/Mentor test và kiểm tra:

1. HR tạo Department, Program PLANNED, kích hoạt Program, thêm Mentor cùng phòng ban ở `/hr/catalog`.
2. INTERN có profile nộp Application; nộp lần hai khi còn PENDING phải bị chặn.
3. HR duyệt/từ chối ở `/hr/applications`; thao tác lặp phải bị chặn. Tắt SMTP để
   xác nhận quyết định vẫn lưu và log ghi lỗi gửi email.
4. HR ghi danh Application APPROVED vào Program ACTIVE, phân/đổi Mentor.
   Kiểm tra không tạo hai Enrollment ACTIVE cho một TTS hoặc hai Mentor hiện hành
   cho một Enrollment, kể cả khi gửi hai request gần như đồng thời.
5. INTERN chỉ thấy Enrollment của mình; thử POST thiếu/sai `csrfToken` phải trả 403.

Các form tại `/hr/catalog` hiện là giao diện tối thiểu để QA kiểm thử tạo mới và
kích hoạt. Chỉnh sửa/xóa danh mục và giao diện hoàn chỉnh thuộc đợt tích hợp FE sau.
