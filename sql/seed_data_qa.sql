-- ========================================================
-- SEED DATA DÀNH CHO QA TEST (BE2 - HỒ SƠ, TÀI LIỆU, HỢP ĐỒNG)
-- ========================================================

-- Thiết lập kết nối và session MySQL theo chuẩn UTF-8 (utf8mb4)
-- Tránh lỗi double-encoding (mojibake) khi import qua client/docker
SET NAMES 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';
SET CHARACTER SET utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

USE internship_management;

-- 1. Thêm Roles
INSERT INTO roles (role_id, role_name) VALUES 
(1, 'ADMIN'),
(2, 'HR'),
(3, 'MENTOR'),
(4, 'INTERN')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- 2. Thêm Users mẫu (Mật khẩu mặc định: '123456' đã hash bằng BCrypt)
-- Hash $2a$10$ifBNj/pgN7t50ik3utzxyOA9X9j4l0vnmgPhfpUQLhyFm.xgaImQ. tương đương "123456"
INSERT INTO users (user_id, role_id, username, password_hash, full_name, email, status) VALUES
(1, 2, 'hr_lan', '$2a$10$ifBNj/pgN7t50ik3utzxyOA9X9j4l0vnmgPhfpUQLhyFm.xgaImQ.', 'Nguyễn Thị Lan', 'hr_lan@company.com', 'ACTIVE'),
(2, 4, 'intern_nam', '$2a$10$ifBNj/pgN7t50ik3utzxyOA9X9j4l0vnmgPhfpUQLhyFm.xgaImQ.', 'Nguyễn Văn Nam', 'nam.nv@student.edu.vn', 'ACTIVE'),
(3, 4, 'intern_hoa', '$2a$10$ifBNj/pgN7t50ik3utzxyOA9X9j4l0vnmgPhfpUQLhyFm.xgaImQ.', 'Trần Thị Hoa', 'hoa.tt@student.edu.vn', 'ACTIVE'),
(4, 4, 'intern_tuan', '$2a$10$ifBNj/pgN7t50ik3utzxyOA9X9j4l0vnmgPhfpUQLhyFm.xgaImQ.', 'Lê Văn Tuấn', 'tuan.lv@student.edu.vn', 'ACTIVE')
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), password_hash=VALUES(password_hash), role_id=VALUES(role_id), status=VALUES(status);

-- 3. Thêm Intern Profiles mẫu
INSERT INTO intern_profiles (profile_id, user_id, full_name, dob, gender, phone, email, university, major, gpa, skills, status) VALUES
(1, 2, 'Nguyễn Văn Nam', '2003-05-15', 'Nam', '0912345678', 'nam.nv@student.edu.vn', 'Đại học Bách Khoa Hà Nội', 'Công nghệ thông tin', 3.45, 'Java, Servlet, MySQL, Spring Boot', 'ACTIVE'),
(2, 3, 'Trần Thị Hoa', '2003-10-20', 'Nữ', '0987654321', 'hoa.tt@student.edu.vn', 'Đại học Kinh Tế Quốc Dân', 'Hệ thống thông tin quản lý', 3.60, 'Python, SQL, PowerBI, Business Analysis', 'ACTIVE'),
(3, 4, 'Lê Văn Tuấn', '2004-01-08', 'Nam', '0901122334', 'tuan.lv@student.edu.vn', 'Đại học Công Nghệ - ĐHQGHN', 'Khoa học máy tính', 3.20, 'C++, Java, OOP, Data Structures', 'ACTIVE')
ON DUPLICATE KEY UPDATE 
    full_name = VALUES(full_name),
    dob = VALUES(dob),
    gender = VALUES(gender),
    phone = VALUES(phone),
    email = VALUES(email),
    university = VALUES(university),
    major = VALUES(major),
    gpa = VALUES(gpa),
    skills = VALUES(skills),
    status = VALUES(status);

-- 4. Thêm Applications mẫu
INSERT INTO applications (application_id, intern_id, status, submitted_at, reviewed_by, reviewed_at, reject_reason) VALUES
(1, 1, 'APPROVED', '2026-09-01 08:30:00', 1, '2026-09-03 10:00:00', NULL),
(2, 2, 'PENDING', '2026-09-10 14:15:00', NULL, NULL, NULL),
(3, 3, 'REJECTED', '2026-09-05 09:00:00', 1, '2026-09-07 16:30:00', 'Điểm GPA chưa đạt yêu cầu tối thiểu đợt 1')
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    reviewed_by = VALUES(reviewed_by),
    reviewed_at = VALUES(reviewed_at),
    reject_reason = VALUES(reject_reason);

-- 5. Thêm Documents mẫu (CV, Đơn xin thực tập)
INSERT INTO documents (document_id, profile_id, document_type, file_name, file_path, file_size, mime_type, status, reviewed_by, reviewed_at, reject_reason, uploaded_at) VALUES
(1, 1, 'CV', 'CV_NguyenVanNam_JavaBackend.pdf', 'uploads/documents/CV_NguyenVanNam_JavaBackend.pdf', 1048576, 'application/pdf', 'APPROVED', 1, '2026-09-02 09:00:00', NULL, '2026-09-01 08:35:00'),
(2, 1, 'APPLICATION_LETTER', 'DonXinThucTap_NguyenVanNam.pdf', 'uploads/documents/DonXinThucTap_NguyenVanNam.pdf', 524288, 'application/pdf', 'APPROVED', 1, '2026-09-02 09:05:00', NULL, '2026-09-01 08:36:00'),
(3, 2, 'CV', 'CV_TranThiHoa_BA.pdf', 'uploads/documents/CV_TranThiHoa_BA.pdf', 838860, 'application/pdf', 'PENDING', NULL, NULL, NULL, '2026-09-10 14:20:00')
ON DUPLICATE KEY UPDATE 
    file_name = VALUES(file_name),
    file_path = VALUES(file_path),
    file_size = VALUES(file_size),
    mime_type = VALUES(mime_type),
    status = VALUES(status),
    reviewed_by = VALUES(reviewed_by),
    reviewed_at = VALUES(reviewed_at),
    reject_reason = VALUES(reject_reason);

-- 6. Thêm Hợp đồng mẫu (Contracts)
INSERT INTO contracts (contract_id, application_id, file_name, file_path, file_size, mime_type, uploaded_by, uploaded_at, status, confirmed_at) VALUES
(1, 1, 'HopDongThucTap_NguyenVanNam_2026.pdf', 'uploads/contracts/HopDongThucTap_NguyenVanNam_2026.pdf', 2097152, 'application/pdf', 1, '2026-09-05 14:00:00', 'PENDING_CONFIRMATION', NULL)
ON DUPLICATE KEY UPDATE 
    file_name = VALUES(file_name),
    file_path = VALUES(file_path),
    file_size = VALUES(file_size),
    mime_type = VALUES(mime_type),
    uploaded_by = VALUES(uploaded_by),
    status = VALUES(status);
