package com.internship.service;

import com.internship.dao.*;
import com.internship.model.*;
import com.internship.util.DBContext;
import com.internship.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * EnrollmentService: Nghiệp vụ ghi danh và phân mentor (BE3).
 *
 * <p>Các thao tác nhiều bước dùng transaction để tránh trạng thái dở dang
 * hoặc gán trùng khi hai yêu cầu đến cùng lúc.</p>
 */
public class EnrollmentService {

    private static final Logger LOGGER = Logger.getLogger(EnrollmentService.class.getName());

    private final ApplicationDAO applicationDAO;
    private final ProgramEnrollmentDAO enrollmentDAO;
    private final MentorAssignmentDAO mentorAssignmentDAO;
    private final InternshipProgramDAO programDAO;
    private final MentorDAO mentorDAO;
    private final ConnectionProvider connections;

    public EnrollmentService() {
        this(new ApplicationDAO(), new ProgramEnrollmentDAO(), new MentorAssignmentDAO(),
                new InternshipProgramDAO(), new MentorDAO(), DBContext::getConnection);
    }

    /**
     * Constructor cho test – inject DAO giả.
     */
    public EnrollmentService(ApplicationDAO applicationDAO,
                             ProgramEnrollmentDAO enrollmentDAO,
                             MentorAssignmentDAO mentorAssignmentDAO,
                             InternshipProgramDAO programDAO,
                             MentorDAO mentorDAO) {
        this(applicationDAO, enrollmentDAO, mentorAssignmentDAO, programDAO, mentorDAO,
                DBContext::getConnection);
    }

    public EnrollmentService(ApplicationDAO applicationDAO,
                             ProgramEnrollmentDAO enrollmentDAO,
                             MentorAssignmentDAO mentorAssignmentDAO,
                             InternshipProgramDAO programDAO,
                             MentorDAO mentorDAO,
                             ConnectionProvider connections) {
        this.applicationDAO = applicationDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.mentorAssignmentDAO = mentorAssignmentDAO;
        this.programDAO = programDAO;
        this.mentorDAO = mentorDAO;
        this.connections = connections;
    }

    // ========== Ghi danh TTS vào chương trình ==========

    /**
     * HR ghi danh TTS vào chương trình.
     *
     * @param applicationId ID đơn đã APPROVED
     * @param programId     ID chương trình
     * @param hrUserId      user_id HR từ session (dùng cho audit, không lưu trực tiếp)
     * @return enrollmentId
     * @throws IllegalStateException nếu vi phạm nghiệp vụ
     */
    public int enrollIntern(int applicationId, int programId, long hrUserId) throws SQLException {
        try (Connection conn = connections.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            try {
                // 1. Kiểm tra Application tồn tại và APPROVED
                Application app = applicationDAO.getApplicationByIdForUpdate(applicationId, conn);
                if (app == null) {
                    throw new IllegalStateException("Đơn ứng tuyển không tồn tại.");
                }
                if (!"APPROVED".equals(app.getStatus())) {
                    throw new IllegalStateException(
                            "Chỉ có thể ghi danh đơn đã được duyệt (APPROVED). Trạng thái hiện tại: "
                                    + app.getStatus());
                }

                // Lock the profile shared by all of this intern's applications.
                enrollmentDAO.lockInternProfile(app.getInternId(), conn);

                // 2. Kiểm tra Application chưa có Enrollment (UNIQUE)
                if (enrollmentDAO.hasEnrollmentForApplication(applicationId, conn)) {
                    throw new IllegalStateException("Đơn này đã được ghi danh vào chương trình.");
                }

                // 3. Kiểm tra TTS chưa có Enrollment ACTIVE
                if (enrollmentDAO.hasActiveEnrollment(app.getInternId(), conn)) {
                    throw new IllegalStateException(
                            "Thực tập sinh đang tham gia một chương trình khác.");
                }

                // 4. Kiểm tra Program tồn tại và ACTIVE
                programDAO.lockProgram(programId, conn);
                InternshipProgram program = programDAO.getProgramById(programId, conn);
                if (program == null) {
                    throw new IllegalStateException("Chương trình thực tập không tồn tại.");
                }
                if (!"ACTIVE".equals(program.getStatus())) {
                    throw new IllegalStateException("Chương trình không ở trạng thái hoạt động.");
                }

                // 5. Kiểm tra giới hạn số lượng (nếu có)
                if (program.getMaxInterns() > 0) {
                    int current = programDAO.countActiveEnrollments(programId, conn);
                    if (current >= program.getMaxInterns()) {
                        throw new IllegalStateException(
                                "Chương trình đã đạt giới hạn số lượng thực tập sinh ("
                                        + program.getMaxInterns() + ").");
                    }
                }

                // 6. Tạo Enrollment
                ProgramEnrollment enrollment = new ProgramEnrollment();
                enrollment.setApplicationId(applicationId);
                enrollment.setProgramId(programId);
                enrollment.setInternProfileId(app.getInternId());

                int enrollmentId = enrollmentDAO.createEnrollment(enrollment, conn);
                if (enrollmentId < 0) {
                    throw new SQLException("Không thể tạo enrollment.");
                }

                conn.commit();
                LOGGER.info("Enrolled intern (profileId=" + app.getInternId()
                        + ") into program " + programId
                        + " (enrollmentId=" + enrollmentId + ") by HR userId=" + hrUserId);
                return enrollmentId;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ========== Phân mentor ==========

    /**
     * HR phân mentor cho enrollment.
     *
     * @param enrollmentId ID enrollment
     * @param mentorId     ID mentor
     * @param hrUserId     user_id HR từ session
     * @return assignmentId
     * @throws IllegalStateException nếu enrollment không ACTIVE, mentor không khả dụng,
     *                               hoặc enrollment đã có mentor đang hiệu lực
     */
    public int assignMentor(int enrollmentId, int mentorId, long hrUserId) throws SQLException {
        try (Connection conn = connections.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            try {
                // 1. Kiểm tra enrollment tồn tại và ACTIVE
                ProgramEnrollment enrollment = enrollmentDAO.getEnrollmentByIdForUpdate(enrollmentId, conn);
                if (enrollment == null) {
                    throw new IllegalStateException("Enrollment không tồn tại.");
                }
                if (!"ACTIVE".equals(enrollment.getStatus())) {
                    throw new IllegalStateException("Enrollment không ở trạng thái ACTIVE.");
                }

                // 2. Kiểm tra chưa có mentor đang hiệu lực
                if (mentorAssignmentDAO.hasActiveAssignment(enrollmentId, conn)) {
                    throw new IllegalStateException(
                            "Enrollment đã có mentor đang hiệu lực. Hãy đổi mentor thay vì gán mới.");
                }

                // 3. Kiểm tra mentor tồn tại và ACTIVE
                mentorDAO.lockMentor(mentorId, conn);
                Mentor mentor = mentorDAO.getMentorById(mentorId, conn);
                if (mentor == null) {
                    throw new IllegalStateException("Mentor không tồn tại.");
                }
                if (!"ACTIVE".equals(mentor.getStatus())) {
                    throw new IllegalStateException("Mentor không ở trạng thái hoạt động.");
                }
                requireMatchingDepartment(enrollment, mentor, conn);

                // 4. Kiểm tra giới hạn mentee
                if (mentor.getCurrentMenteeCount() >= mentor.getMaxMentees()) {
                    throw new IllegalStateException(
                            "Mentor đã đạt giới hạn số lượng mentee ("
                                    + mentor.getMaxMentees() + ").");
                }

                // 5. Tạo assignment
                MentorAssignment assignment = new MentorAssignment();
                assignment.setEnrollmentId(enrollmentId);
                assignment.setMentorId(mentorId);
                assignment.setAssignedBy(hrUserId);

                int assignmentId = mentorAssignmentDAO.createAssignment(assignment, conn);
                if (assignmentId < 0) {
                    throw new SQLException("Không thể tạo mentor assignment.");
                }

                conn.commit();
                LOGGER.info("Assigned mentor " + mentorId + " to enrollment "
                        + enrollmentId + " (assignmentId=" + assignmentId
                        + ") by HR userId=" + hrUserId);
                return assignmentId;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ========== Đổi mentor ==========

    /**
     * HR đổi mentor cho enrollment.
     * Kết thúc MentorAssignment cũ rồi tạo bản ghi mới.
     *
     * @param enrollmentId ID enrollment
     * @param newMentorId  ID mentor mới
     * @param endReason    lý do đổi mentor
     * @param hrUserId     user_id HR từ session
     * @return assignmentId mới
     */
    public int changeMentor(int enrollmentId, int newMentorId, String endReason, long hrUserId)
            throws SQLException {
        if (endReason == null || endReason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do đổi mentor.");
        }
        if (endReason.trim().length() > 200) {
            throw new IllegalArgumentException("Lý do đổi mentor vượt quá 200 ký tự.");
        }

        try (Connection conn = connections.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            try {
                // 1. Kiểm tra enrollment
                ProgramEnrollment enrollment = enrollmentDAO.getEnrollmentByIdForUpdate(enrollmentId, conn);
                if (enrollment == null) {
                    throw new IllegalStateException("Enrollment không tồn tại.");
                }
                if (!"ACTIVE".equals(enrollment.getStatus())) {
                    throw new IllegalStateException("Enrollment không ở trạng thái ACTIVE.");
                }

                MentorAssignment oldAssignment = mentorAssignmentDAO.getActiveAssignment(enrollmentId, conn);
                if (oldAssignment == null) {
                    throw new IllegalStateException("Enrollment chưa có mentor để đổi.");
                }
                if (oldAssignment.getMentorId() == newMentorId) {
                    throw new IllegalArgumentException("Mentor mới phải khác mentor hiện tại.");
                }

                // 3. Kiểm tra mentor mới trước khi kết thúc assignment cũ.
                mentorDAO.lockMentor(newMentorId, conn);
                Mentor newMentor = mentorDAO.getMentorById(newMentorId, conn);
                if (newMentor == null) {
                    throw new IllegalStateException("Mentor mới không tồn tại.");
                }
                if (!"ACTIVE".equals(newMentor.getStatus())) {
                    throw new IllegalStateException("Mentor mới không ở trạng thái hoạt động.");
                }
                requireMatchingDepartment(enrollment, newMentor, conn);
                if (newMentor.getCurrentMenteeCount() >= newMentor.getMaxMentees()) {
                    throw new IllegalStateException(
                            "Mentor mới đã đạt giới hạn số lượng mentee.");
                }

                if (!mentorAssignmentDAO.endActiveAssignment(enrollmentId, endReason.trim(), conn)) {
                    throw new IllegalStateException("Mentor hiện tại đã thay đổi. Vui lòng tải lại trang.");
                }

                // 4. Tạo assignment mới
                MentorAssignment newAssignment = new MentorAssignment();
                newAssignment.setEnrollmentId(enrollmentId);
                newAssignment.setMentorId(newMentorId);
                newAssignment.setAssignedBy(hrUserId);

                int assignmentId = mentorAssignmentDAO.createAssignment(newAssignment, conn);
                if (assignmentId < 0) {
                    throw new SQLException("Không thể tạo mentor assignment mới.");
                }

                conn.commit();
                LOGGER.info("Changed mentor for enrollment " + enrollmentId
                        + " to mentor " + newMentorId + " by HR userId=" + hrUserId);
                return assignmentId;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ========== Xem thông tin ==========

    public ProgramEnrollment getEnrollmentById(int enrollmentId) throws SQLException {
        return enrollmentDAO.getEnrollmentById(enrollmentId);
    }

    public List<ProgramEnrollment> getAllEnrollments() throws SQLException {
        return enrollmentDAO.getAllEnrollments();
    }

    public List<ProgramEnrollment> getEnrollmentsByProgram(int programId) throws SQLException {
        return enrollmentDAO.getEnrollmentsByProgram(programId);
    }

    public ProgramEnrollment getActiveEnrollmentByIntern(int internProfileId) throws SQLException {
        return enrollmentDAO.getActiveEnrollmentByIntern(internProfileId);
    }

    /**
     * Lấy mentor hiện hành cho enrollment.
     * Đây là method mà BE4 sẽ dùng.
     */
    public MentorAssignment getCurrentMentor(int enrollmentId) throws SQLException {
        return mentorAssignmentDAO.getActiveAssignment(enrollmentId);
    }

    public List<MentorAssignment> getMentorHistory(int enrollmentId) throws SQLException {
        return mentorAssignmentDAO.getAssignmentHistory(enrollmentId);
    }

    private void requireMatchingDepartment(ProgramEnrollment enrollment, Mentor mentor,
                                           Connection conn) throws SQLException {
        InternshipProgram program = programDAO.getProgramById(enrollment.getProgramId(), conn);
        if (program == null || mentor.getDepartmentId() == null
                || mentor.getDepartmentId() != program.getDepartmentId()) {
            throw new IllegalStateException("Mentor phải thuộc phòng ban của chương trình.");
        }
    }
}
