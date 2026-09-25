# INTERNSHIP MANAGEMENT SYSTEM - BACKEND 2 (BE2)
> **Phạm vi phụ trách**: Quản lý Hồ sơ thực tập sinh, Tài liệu (CV / Đơn ứng tuyển) và Hợp đồng thực tập.

---

## 1. Kết quả bàn giao của Backend 2 (BE2 Deliverables)

| Tầng / Thành phần | Tên File / Lớp | Trách nhiệm chính |
|---|---|---|
| **Model** | `InternProfile.java` | Thực thể hồ sơ thực tập sinh (thông tin cá nhân, trường, ngành, GPA, kỹ năng, trạng thái) |
| | `Document.java` | Thực thể tài liệu đính kèm (CV, Đơn xin thực tập, trạng thái duyệt PENDING/APPROVED/REJECTED) |
| | `Contract.java` | Thực thể Hợp đồng thực tập (US9 & US10, lưu confirmed_at và trạng thái CONFIRMED) |
| | `User.java` | Thực thể người dùng hỗ trợ phân quyền trong Session |
| **DAO** | `InternProfileDAO.java` | CRUD hồ sơ, tìm kiếm đa tiêu chí (theo tên, trường, ngành) |
| | `DocumentDAO.java` | Thêm, xem, xóa tài liệu đính kèm |
| | `ContractDAO.java` | Lưu hợp đồng theo application_id, cập nhật trạng thái xác nhận |
| **Service** | `InternProfileService.java` | Validation dữ liệu bắt buộc (Email regex, Phone regex, GPA 0-4), logic nghiệp vụ |
| | `DocumentService.java` | Kiểm tra định dạng (.pdf, .doc, .docx), dung lượng (<= 5MB), kiểm tra quyền xem/tải |
| **Servlet / Controller** | `InternProfileServlet.java` | Điều khiển tạo/sửa hồ sơ cá nhân và màn hình tìm kiếm cho HR |
| | `DocumentUploadServlet.java` | Xử lý tải lên CV/Đơn (TTS) và Hợp đồng (HR) |
| | `DocumentDownloadServlet.java`| Tải tài liệu có kiểm tra quyền bảo mật (TTS chỉ tải file mình, HR tải tất cả) |
| | `ContractServlet.java` | TTS xem hợp đồng và bấm xác nhận hợp đồng (US10) |
| **Database** | `sql/schema_be2.sql` | Cấu trúc bảng MySQL: `intern_profiles`, `documents`, `contracts` |
| | `sql/seed_data_qa.sql` | Dữ liệu mẫu hoàn chỉnh phục vụ QA test |

---

## 2. Hướng dẫn cài đặt & Chạy ứng dụng

### 2.1. Cấu hình Cơ sở dữ liệu (MySQL)
1. Mở MySQL Workbench hoặc phpMyAdmin (localhost:8080).
2. Chạy lần lượt 2 file SQL:
   - `sql/schema_be2.sql`: Tạo database và các bảng.
   - `sql/seed_data_qa.sql`: Nạp tài khoản và dữ liệu mẫu kiểm thử.
3. Nếu cổng MySQL khác `3306` (Ví dụ máy Huệ dùng Docker port `3307`), cấu hình biến môi trường hoặc chỉnh trực tiếp trong file `com.internship.util.DBContext.java`:
   - `DB_PORT`: `3307`
   - `DB_USER`: `root`
   - `DB_PASS`: `root`

### 2.2. Build và Run với Apache Tomcat 10.1
- Dự án quản lý bằng **Maven**:
  ```bash
  mvn clean package
  ```
- Deploy file `target/internship-management-system.war` lên Tomcat 10.1 (Webapps).
- Mở trình duyệt truy cập: `http://localhost:8082/internship-management-system/` (hoặc cổng Tomcat tương ứng của máy bạn).

---

## 3. Kịch bản kiểm thử dành cho QA (QA Test Cases)

1. **Test Tạo & Cập nhật Hồ sơ (US01)**:
   - Đăng nhập nhanh với `intern_nam`.
   - Vào `/intern/profile`. Thử bỏ trống tên, nhập sai email, số điện thoại hoặc GPA > 4.0 -> Hệ thống hiển thị thông báo lỗi validation.
   - Điền đầy đủ thông tin hợp lệ -> Bấm cập nhật -> CSDL cập nhật thành công.
2. **Test Tìm kiếm hồ sơ (US01 - HR View)**:
   - Đăng nhập với `hr_lan`.
   - Vào `/hr/interns`.
   - Lọc theo tên `Nam`, trường `Bách Khoa`, ngành `Công nghệ thông tin` -> Kết quả hiển thị chính xác.
3. **Test Upload CV & Đơn (US04)**:
   - Đăng nhập với TTS -> Chọn file ảnh `.png` hoặc file exe -> Hệ thống từ chối (chỉ nhận PDF, DOC, DOCX).
   - Chọn file > 5MB -> Hệ thống từ chối vì quá dung lượng.
   - Chọn file PDF hợp lệ -> Upload thành công và xuất hiện trong danh sách tài liệu.
4. **Test Phân quyền tải tài liệu (US04 - Security)**:
   - TTS `intern_nam` chỉ có thể download tài liệu có `profile_id` của mình.
   - Thử thay đổi ID trên URL sang file của người khác -> Báo lỗi `403 Forbidden`.
   - HR có thể download tài liệu của bất kỳ thực tập sinh nào.
5. **Test Hợp đồng thực tập (US9 & US10)**:
   - HR vào chi tiết ứng viên đã được duyệt (`/hr/intern-detail?id=1`), upload file hợp đồng cho đơn ứng tuyển số 1.
   - TTS `intern_nam` truy cập `/contract/view`, tải về xem và nhấn **"Tôi đồng ý & Xác nhận hợp đồng"**.
   - Hệ thống ghi nhận trạng thái `CONFIRMED` và lưu `confirmed_at`.
