package com.internship.service;

import com.internship.dao.InternProfileDAO;
import com.internship.model.InternProfile;
import com.internship.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * InternProfileService: Xử lý logic nghiệp vụ và validation dữ liệu hồ sơ thực tập sinh (BE2).
 */
public class InternProfileService {

    private final InternProfileDAO profileDAO;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)[0-9]{9,10}$");

    public InternProfileService() {
        this.profileDAO = new InternProfileDAO();
    }

    public InternProfileService(InternProfileDAO profileDAO) {
        this.profileDAO = profileDAO;
    }

    /**
     * Kiểm tra dữ liệu bắt buộc (Validation) trước khi lưu.
     * @return danh sách lỗi, nếu rỗng nghĩa là hợp lệ.
     */
    public List<String> validateProfile(InternProfile profile) {
        List<String> errors = new ArrayList<>();

        if (profile == null) {
            errors.add("Dữ liệu hồ sơ không được để trống.");
            return errors;
        }

        // 1. Kiểm tra Họ và tên
        if (profile.getFullName() == null || profile.getFullName().trim().isEmpty()) {
            errors.add("Họ và tên là thông tin bắt buộc.");
        } else if (profile.getFullName().trim().length() < 2 || profile.getFullName().trim().length() > 100) {
            errors.add("Họ và tên phải từ 2 đến 100 ký tự.");
        }

        // 2. Kiểm tra Email
        if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
            errors.add("Email là thông tin bắt buộc.");
        } else if (!EMAIL_PATTERN.matcher(profile.getEmail().trim()).matches()) {
            errors.add("Địa chỉ email không đúng định dạng.");
        }

        // 3. Kiểm tra Số điện thoại
        if (profile.getPhone() == null || profile.getPhone().trim().isEmpty()) {
            errors.add("Số điện thoại là thông tin bắt buộc.");
        } else if (!PHONE_PATTERN.matcher(profile.getPhone().trim()).matches()) {
            errors.add("Số điện thoại không hợp lệ (cần 10 chữ số, bắt đầu bằng 0 hoặc +84).");
        }

        // 4. Kiểm tra Trường & Ngành học
        if (profile.getUniversity() == null || profile.getUniversity().trim().isEmpty()) {
            errors.add("Tên trường đại học/cao đẳng là bắt buộc.");
        }
        if (profile.getMajor() == null || profile.getMajor().trim().isEmpty()) {
            errors.add("Chuyên ngành là bắt buộc.");
        }

        // 5. Kiểm tra GPA (nếu có nhập)
        if (profile.getGpa() != null) {
            if (profile.getGpa() < 0.0 || profile.getGpa() > 4.0) {
                errors.add("Điểm GPA hệ 4 phải nằm trong khoảng từ 0.0 đến 4.0.");
            }
        }

        return errors;
    }

    /**
     * Tạo mới hồ sơ thực tập sinh.
     */
    public int createProfile(InternProfile profile) throws IllegalArgumentException, SQLException {
        List<String> errors = validateProfile(profile);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        // Kiểm tra xem User này đã có profile chưa
        InternProfile existing = profileDAO.getProfileByUserId(profile.getUserId());
        if (existing != null) {
            throw new IllegalArgumentException("Người dùng này đã có hồ sơ thực tập sinh.");
        }

        return profileDAO.createProfile(profile);
    }

    /**
     * TTS tự tạo mới hồ sơ cá nhân có kiểm tra role.
     */
    public int createProfile(InternProfile profile, User currentUser) 
            throws IllegalArgumentException, IllegalAccessException, SQLException {
        if (currentUser == null || !DocumentService.isIntern(currentUser)) {
            throw new IllegalAccessException("Chỉ thực tập sinh mới có quyền tạo hồ sơ cá nhân.");
        }
        profile.setUserId(currentUser.getUserId());
        profile.setStatus("ACTIVE");
        return createProfile(profile);
    }

    /**
     * Cập nhật hồ sơ thực tập sinh (bảo vệ quyền sở hữu).
     */
    public boolean updateProfile(InternProfile profile, User currentUser) 
            throws IllegalArgumentException, IllegalAccessException, SQLException {
        if (currentUser == null) {
            throw new IllegalAccessException("Vui lòng đăng nhập để thực hiện thao tác.");
        }
        if (!DocumentService.isIntern(currentUser)) {
            throw new IllegalAccessException("Chỉ thực tập sinh mới có quyền cập nhật hồ sơ cá nhân.");
        }
        if (profile == null || profile.getProfileId() <= 0) {
            throw new IllegalArgumentException("Mã hồ sơ không hợp lệ.");
        }

        InternProfile existing = profileDAO.getProfileById(profile.getProfileId());
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy hồ sơ có ID: " + profile.getProfileId());
        }

        // Kiểm tra quyền sở hữu (chống IDOR)
        if (existing.getUserId() != currentUser.getUserId()) {
            throw new IllegalAccessException("Bạn không có quyền chỉnh sửa hồ sơ của người khác.");
        }

        // Bảo vệ các trường bất biến: userId và status giữ nguyên từ existing
        profile.setUserId(existing.getUserId());
        profile.setStatus(existing.getStatus());

        List<String> errors = validateProfile(profile);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        return profileDAO.updateProfile(profile);
    }

    /**
     * Cập nhật hồ sơ thực tập sinh (overload giữ tương thích).
     */
    public boolean updateProfile(InternProfile profile) throws IllegalArgumentException, SQLException {
        List<String> errors = validateProfile(profile);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        InternProfile existing = profileDAO.getProfileById(profile.getProfileId());
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy hồ sơ có ID: " + profile.getProfileId());
        }

        if (profile.getUserId() > 0 && existing.getUserId() != profile.getUserId()) {
            throw new IllegalArgumentException("Không có quyền chỉnh sửa hồ sơ của người dùng khác.");
        }
        profile.setUserId(existing.getUserId());
        if (profile.getStatus() == null) {
            profile.setStatus(existing.getStatus());
        }

        return profileDAO.updateProfile(profile);
    }

    /**
     * HR/Admin chỉnh sửa hồ sơ thực tập sinh (US02 - Sprint 2).
     * Tuân thủ quy ước 4:
     * - Whitelist các trường thông tin: fullName, dob, gender, phone, email, university, major, gpa, skills.
     * - KHÔNG nhận hay thay đổi userId, role, trạng thái Application từ form.
     * - profileId phải tồn tại trong CSDL.
     */
    public boolean updateProfileByHr(InternProfile input, User hrUser) 
            throws IllegalArgumentException, IllegalAccessException, SQLException {
        if (!DocumentService.isHR(hrUser) && !DocumentService.isAdmin(hrUser)) {
            throw new IllegalAccessException("Chỉ HR hoặc Admin mới có quyền chỉnh sửa hồ sơ thực tập sinh.");
        }

        if (input == null || input.getProfileId() <= 0) {
            throw new IllegalArgumentException("Mã hồ sơ (profileId) không hợp lệ.");
        }

        InternProfile existing = profileDAO.getProfileById(input.getProfileId());
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy hồ sơ có ID: " + input.getProfileId());
        }

        // Áp dụng whitelist an toàn: chỉ cập nhật thông tin cá nhân và học vấn
        existing.setFullName(input.getFullName() != null ? input.getFullName().trim() : null);
        existing.setDob(input.getDob());
        if (input.getGender() != null && !input.getGender().trim().isEmpty()) {
            existing.setGender(input.getGender().trim());
        }
        existing.setPhone(input.getPhone() != null ? input.getPhone().trim() : null);
        existing.setEmail(input.getEmail() != null ? input.getEmail().trim() : null);
        existing.setUniversity(input.getUniversity() != null ? input.getUniversity().trim() : null);
        existing.setMajor(input.getMajor() != null ? input.getMajor().trim() : null);
        existing.setGpa(input.getGpa());
        existing.setSkills(input.getSkills());

        // Validate lại toàn bộ dữ liệu trước khi lưu
        List<String> errors = validateProfile(existing);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        return profileDAO.updateProfile(existing);
    }

    public InternProfile getProfileById(int profileId) throws SQLException {
        return profileDAO.getProfileById(profileId);
    }

    public InternProfile getProfileByUserId(long userId) throws SQLException {
        return profileDAO.getProfileByUserId(userId);
    }

    public List<InternProfile> getAllProfiles() throws SQLException {
        return profileDAO.getAllProfiles();
    }

    /**
     * Tìm kiếm hồ sơ theo tên, trường, ngành (Yêu cầu BE2).
     */
    public List<InternProfile> searchProfiles(String name, String university, String major) throws SQLException {
        return profileDAO.searchProfiles(name, university, major);
    }

    /**
     * Tìm kiếm và lọc hồ sơ theo tên, trường, ngành và trạng thái (Hỗ trợ mở rộng cho FE2 / QA TC2).
     */
    public List<InternProfile> searchProfiles(String name, String university, String major, String status) throws SQLException {
        return profileDAO.searchProfiles(name, university, major, status);
    }

    public boolean updateStatus(int profileId, String status) throws SQLException {
        return profileDAO.updateStatus(profileId, status);
    }
}
