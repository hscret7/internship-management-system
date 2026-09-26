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

### 4. Mentor Assignment – HR phân mentor

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/enrollments` |
| **GET Params** | `action=assign-form&enrollmentId=<id>` (form chọn mentor) |
| **POST Params** | `action=assign-mentor&enrollmentId=<id>&mentorId=<id>` |
| **Quyền** | HR hoặc ADMIN |
| **Validation** | Enrollment phải ACTIVE; chưa có mentor hiệu lực; mentor ACTIVE; chưa đạt giới hạn mentee |
| **Response** | Redirect → `/hr/enrollments` với session message |

### 5. Mentor Change – HR đổi mentor

| Thuộc tính | Giá trị |
|---|---|
| **URL** | `/hr/enrollments` |
| **GET Params** | `action=change-form&enrollmentId=<id>` (form đổi mentor) |
| **POST Params** | `action=change-mentor&enrollmentId=<id>&newMentorId=<id>&endReason=<text>` |
| **Quyền** | HR hoặc ADMIN |
| **Behavior** | Kết thúc assignment cũ (set ended_at + end_reason), tạo assignment mới. KHÔNG tạo enrollment mới |
| **Validation** | endReason bắt buộc; mentor mới ACTIVE và chưa đạt giới hạn |

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
| ProgramEnrollment | `ACTIVE` → `COMPLETED` / `WITHDRAWN` | Một TTS tối đa 1 ACTIVE |
| MentorAssignment | `ended_at IS NULL` (hiệu lực) / `ended_at IS NOT NULL` (đã kết thúc) | Một enrollment tối đa 1 hiệu lực |
| InternshipProgram | `ACTIVE` / `INACTIVE` / `COMPLETED` | Chỉ ACTIVE mới nhận ghi danh |
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
│   └── EnrollmentService.java                        # Nghiệp vụ ghi danh + mentor
└── controller/
    ├── InternApplicationServlet.java                 # /intern/applications
    ├── HRApplicationServlet.java                     # /hr/applications
    ├── HREnrollmentServlet.java                      # /hr/enrollments
    └── InternEnrollmentServlet.java                  # /intern/enrollment
src/main/webapp/WEB-INF/views/
├── intern/applications.jsp                           # JSP tối thiểu TTS
├── intern/enrollment.jsp                             # JSP tối thiểu TTS
├── hr/applications.jsp                               # JSP tối thiểu HR
└── hr/enrollments.jsp                                # JSP tối thiểu HR
src/test/java/com/internship/service/
├── ApplicationServiceTest.java                       # Unit test ApplicationService
└── EnrollmentServiceTest.java                        # Unit test EnrollmentService
```

## Chạy test

```bash
mvn clean test
```

> **Lưu ý**: Các test dùng stub DAO, KHÔNG cần MySQL. Luồng transaction (approve/reject/enroll/assign qua DB thật) chưa kiểm chứng runtime – cần MySQL + Tomcat để test tích hợp.
