# BE4 API contract (FE2 / FE3)

BE4 cung cấp JSON API trên cùng ứng dụng Tomcat. Không cần server/API riêng. Prefix thực tế là `/{contextPath}` (ví dụ `/internship-management-system`). User phải đăng nhập theo cơ chế BE1; BE4 lấy user ID từ session `currentUser`, không nhận `userId` do client gửi.

## Cách gọi

- GET và POST trả `application/json;charset=UTF-8`; ngày là `YYYY-MM-DD`, thời điểm là ISO local datetime hoặc `null`.
- POST dùng `Content-Type: application/x-www-form-urlencoded;charset=UTF-8`.
- Trước POST, FE gọi `GET /{role}/be4/csrf` trên cùng session, nhận `{ "csrfToken": "..." }`; gửi token qua header `X-CSRF-Token` hoặc trường `_csrf` trong form.
- `role` trong URL là `intern`, `mentor`, hoặc `hr`. User phải có đúng role tương ứng; riêng `ADMIN` có thể gọi phần `hr`.
- Lỗi trả `{ "error": "..." }` với mã 400 (tham số sai), 403 (không có quyền / CSRF), 404 (route sai), 409 (xung đột trạng thái/trùng), 415 (POST sai content type), 500 (lỗi DB). Nếu chưa đăng nhập, filter BE1 có thể chuyển về trang login.
- Tất cả ID dạng số nguyên dương. Không đưa dữ liệu HTML thô vào trang; FE phải escape khi render nội dung do người dùng nhập.

## FE3: Mentor

| Method | Path sau `/mentor/be4` | Tham số | Kết quả |
|---|---|---|---|
| GET | `/dashboard` | — | `MentorDashboard` |
| GET | `/enrollments` | — | `EnrollmentSummary[]` được phân cho mentor |
| GET | `/tasks` | `enrollmentId` | `Task[]` của đợt thực tập được phân |
| POST | `/tasks` | `title`, `description` (tùy chọn), `dueDate` (tùy chọn) | `Task` mới |
| GET | `/assignments` | `enrollmentId` | `TaskAssignment[]` |
| POST | `/assignments` | `taskId`, `enrollmentId` | `TaskAssignment` mới |
| GET | `/progress` | `assignmentId` | `TaskProgress[]` (lịch sử) |
| GET | `/reports` | `enrollmentId` | `WeeklyReport[]` |
| GET | `/feedback` | `reportId` | `ReportFeedback[]` |
| POST | `/feedback` | `reportId`, `comment` và/hoặc `rating` 1–5 | `ReportFeedback` mới; báo cáo thành `REVIEWED` |
| GET | `/evaluation` | `enrollmentId` | `Evaluation` hoặc `null` |
| POST | `/evaluation` | `enrollmentId`, `score` 0–100, `comment` (tùy chọn) | `Evaluation` mới |

## FE3: Intern

| Method | Path sau `/intern/be4` | Tham số | Kết quả |
|---|---|---|---|
| GET | `/enrollments` | — | `EnrollmentSummary[]` của TTS |
| GET | `/tasks` | `enrollmentId` | `Task[]` |
| GET | `/assignments` | `enrollmentId` | `TaskAssignment[]` |
| GET | `/progress` | `assignmentId` | `TaskProgress[]` |
| POST | `/progress` | `assignmentId`, `completionPercentage` 0–100, `note` (tùy chọn) | `TaskProgress` mới |
| GET | `/reports` | `enrollmentId` | `WeeklyReport[]` |
| POST | `/reports` | `enrollmentId`, `weekNumber` (bắt đầu từ 1), `content` (có thể trống khi nháp) | `WeeklyReport` nháp |
| POST | `/reports/update` | `reportId`, `content` | `WeeklyReport` đã sửa (chỉ nháp) |
| POST | `/reports/submit` | `reportId` | `WeeklyReport` đã nộp |
| GET | `/feedback` | `reportId` | `ReportFeedback[]` |
| GET | `/evaluation` | `enrollmentId` | `Evaluation` hoặc `null` |

## FE2: HR

| Method | Path sau `/hr/be4` | Tham số | Kết quả |
|---|---|---|---|
| GET | `/dashboard` | - | `HrDashboard` |
| GET | `/evaluations` | - | `EvaluationSummary[]` gồm mã enrollment, `internProfileId`, tên TTS, chương trình, mentor đánh giá, điểm, nhận xét và ngày đánh giá |
| GET | `/evaluation` | `enrollmentId` | `Evaluation` hoặc `null` |

`internProfileId` là mã hồ sơ TTS ổn định, dùng để đếm số người đã được đánh giá.
Không dùng tên TTS làm khóa: hai người có thể trùng tên và một người có nhiều đợt thực tập.
Đây là trường JSON bổ sung; các trường cũ và route không thay đổi, không cần migration DB.

`HrDashboard` gồm `totalInterns`, `activeEnrollments`, `completedEnrollments`, `completionRatePercent`, `internsByUniversity`, `internsByMajor`, `activeInternsByMentor`. Ba mảng cuối gồm `{ "label": "...", "count": 1 }`. Tỷ lệ = completed / (active + completed) × 100; không tính enrollment cancelled. `MentorDashboard` gồm `activeInterns`, `tasksTodo`, `tasksInProgress`, `tasksDone`, `reportsSubmitted`, `reportsReviewed`, `evaluationsCompleted`.

## Quy tắc nghiệp vụ

- Mentor đang hoạt động được tạo mẫu task; chỉ mentor hiện tại của enrollment `ACTIVE` được giao task, phản hồi báo cáo và đánh giá tổng kết cho enrollment đó.
- Intern chỉ sửa task/report của chính mình trong enrollment `ACTIVE`. Tiến độ tăng dần, 0–100; 100 là `DONE`, không sửa nữa. Mỗi lần cập nhật có một dòng lịch sử.
- Mỗi enrollment chỉ có một báo cáo cho mỗi `weekNumber`; báo cáo `DRAFT` mới sửa/nộp được; `SUBMITTED` mới được mentor phản hồi một lần.
- Một enrollment có tối đa một evaluation; score tối đa hai số thập phân. Mentor đánh giá trước khi HR kết thúc enrollment.
- GET vẫn xem được lịch sử của enrollment đã hoàn thành (khi intern còn sở hữu hoặc mentor vẫn được phân công); POST không cho sửa. Mentor đã bị gỡ/đổi khỏi enrollment không còn quyền xem lịch sử của enrollment đó.
- Đổi mentor không đổi `enrollment_id`, task/report/progress đã có hoặc tác giả lịch sử. Mentor mới được xem và tiếp tục xử lý dữ liệu thuộc enrollment, nhưng không được ghi đè evaluation đã tạo.
- `activeInterns`, số task và report trên dashboard mentor tính theo phân công hiện tại. `evaluationsCompleted` là số evaluation do mentor đó đã thực hiện (lịch sử tác giả), không phải số evaluation của mentee hiện tại. FE không dùng chỉ số này để suy ra workload/còn chỗ.
- Giao task, phản hồi và đánh giá giữ khóa enrollment cùng transaction với kiểm tra mentor hiện tại. Nếu thao tác HR đổi mentor đã khóa trước, request phải đợi và kiểm tra lại quyền sau bàn giao; nếu thao tác BE4 khóa trước, nó hoàn tất trước khi HR bàn giao. Không có thay đổi format API.

## Tích hợp DB và kiểm thử

Chạy `database/schema.sql`, `sql/schema_be2.sql`, `sql/schema_be3.sql`, rồi `sql/schema_be4.sql` theo thứ tự trên database test. Bản Sprint 2 không đổi DDL hay file chung BE1/BE2/BE3; không cần migration mới cho BE4.

`mvn clean package` chạy kiểm tra nhanh không cần MySQL; bộ MySQL opt-in không được chạy nếu chưa bật. `mvn -Dbe4.mysql=true clean package` chạy thêm 14 ca regression trên database tạm, dùng tài khoản từ `db.properties` local (không commit). Test cần quyền CREATE/DROP database và chỉ dọn database `be4_sprint2_test_<UUID>` do chính lần chạy đó tạo; không thay đổi database ứng dụng. Kết quả và checklist QA xem `docs/BE4_SPRINT2_HANDOFF.md`. Kiểm tra Tomcat/UI tích hợp là bước QA riêng, không được suy ra chỉ từ build thành công.
