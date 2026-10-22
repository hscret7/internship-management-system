# BE4 — Sprint 2: dependency review và regression

## Phạm vi

Theo bản chốt Sprint 2, BE4 không nhận thêm User Story chức năng. Phần việc là review khóa/migration của enrollment, kiểm tra Task/TaskProgress/WeeklyReport/Feedback/Evaluation/Dashboard sau khi BE3 phân hoặc đổi mentor, và bàn giao kết quả cho QA/đúng owner.

Nhánh: `feature/be4-sprint2-regression`, tạo mới từ `develop` tại `8af20bc64b383575f8e7c75daedb68bdabe3f152`. Không gộp lại toàn bộ nhánh BE4 Sprint 1 cũ. Phần chương trình/mentor Sprint 2 của BE3 đang nằm trong PR #24 và được kiểm tra ghép riêng; BE4 không mang commit BE3 vào PR của mình.

## Kết quả review khóa và migration

- `program_enrollments.enrollment_id`: INT; các khóa trong `task_assignments`, `weekly_reports`, `evaluations` cũng INT, FK đúng bảng enrollment.
- `users.user_id`: BIGINT; tác giả task `created_by_user_id`, tác giả feedback `reviewer_user_id` là BIGINT. Test dùng user ID lớn hơn giới hạn INT.
- `evaluations.mentor_id` trỏ đến mentor đã đánh giá, không phải luôn là mentor hiện tại. Không sửa field này khi đổi mentor.
- Một task/enrollment có một assignment; một enrollment/tuần có một report; một enrollment có tối đa một evaluation. Lịch sử task progress append-only.
- Phân công mentor hiện tại: `ended_at IS NULL`; workload chỉ tính enrollment ACTIVE. Phân công cũ giữ nguyên để lưu lịch sử, không cộng lại vào workload nếu đổi quay về mentor ban đầu.
- BE3 Sprint 2 không đổi cấu trúc các khóa trên; **BE4 không cần migration mới**. File SQL chỉ cập nhật chú thích, không đổi DDL và không xóa dữ liệu.
- Thứ tự schema vẫn là BE1 → BE2 → BE3 → BE4. `CREATE TABLE IF NOT EXISTS` không tự sửa schema cũ sai kiểu; nếu môi trường QA lệch schema, báo owner trước, không DROP bảng để làm lại.

## Lỗi đã tái hiện và sửa

Trước sửa, ba ca MySQL đồng thời đều tái hiện: thao tác BE4 đã kiểm tra mentor cũ có quyền, nhưng HR có thể commit đổi mentor trước lúc BE4 ghi task assignment/feedback/evaluation. Quyền kiểm tra và thời điểm ghi không nằm trong cùng hàng rào giao dịch.

Sửa giới hạn trong BE4:

1. `TaskAccessDAO.lockEnrollmentForMentorWrite` khóa dòng enrollment bằng `SELECT … FOR UPDATE`, trong transaction.
2. Giao task, phản hồi và đánh giá dùng READ_COMMITTED, khóa enrollment trước kiểm tra mentor, giữ khóa đến commit/rollback. Đây là cùng dòng BE3 đang khóa để phân/đổi mentor.
3. Với feedback: đọc liên kết report/enrollment trước, khóa enrollment rồi khóa report, kiểm tra lại quyền/trạng thái. Thứ tự khóa enrollment → report tránh đảo khóa với thao tác bàn giao.
4. Evaluation chuyển từ kiểm tra/ghi rời sang transaction có rollback. Tác giả lịch sử và ràng buộc không ghi đè vẫn giữ nguyên.

Nếu thao tác BE4 lấy khóa trước, nó lưu xong rồi HR mới bàn giao. Nếu HR lấy khóa trước, request BE4 đợi, đọc lại mentor hiện tại sau bàn giao và từ chối mentor cũ. Không đổi route, tham số hay JSON cho FE.

## Kiểm chứng tự động

Lệnh tại repo với Java 21/Maven/MySQL local:

```powershell
mvn clean package
mvn -Dbe4.mysql=true clean package
```

Ngày kiểm tra: 30/09/2026.

- Bản BE4 riêng từ develop: **69 tests, 0 failures, 0 errors, 0 skipped**, đóng gói WAR thành công khi bật `be4.mysql`.
- Ghép thử cục bộ BE4 với BE3 Sprint 2 commit `237391225a4c9b9374878660e45cf1284e2bbe1f` (PR #24): **92 tests, 0 failures, 0 errors, 0 skipped**, WAR thành công với `mvn -Dbe3.mysql=true -Dbe4.mysql=true clean package`. Không có conflict. Nhánh ghép chỉ nằm local để kiểm chứng, không push hay merge vào develop.
- 9 test nhanh mới: thứ tự khóa trước quyền, commit sau ghi, không ghi nếu sai quyền, rollback khi lỗi SQL; cả giao task/feedback/evaluation.
- 14 ca MySQL mới: FK/kiểu khóa/unique, giữ lịch sử sau bàn giao, tiếp nhận report cũ, đánh giá bởi mentor mới, dashboard khi đổi đi/đổi lại, bàn giao thất bại, trạng thái COMPLETED/CANCELLED, truy cập khác owner/tài khoản LOCKED và sáu ca đồng thời (ba thao tác × hai thứ tự lấy khóa).
- Trong Maven, 3 test wrapper BE4 hiện có còn gọi 27 checks chi tiết của Sprint 1 (9 task + 11 report/evaluation/dashboard + 7 HTTP); không cộng thêm 27 vào tổng 69 vì chúng được gói trong 3 test wrapper.
- Các test MySQL tạo một database tạm mới cho từng ca, chỉ đọc credentials local, không hardcode/đưa mật khẩu vào log. Chỉ DROP database tên hợp lệ `be4_sprint2_test_<UUID>` sau khi CREATE của chính test thành công. Test không đổi cấu hình DBConnection toàn cục và không ghi vào database ứng dụng.
- Build WAR không xác nhận đã chạy toàn bộ màn hình Tomcat/FE. QA giao diện, session/CSRF trên web và acceptance tích hợp vẫn cần thực hiện.

## Ý nghĩa số liệu cần thống nhất với FE

- Dashboard mentor: activeInterns/task/report thuộc mentee đang được phân cho mentor hiện tại; bàn giao sang người mới thì các số này chuyển theo enrollment.
- `evaluationsCompleted`: số đánh giá do mentor đã thực hiện, là lịch sử tác giả. Đổi mentor không chuyển điểm đánh giá sang tên mentor mới và không dùng số này làm workload/còn chỗ.
- HR evaluation list hiển thị người đã đánh giá, không thay bằng người hiện phụ trách.
- Intern giữ nguyên task/progress/report/evaluation sau đổi mentor, không phải nộp lại.

## Checklist QA sau khi các PR được review và tích hợp

Dùng tài khoản HR, mentor A/B cùng phòng ban, mentor C phòng ban khác và TTS đã có enrollment ACTIVE. Không dùng tài khoản thử tự tạo trong database tạm (không có mật khẩu đăng nhập web).

| Ca | Thao tác trên web | Kết quả cần đối chiếu |
|---|---|---|
| Q01 | A giao task; TTS cập nhật tiến độ, nộp report; A phản hồi/đánh giá; HR đổi A → B | Enrollment ID/task/report/progress không đổi; tác giả task/feedback/evaluation vẫn là A |
| Q02 | Tải lại màn hình của A và B sau đổi mentor | A mất truy cập enrollment; B xem được dữ liệu cũ và giao task/phản hồi mới; TTS vẫn xem được dữ liệu của mình |
| Q03 | TTS nộp report trước bàn giao, B phản hồi sau bàn giao | Feedback lưu đúng B, report thành REVIEWED; A bị từ chối; không phản hồi hai lần |
| Q04 | Xem dashboard A/B và workload BE3, đổi B → A lần nữa | Mentee/task/report chuyển về A, không đếm assignment cũ hai lần; evaluation lịch sử không đổi tác giả |
| Q05 | HR thử đổi sang C khác phòng ban hoặc mentor hết chỗ | Bàn giao bị chặn; assignment hiện tại, quyền và dữ liệu BE4 vẫn nguyên |
| Q06 | HR đổi mentor đồng thời với A giao task/phản hồi/đánh giá | Thao tác tuần tự theo khóa; không lưu bằng quyền đã hết hiệu lực; FE hiện đúng lỗi/nhắc tải lại |
| Q07 | Đổi enrollment sang COMPLETED bằng luồng hợp lệ, xem/ghi dữ liệu | Lịch sử đọc được cho owner/mentor còn được phân; thao tác ghi bị chặn; không cộng completed/cancelled vào workload ACTIVE |
| Q08 | Đổi URL/ID sang enrollment của người khác, bỏ/sai CSRF, session hết hạn | Quyền/CSRF bị chặn hoặc về đăng nhập; không lộ dữ liệu khác owner |
| Q09 | Nội dung tiếng Việt, text do người dùng nhập; reload/refresh sau POST | UTF-8 đúng, FE escape nội dung, không tạo trùng assignment/report/evaluation |

QA ghi bug gồm branch/commit/WAR đang dùng, URL, role, ID dữ liệu, bước tái hiện, kết quả thực tế/kỳ vọng và log server đã che secrets.

- BE4: API task/progress/report/feedback/evaluation/dashboard và quyền liên quan.
- BE3: phân/đổi mentor, capacity, enrollment, chương trình/lịch.
- BE1: auth/session/role/filter/CSRF hạ tầng dùng chung.
- FE đúng màn hình: render, điều hướng, thông báo và lỗi hiển thị.

Không kết luận owner chỉ dựa vào trang 500; đối chiếu exception/log và phiên bản deploy trước.

## Cập nhật tích hợp ngày 05/10/2026

- BE2 PR #37 và BE3 PR #24 đã merge vào develop. Nhánh BE4 đã merge bình thường
  develop `1a96a9e`, không conflict; không rebase/force-push hoặc đưa BE2 cũ trở lại.
- `mvn -Dbe3.mysql=true -Dbe4.mysql=true clean package` trên máy BE4:
  **144 tests, 0 failures/errors/skipped**, WAR thành công. Có chạy đủ 14 ca MySQL
  BE4 và 2 ca MySQL BE3 trên database tạm riêng, không ghi vào database ứng dụng.
- Đã rà lại shared enrollment lock và thứ tự transaction với BE3 hiện tại:
  giao task/phản hồi/đánh giá giữ khóa đến commit, mentor cũ bị chặn sau bàn giao;
  enrollment và tác giả dữ liệu lịch sử không bị thay đổi.
- Không cần migration BE4 mới; database cũ vẫn cần migration tài liệu BE2 theo
  `sql/migrate_be2_documents.sql` khi dùng BE2 Sprint 2.
- PR #25 sẵn sàng tích hợp vào develop. FE3 PR #26 cần cập nhật theo develop có
  #24/#25 để kiểm tra giao diện kết hợp. Các kiểm tra Tomcat/UI và nghiệm thu của
  QA không được suy ra chỉ từ build; checklist Q01–Q09 vẫn là điều kiện bàn giao.

### Bổ sung định danh kết quả đánh giá (05/10/2026)

- PR #25 đã merge vào develop `1016905`.
- Bổ sung `internProfileId` cho `GET /hr/be4/evaluations`, giữ nguyên trường cũ
  và constructor Java cũ. Không cần migration.
- Số người đã đánh giá phải đếm theo mã hồ sơ, không theo họ tên hay số enrollment.
- Chạy lại **147 test, 0 failure/error/skip** trên MySQL thật, gồm regression
  hai người trùng tên và một người có nhiều đợt đánh giá.
