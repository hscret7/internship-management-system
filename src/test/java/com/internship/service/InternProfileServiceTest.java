package com.internship.service;

import com.internship.dao.InternProfileDAO;
import com.internship.model.InternProfile;
import com.internship.model.Role;
import com.internship.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho InternProfileService (US02, US03 - BE2).
 */
class InternProfileServiceTest {

    static class StubInternProfileDAO extends InternProfileDAO {
        final List<InternProfile> database = new ArrayList<>();
        int nextId = 1;

        @Override
        public int createProfile(InternProfile profile) throws SQLException {
            profile.setProfileId(nextId++);
            database.add(profile);
            return profile.getProfileId();
        }

        @Override
        public boolean updateProfile(InternProfile profile) throws SQLException {
            for (int i = 0; i < database.size(); i++) {
                if (database.get(i).getProfileId() == profile.getProfileId()) {
                    database.set(i, profile);
                    return true;
                }
            }
            return false;
        }

        @Override
        public InternProfile getProfileById(int profileId) throws SQLException {
            return database.stream()
                    .filter(p -> p.getProfileId() == profileId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public InternProfile getProfileByUserId(long userId) throws SQLException {
            return database.stream()
                    .filter(p -> p.getUserId() == userId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<InternProfile> getAllProfiles() throws SQLException {
            return new ArrayList<>(database);
        }

        @Override
        public List<InternProfile> searchProfiles(String name, String university, String major) throws SQLException {
            return database.stream().filter(p -> {
                boolean matchName = (name == null || name.trim().isEmpty())
                        || (p.getFullName() != null && p.getFullName().toLowerCase().contains(name.trim().toLowerCase()));
                boolean matchUni = (university == null || university.trim().isEmpty())
                        || (p.getUniversity() != null && p.getUniversity().toLowerCase().contains(university.trim().toLowerCase()));
                boolean matchMajor = (major == null || major.trim().isEmpty())
                        || (p.getMajor() != null && p.getMajor().toLowerCase().contains(major.trim().toLowerCase()));
                return matchName && matchUni && matchMajor;
            }).toList();
        }

        @Override
        public boolean updateStatus(int profileId, String status) throws SQLException {
            InternProfile p = getProfileById(profileId);
            if (p != null) {
                p.setStatus(status);
                return true;
            }
            return false;
        }
    }

    private StubInternProfileDAO stubDAO;
    private InternProfileService service;
    private User hrUser;
    private User adminUser;
    private User internUser;

    @BeforeEach
    void setUp() {
        stubDAO = new StubInternProfileDAO();
        service = new InternProfileService(stubDAO);

        Role hrRole = new Role();
        hrRole.setRoleId(2L);
        hrRole.setRoleName("HR");
        hrUser = new User();
        hrUser.setUserId(10L);
        hrUser.setRole(hrRole);

        Role adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName("ADMIN");
        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setRole(adminRole);

        Role internRole = new Role();
        internRole.setRoleId(4L);
        internRole.setRoleName("INTERN");
        internUser = new User();
        internUser.setUserId(20L);
        internUser.setRole(internRole);
    }

    private InternProfile createValidProfile(int profileId, long userId, String name, String email) {
        InternProfile p = new InternProfile();
        p.setProfileId(profileId);
        p.setUserId(userId);
        p.setFullName(name);
        p.setEmail(email);
        p.setPhone("0912345678");
        p.setUniversity("Đại học Bách Khoa");
        p.setMajor("Công nghệ thông tin");
        p.setGpa(3.5);
        p.setStatus("ACTIVE");
        return p;
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {
        @Test
        @DisplayName("Hồ sơ hợp lệ không có lỗi")
        void testValidProfile() {
            InternProfile p = createValidProfile(1, 20L, "Nguyễn Văn Nam", "nam@student.edu.vn");
            List<String> errors = service.validateProfile(p);
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Họ và tên rỗng hoặc quá ngắn báo lỗi")
        void testInvalidName() {
            InternProfile p = createValidProfile(1, 20L, "", "nam@student.edu.vn");
            List<String> errors = service.validateProfile(p);
            assertFalse(errors.isEmpty());

            p.setFullName("A");
            errors = service.validateProfile(p);
            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("Email và số điện thoại sai định dạng báo lỗi")
        void testInvalidEmailAndPhone() {
            InternProfile p = createValidProfile(1, 20L, "Nguyễn Văn Nam", "sai-email");
            p.setPhone("12345");
            List<String> errors = service.validateProfile(p);
            assertEquals(2, errors.size());
        }

        @Test
        @DisplayName("GPA ngoài khoảng 0.0 - 4.0 báo lỗi")
        void testInvalidGpa() {
            InternProfile p = createValidProfile(1, 20L, "Nguyễn Văn Nam", "nam@student.edu.vn");
            p.setGpa(4.5);
            List<String> errors = service.validateProfile(p);
            assertFalse(errors.isEmpty());

            p.setGpa(-0.5);
            errors = service.validateProfile(p);
            assertFalse(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("US02: HR Chỉnh sửa hồ sơ TTS")
    class HrEditProfileTests {
        @Test
        @DisplayName("HR cập nhật hồ sơ TTS thành công với thông tin whitelist")
        void testHrUpdateSuccess() throws Exception {
            InternProfile original = createValidProfile(1, 20L, "Nguyễn Văn Nam", "nam@student.edu.vn");
            stubDAO.database.add(original);

            InternProfile updateData = new InternProfile();
            updateData.setProfileId(1);
            updateData.setFullName("Nguyễn Văn Nam (Đã cập nhật)");
            updateData.setEmail("nam.updated@student.edu.vn");
            updateData.setPhone("0987654321");
            updateData.setUniversity("Đại học Quốc Gia Hà Nội");
            updateData.setMajor("Khoa học máy tính");
            updateData.setGpa(3.8);
            updateData.setSkills("Java, Spring Boot, MySQL, Docker");

            boolean ok = service.updateProfileByHr(updateData, hrUser);
            assertTrue(ok);

            InternProfile stored = stubDAO.getProfileById(1);
            assertEquals("Nguyễn Văn Nam (Đã cập nhật)", stored.getFullName());
            assertEquals("nam.updated@student.edu.vn", stored.getEmail());
            assertEquals("0987654321", stored.getPhone());
            assertEquals("Đại học Quốc Gia Hà Nội", stored.getUniversity());
            assertEquals("Khoa học máy tính", stored.getMajor());
            assertEquals(3.8, stored.getGpa());
            // Bảo toàn userId và status gốc
            assertEquals(20L, stored.getUserId());
            assertEquals("ACTIVE", stored.getStatus());
        }

        @Test
        @DisplayName("Admin cập nhật hồ sơ TTS thành công")
        void testAdminUpdateSuccess() throws Exception {
            InternProfile original = createValidProfile(2, 21L, "Trần Thị Hoa", "hoa@student.edu.vn");
            stubDAO.database.add(original);

            InternProfile updateData = new InternProfile();
            updateData.setProfileId(2);
            updateData.setFullName("Trần Thị Hoa Updated");
            updateData.setEmail("hoa@student.edu.vn");
            updateData.setPhone("0912345678");
            updateData.setUniversity("ĐH Kinh Tế");
            updateData.setMajor("Hệ thống thông tin");

            boolean ok = service.updateProfileByHr(updateData, adminUser);
            assertTrue(ok);
        }

        @Test
        @DisplayName("Vai trò không phải HR hoặc Admin bị từ chối quyền")
        void testNonHrForbidden() {
            InternProfile original = createValidProfile(1, 20L, "Nguyễn Văn Nam", "nam@student.edu.vn");
            stubDAO.database.add(original);

            InternProfile updateData = new InternProfile();
            updateData.setProfileId(1);
            updateData.setFullName("Tên Mới");

            assertThrows(IllegalAccessException.class, () ->
                    service.updateProfileByHr(updateData, internUser));
        }

        @Test
        @DisplayName("profileId không tồn tại báo lỗi")
        void testProfileNotFound() {
            InternProfile updateData = new InternProfile();
            updateData.setProfileId(999);
            updateData.setFullName("Không Tồn Tại");

            assertThrows(IllegalArgumentException.class, () ->
                    service.updateProfileByHr(updateData, hrUser));
        }
    }

    @Nested
    @DisplayName("US03: HR Tìm kiếm và Lọc TTS")
    class SearchProfilesTests {
        @BeforeEach
        void initData() {
            stubDAO.database.add(createValidProfile(1, 20L, "Nguyễn Văn Nam", "nam@bk.edu.vn"));
            stubDAO.database.add(createValidProfile(2, 21L, "Trần Thị Hoa", "hoa@neu.edu.vn"));
            stubDAO.database.get(1).setUniversity("Đại học Kinh Tế Quốc Dân");
            stubDAO.database.get(1).setMajor("Quản trị kinh doanh");

            stubDAO.database.add(createValidProfile(3, 22L, "Lê Văn Tuấn", "tuan@vnu.edu.vn"));
            stubDAO.database.get(2).setUniversity("Đại học Công Nghệ");
            stubDAO.database.get(2).setMajor("Công nghệ thông tin");
        }

        @Test
        @DisplayName("Lọc theo tên thành công")
        void testSearchByName() throws Exception {
            List<InternProfile> result = service.searchProfiles("Nam", null, null);
            assertEquals(1, result.size());
            assertEquals("Nguyễn Văn Nam", result.get(0).getFullName());
        }

        @Test
        @DisplayName("Lọc theo trường thành công")
        void testSearchByUniversity() throws Exception {
            List<InternProfile> result = service.searchProfiles(null, "Kinh Tế", null);
            assertEquals(1, result.size());
            assertEquals("Trần Thị Hoa", result.get(0).getFullName());
        }

        @Test
        @DisplayName("Lọc theo ngành thành công")
        void testSearchByMajor() throws Exception {
            List<InternProfile> result = service.searchProfiles(null, null, "Công nghệ thông tin");
            assertEquals(2, result.size()); // Nam và Tuấn
        }

        @Test
        @DisplayName("Lọc kết hợp tên, trường và ngành")
        void testSearchCombined() throws Exception {
            List<InternProfile> result = service.searchProfiles("Tuấn", "Công Nghệ", "Công nghệ thông tin");
            assertEquals(1, result.size());
            assertEquals("Lê Văn Tuấn", result.get(0).getFullName());
        }

        @Test
        @DisplayName("Tìm kiếm với điều kiện rỗng trả về toàn bộ")
        void testSearchEmptyCriteria() throws Exception {
            List<InternProfile> result = service.searchProfiles("", "  ", null);
            assertEquals(3, result.size());
        }
    }
}
