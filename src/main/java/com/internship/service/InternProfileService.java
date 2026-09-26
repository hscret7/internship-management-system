package com.internship.service;

import com.internship.dao.InternProfileDAO;
import com.internship.model.InternProfile;

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
     * Cập nhật hồ sơ thực tập sinh.
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

        return profileDAO.updateProfile(profile);
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

    public boolean updateStatus(int profileId, String status) throws SQLException {
        return profileDAO.updateStatus(profileId, status);
    }
}
