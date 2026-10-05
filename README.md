# INTERNSHIP MANAGEMENT SYSTEM

Hệ thống Quản lý Thực tập sinh (Internship Management System) hỗ trợ quy trình quản trị thực tập sinh toàn diện: từ đăng ký, ứng tuyển, quản lý hồ sơ, duyệt tài liệu, ký hợp đồng, ghi danh chương trình, phân công mentor cho đến giao việc, báo cáo tuần và đánh giá kết quả thực tập.

---

## 1. Các phân hệ chức năng & Tài liệu kỹ thuật

Dự án được phân chia theo các vai trò và phân hệ:

- **BE1 (Core & Authentication)**: Đăng ký, đăng nhập, phân quyền người dùng (ADMIN, HR, MENTOR, INTERN), session và dịch vụ email SMTP.
- **BE2 (Intern Profiles, Documents & Contracts)**: Quản lý hồ sơ thực tập sinh, tải lên và duyệt tài liệu (CV, đơn ứng tuyển), quản lý và xác nhận hợp đồng. Chi tiết xem [BE2 API Contract](docs/BE2_API_CONTRACT.md).
- **BE3 (Programs, Enrollments & Mentors)**: Quản lý chương trình thực tập theo phòng ban, ghi danh thực tập sinh, phân công và điều chuyển mentor, quản lý tải hướng dẫn. Chi tiết xem [BE3 API Contract](docs/BE3_API_CONTRACT.md) và [BE3 Sprint 2 Handoff](docs/BE3_SPRINT2_HANDOFF.md).
- **BE4 (Tasks, Reports & Evaluations)**: Giao việc, theo dõi tiến độ công việc, báo cáo tuần, đánh giá thực tập sinh và kiểm thử hồi quy. Chi tiết xem [BE4 Sprint 2 Handoff](docs/BE4_SPRINT2_HANDOFF.md).
- **FE1**: Giao diện đăng nhập, tài khoản admin, xem/xác nhận hợp đồng và lịch tổng quan TTS.
- **FE2**: Giao diện HR quản lý hồ sơ, tài liệu, hợp đồng, danh mục chương trình và ghi danh.
- **FE3**: Giao diện HR quản lý mentor, phân công/điều chuyển mentor và theo dõi workload. Chi tiết xem [FE3 Sprint 2 Handoff](docs/FE3_SPRINT2_HANDOFF.md).

---

## 2. Hướng dẫn cài đặt & Khởi chạy ứng dụng

### 2.1. Cấu hình Cơ sở dữ liệu (MySQL)
1. Mở MySQL Workbench hoặc phpMyAdmin (localhost:8080).
2. Trên database mới (`internship_management`), chạy lần lượt các script SQL:
   - `database/schema.sql`: Tạo database `internship_management`, các bảng `roles`, `users` của BE1.
   - `sql/schema_be2.sql`: Thêm `intern_profiles`, `applications`, `documents`, `contracts` của BE2.
   - `sql/schema_be3.sql`: Thêm phòng ban, chương trình, mentor, ghi danh và lịch sử phân mentor của BE3.
   - `sql/seed_data_qa.sql` (tùy chọn): Nạp dữ liệu mẫu phục vụ kiểm thử.
3. Sao chép `src/main/resources/db.properties.example` thành `src/main/resources/db.properties`, rồi cấu hình thông tin kết nối MySQL (host, port, user, password). File `db.properties` chỉ lưu local và không commit lên repository. Các DAO kết nối CSDL qua `DBContext`.

### 2.2. Build và Run với Apache Tomcat 10.1
- Dự án quản lý bằng **Maven**:
  ```bash
  mvn clean package
  ```
- Deploy file `target/internship-management-system.war` lên Apache Tomcat 10.1 (`webapps/`).
- Mở trình duyệt truy cập: `http://localhost:8080/internship-management-system/` (hoặc cổng cấu hình tương ứng).
