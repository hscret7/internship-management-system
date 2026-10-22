# FE3 Sprint 2 — Mentor UI

## Phạm vi

- US29: Danh sách mentor, lọc phòng ban, tìm tên/email/chuyên môn, thêm hồ sơ mentor từ tài khoản MENTOR có sẵn.
- US30: Danh sách phân công, phân mentor đầu tiên, đổi mentor có lý do, xem lịch sử bàn giao.
- US31: Số TTS đang được hướng dẫn / giới hạn, chỗ còn lại, mentor đã đủ tải; lọc còn chỗ / đủ tải / tạm ngừng.
- Giao diện dùng khung và màu của FE1, hỗ trợ light/dark theo thiết lập hiện có, responsive và UTF-8.
- Không triển khai lại luật nghiệp vụ, không thêm schema, không thay đổi session/auth/CSRF của BE1 hoặc DAO/Service của BE3.

## Phụ thuộc và thứ tự tích hợp

Nhánh FE3 được tạo trực tiếp từ `develop` (`8af20bc`). Sau cập nhật ngày 05/10,
nhánh đã merge bình thường develop có BE2/BE3/BE4; diff PR so với develop chỉ
chứa thay đổi FE3, không đưa phiên bản prerequisite cũ trở lại.

1. Tích hợp bản khôi phục FE3 Sprint 1 **PR #27** để giữ `/hr/dashboard` và layout HR của #17/#18. Nhánh FE3 Sprint 2 đã merge bình thường nhánh khôi phục (không force-push); khi #27 được merge, diff #26 sẽ tập trung lại vào Sprint 2.
2. Cần tích hợp BE3 Sprint 2 **PR #24** trước khi merge #26 để có bộ lọc phòng ban, danh sách tài khoản đủ điều kiện, mentor cùng phòng ban/còn sức chứa và quy tắc ghi danh ACTIVE.
3. BE4 Sprint 2 **PR #25** không đổi API của màn hình này; nên tích hợp để có regression/hardening giao dịch khi đổi mentor đồng thời với thao tác task/report/evaluation.
4. Sau đó cập nhật nhánh FE3 theo `develop`, build và kiểm tra diff rồi merge PR FE3.
5. QA test trên bản tích hợp của nhóm. Không merge feature trực tiếp vào `main`.

Đã ghép thử cả #24 và #25 trên **nhánh local-only**, không push nhánh ghép này.

## Trang và luồng dữ liệu

| Màn hình | GET | Dữ liệu từ BE3 |
| --- | --- | --- |
| Đội ngũ & workload | `/hr/mentors?departmentId=...` | `departments`, `mentors`, `mentorUsers`, `selectedDepartmentId` |
| Danh sách phân công | `/hr/enrollments?ui=mentor` | `enrollments`, `viewMode=list` |
| Phân mentor | `/hr/enrollments?ui=mentor&action=assign-form&enrollmentId=...` | `enrollment`, `mentors`, `viewMode=assign-form` |
| Đổi mentor | `/hr/enrollments?ui=mentor&action=change-form&enrollmentId=...` | `enrollment`, `mentors`, `viewMode=change-form` |
| Lịch sử | `/hr/enrollments?ui=mentor&action=detail&id=...` | `enrollment`, `mentorHistory`, `viewMode=detail` |

Tất cả form POST gửi `csrfToken` do server cấp:

- Tạo mentor → `/hr/mentors`: `action=create-mentor`, `userId`, `departmentId`, `specialization`, `maxMentees`.
- Phân mentor → `/hr/enrollments`: `ui=mentor`, `action=assign-mentor`, `enrollmentId`, `mentorId`.
- Đổi mentor → `/hr/enrollments`: `ui=mentor`, `action=change-mentor`, `enrollmentId`, `newMentorId`, `endReason`.

Sau POST, filter trình bày FE3 giữ URL trong màn hình mentor và đưa phân/đổi về chi tiết đúng ghi danh. Filter **không xử lý quyền, không bỏ qua CSRF, không ghi DB**; không đổi redirect login/403 hoặc các thao tác của FE2.

`/hr/catalog`, `/hr/programs` và `/hr/enrollments` không có `ui=mentor` vẫn dùng màn hình gốc của module chương trình/ghi danh. `viewMode=enroll-form` luôn giữ nguyên form FE2. Controller catalog chỉ bổ sung alias đường dẫn giống #24, không thêm nghiệp vụ.

## Hành vi giao diện

- Tìm nhanh bỏ dấu tiếng Việt, không gọi thêm API; lọc phòng ban gửi GET về BE3.
- Số liệu tổng quan workload theo phòng ban đang chọn, **không đổi theo tìm nhanh**. Chỉ đếm mentor ACTIVE và ghi danh ACTIVE theo dữ liệu BE3.
- Bộ lọc theo mentor lấy `mentorId` từ link trên thẻ workload. Bỏ lọc xóa cả tham số URL để reload không khôi phục bộ lọc cũ.
- Phân/đổi chỉ có nút cho ghi danh ACTIVE. Mentor đầy tải, khác phòng ban, tạm ngừng hoặc mentor hiện tại không có trong lựa chọn sau tích hợp #24.
- Lý do đổi bắt buộc, tối đa 200 ký tự, chặn chỉ có khoảng trắng. Giới hạn số TTS là số nguyên lớn hơn 0. Server vẫn kiểm tra lại mọi dữ liệu.
- Có trạng thái chưa có mentor/ghi danh, không có người phù hợp, lỗi tải dữ liệu và lỗi nghiệp vụ. Lỗi nghiệp vụ không che dữ liệu đã tải thành công; dữ liệu thực sự lỗi không được thay bằng số 0.
- Tên/email/chuyên môn/lý do/lịch sử/thông báo được escape bằng JSTL `c:out`; phần lựa chọn động dùng `textContent`, không dùng `innerHTML`.
- Thêm mentor chỉ tạo hồ sơ từ tài khoản MENTOR đang hoạt động, chưa có hồ sơ. Cấp tài khoản đăng nhập thuộc Admin/BE1, không tự cấp tài khoản từ FE3.
- Form và danh sách SSR vẫn xem/submit được khi không có JavaScript; tìm nhanh, thẻ lựa chọn và chống bấm hai lần là progressive enhancement.

## File thay đổi

- `src/main/webapp/WEB-INF/views/hr/mentors.jsp`: danh bạ, form thêm, workload.
- `src/main/webapp/WEB-INF/views/hr/mentor-management.jsp`: danh sách, form phân/đổi, chi tiết/lịch sử.
- `catalog.jsp`, `enrollments.jsp`: wrapper chọn UI; giữ form/luồng FE2, khôi phục layout HR của Sprint 1. Header/footer chỉ chạy trong nhánh giao diện tương ứng, không lặp layout khi mở mentor UI.
- `assets/css/fe3-mentor.css`, `assets/js/fe3-mentor.js`: riêng cho Sprint 2, không sửa CSS/JS dùng chung.
- `common/sidebar.jsp`: mở hai link Đội ngũ mentor / Phân công mentor cho HR và Admin.
- `HRCatalogServlet.java`: alias đường dẫn, giống PR #24.
- `filter/Fe3MentorUiFilter.java` và 12 test: redirect trình bày có phạm vi cụ thể.

Các menu “Sắp có” ngoài US29–31 giữ nguyên; hoàn thiện các module đó thuộc owner tương ứng, không mở rộng scope FE3 trong PR này.

Các thay đổi FE3 Sprint 1 của #17/#18 được đưa vào qua nhánh khôi phục #27,
kèm regression test và tài liệu `FE3_SPRINT1_RESTORE.md`. Hai trang Intern
của FE1 #23 không bị thay đổi.

## Kiểm chứng ngày 01/10/2026

### Tự động - trước cập nhật khôi phục Sprint 1

- FE3 standalone: `mvn clean package` thành công; **58 test, 0 failure/error/skip**, gồm 12 test mới cho filter.
- Nhánh ghép thử FE3 + #24 + #25: build WAR thành công, không conflict; **100 test, 0 failure/error, 11 skip** (các test MySQL opt-in chưa cấu hình trên máy build; không tính là đã chạy).
- Bốn JSP catalog/enrollments/mentors/mentor-management đã compile bằng Jasper trên Tomcat 10.1.60, 0 lỗi.
- JavaScript syntax check thành công.

### Web thật với dữ liệu giả lập trong database riêng

- Đăng nhập HR; tải danh sách/phân công UTF-8, không HTTP 500.
- Thêm hồ sơ mentor thành công; redirect về `/hr/mentors`, số liệu sức chứa cập nhật.
- Phân mentor đầu tiên; đổi mentor có lý do; redirect đúng chi tiết, enrollment không đổi, lịch sử cũ và mới cùng hiển thị.
- Tìm tên không dấu; lọc đủ tải; lọc phòng ban; lọc ghi danh theo mentor và bỏ lọc.
- Lý do chỉ có khoảng trắng bị chặn trước submit.
- Mentor đủ tải bị server từ chối; thông báo lỗi vẫn ở chi tiết, mentor hiện tại không thay đổi.
- Ghi danh COMPLETED không có nút đổi; truy cập trực tiếp form đổi bị BE3 chặn và vẫn hiển thị danh sách đã tải.
- Nội dung `<script>` trong lý do lịch sử hiển thị như chữ, không tạo thẻ script.
- Tài khoản INTERN vào hai trang HR bị **403**; POST thiếu CSRF bị **403**.
- Route gốc `/hr/catalog` và `/hr/enrollments` vẫn **200**, giữ màn hình cũ.
- Desktop và viewport 390px: không tràn ngang toàn trang; bảng cuộn ngang bên trong; menu mobile và form responsive.

Không commit/push cấu hình database, mật khẩu, dữ liệu giả lập, WAR, ảnh preview hoặc nhánh tích hợp local-only.

### Cập nhật sau khôi phục #27 (01/10/2026)

- FE3 standalone chứa cả Sprint 1 + Sprint 2: `mvn clean package`
  **68 test, 0 failure/error/skip** (thêm 10 regression test dashboard/layout).
- Sáu HR JSP (dashboard/applications/catalog/enrollments/mentors/mentor-management)
  compile bằng Jasper với 0 lỗi.
- Smoke trên preview local có backend #24/#25: dashboard HR lấy số liệu,
  menu có cả Thống kê/Ghi danh/Đội ngũ mentor/Phân công mentor; các trang
  chương trình/ghi danh và hai trang mentor tải đúng, mỗi trang chỉ có
  một `.app-shell`, một sidebar và một vùng nội dung chính.
- Giữ draft #26 cho đến khi các PR phụ thuộc được tích hợp/review. Đây không
  phải xác nhận toàn bộ QA đã pass hoặc code đã có trên `develop`.

## Checklist QA sau tích hợp

- HR và Admin cùng thao tác được; Mentor/Intern/khách không truy cập hoặc POST được vào chức năng HR.
- Bộ lọc phòng ban, tài khoản đủ điều kiện, empty/error states với dữ liệu QA thật.
- Thêm trùng mentor, capacity không hợp lệ, đổi về cùng mentor, khác phòng ban và ghi danh không ACTIVE.
- Hai HR phân người cuối cùng còn chỗ đồng thời; server chỉ chấp nhận thao tác hợp lệ.
- Đổi mentor trong lúc người cũ gửi phản hồi/đánh giá hoặc giao task (regression BE4 #25).
- Logout/login, browser back/reload, thông báo sau redirect; không tạo lặp do submit hai lần.
- Menu/file dùng chung sau khi FE1/FE2 cập nhật; màn hình chương trình và form ghi danh của FE2 không bị thay thế.
- Kiểm tra light/dark, bàn phím, mobile, chuỗi tiếng Việt và chuỗi có ký tự đặc biệt.

## Kiểm chứng tích hợp ngày 05/10/2026

- Các phụ thuộc đã merge: BE2 #37, BE3 #24, BE4 #25 và bổ sung định danh BE4 #38.
  Nhánh FE3 cập nhật theo develop `ee07cef` bằng merge thường, không conflict,
  không force-push và không ghi trực tiếp vào main/develop.
- Giữ layout HR đã khôi phục #27; catalog/enrollments vẫn còn đầy đủ form,
  thao tác và regression của BE3/FE2. Mentor UI chỉ thay phần hiển thị có phạm vi riêng.
- Sửa thống kê TTS đã đánh giá: đếm `internProfileId`, không gộp người trùng tên
  hoặc đếm một người nhiều lần vì nhiều enrollment. Nếu API cũ thiếu định danh,
  hiển thị `—`, không đưa ra con số đoán từ tên.
- Build WAR trên máy FE3: **156 test, 0 failure/error, 13 skip**; các ca MySQL
  opt-in không được tính là đã chạy trên máy FE3 vì máy đó chưa cấu hình DB.
- Bản source giống máy FE3 được build trong môi trường thử riêng với MySQL thật:
  `mvn -Dbe3.mysql=true -Dbe4.mysql=true clean package`:
  **160 test, 0 failure/error/skip**. Không tác động database ứng dụng của nhóm.
- `node --test src/test/js/fe3-evaluation.test.cjs`: **4/4 pass**; kiểm tra người
  trùng tên, nhiều đợt cùng người, đổi tên, API cũ thiếu mã và danh sách rỗng.
- JavaScript syntax check thành công; 14 JSP HR/Mentor/Intern compile bằng
  Jasper trên Tomcat 10.1.60 với **0 lỗi**.
- **192 kiểm tra HTTP/API** trước/sau bàn giao trên Tomcat và database giả lập:
  đăng nhập các role, quyền/CSRF, task → tiến độ → báo cáo → phản hồi → đánh giá,
  mentor cũ mất quyền, mentor mới tiếp tục phản hồi; enrollment, dữ liệu và tác giả cũ giữ nguyên.
- Kiểm tra trình duyệt: tìm tên không dấu, lọc đủ tải, thêm hồ sơ mentor,
  phân/đổi và redirect, lý do chỉ có khoảng trắng bị chặn, lịch sử có chuỗi
  `<script>` hiển thị như chữ. Dashboard và kết quả đánh giá lấy dữ liệu thật.
- Mobile 390px: không tràn ngang trang; bảng dài cuộn trong khung; menu mobile
  mở/đóng được. Mỗi trang chỉ có một sidebar; UTF-8 hiển thị đúng.

Phạm vi code/tích hợp FE3 đã hoàn tất và bàn giao QA của nhóm. Các kết quả trên
là kiểm chứng kỹ thuật với dữ liệu thử, không thay thế nghiệm thu của QA hoặc
khẳng định các module ngoài scope của FE3 đã hoàn thành.

### Chốt sau tích hợp #26 (05/10/2026)

- PR #26 đã merge vào `develop` tại `ae942ccd`; bản sửa tiếp theo chỉ bổ sung
  độ tương phản bảng FE3 và regression test, không sửa CSS chung của FE1.
- Đồng bộ biến màu Bootstrap cho ô bảng và trạng thái hover/striped/active
  với palette của theme trong phạm vi `.fe3-page .table`.
- Build WAR cập nhật trên máy FE3: **157 test, 0 failure/error, 13 skip**.
  Các test MySQL opt-in vẫn chưa chạy trên máy FE3 do thiếu cấu hình DB.
- Build cùng source trong môi trường thử riêng có MySQL thật:
  **161 test, 0 failure/error/skip**; JavaScript behavior test **4/4 pass**.
- Tổng kiểm chứng web/API trong đợt tích hợp: **283 assertion đạt**, gồm
  91 kiểm tra cuối sau nạp lại WAR. Không sử dụng dữ liệu ứng dụng của nhóm.
- Trình duyệt xác nhận 3 đánh giá thuộc 2 hồ sơ trùng tên được đếm đúng là
  2 TTS; chữ trong ô bảng có màu `rgb(230, 235, 242)`, đúng màu chữ của theme
  tối đang dùng và đọc được trên nền tối.
- Các checklist QA bên trên vẫn cần nhóm nghiệm thu trên bản develop mới.
