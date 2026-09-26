package com.internship.service;

import com.internship.dao.ApplicationDAO;
import com.internship.dao.InternProfileDAO;
import com.internship.model.Application;
import com.internship.model.InternProfile;
import com.internship.service.email.EmailService;
import com.internship.util.DBContext;
import com.internship.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ApplicationService: Nghiệp vụ nộp và xét duyệt đơn ứng tuyển (BE3).
 *
 * <p>EmailService được inject qua constructor để hỗ trợ test với mock.
 * Lỗi gửi email được log nhưng KHÔNG làm mất quyết định đã lưu.</p>
 */
public class ApplicationService {

    private static final Logger LOGGER = Logger.getLogger(ApplicationService.class.getName());

    private final ApplicationDAO applicationDAO;
    private final InternProfileDAO internProfileDAO;
    private final EmailService emailService;
    private final ConnectionProvider connections;

    /**
     * Constructor chính – production sử dụng SmtpEmailService.
     */
    public ApplicationService(EmailService emailService) {
        this(new ApplicationDAO(), new InternProfileDAO(), emailService, DBContext::getConnection);
    }

    /**
     * Constructor cho test – inject DAO giả.
     */
    public ApplicationService(ApplicationDAO applicationDAO,
                              InternProfileDAO internProfileDAO,
                              EmailService emailService) {
        this(applicationDAO, internProfileDAO, emailService, DBContext::getConnection);
    }

    public ApplicationService(ApplicationDAO applicationDAO,
                              InternProfileDAO internProfileDAO,
                              EmailService emailService,
                              ConnectionProvider connections) {
        this.applicationDAO = applicationDAO;
        this.internProfileDAO = internProfileDAO;
        this.emailService = emailService;
        this.connections = connections;
    }

    // ========== TTS: Nộp đơn ứng tuyển ==========

    /**
     * TTS nộp đơn ứng tuyển.
     *
     * @param userId user_id từ session (BIGINT)
     * @return applicationId nếu thành công
     * @throws IllegalStateException nếu chưa có hồ sơ hoặc đã có đơn PENDING
     */
    public int submitApplication(long userId) throws SQLException {
        if (userId <= 0) throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        try (Connection conn = connections.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            try {
                // Lock the existing profile row so concurrent submissions cannot both pass.
                Integer profileId = applicationDAO.lockInternProfileId(userId, conn);
                if (profileId == null) {
                    throw new IllegalStateException("Bạn cần tạo hồ sơ thực tập trước khi nộp đơn.");
                }
                if (applicationDAO.hasPendingApplication(profileId, conn)) {
                    throw new IllegalStateException("Bạn đã có đơn ứng tuyển đang chờ xét duyệt.");
                }
                int id = applicationDAO.createApplication(profileId, conn);
                if (id <= 0) throw new SQLException("Không thể tạo đơn ứng tuyển.");
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ========== TTS: Xem đơn của mình ==========

    public List<Application> getMyApplications(long userId) throws SQLException {
        InternProfile profile = internProfileDAO.getProfileByUserId(userId);
        if (profile == null) {
            return List.of();
        }
        return applicationDAO.getApplicationsByInternId(profile.getProfileId());
    }

    // ========== HR: Xem danh sách đơn ==========

    public List<Application> getPendingApplications() throws SQLException {
        return applicationDAO.getPendingApplications();
    }

    public List<Application> getAllApplications() throws SQLException {
        return applicationDAO.getAllApplications();
    }

    public Application getApplicationById(int applicationId) throws SQLException {
        return applicationDAO.getApplicationById(applicationId);
    }

    // ========== HR: Duyệt đơn ==========

    /**
     * HR duyệt đơn.
     *
     * @param applicationId ID đơn cần duyệt
     * @param reviewerId    user_id HR từ session
     * @throws IllegalStateException nếu đơn không tồn tại, không phải PENDING,
     *                               hoặc đã được xử lý bởi request khác
     */
    public void approveApplication(int applicationId, long reviewerId) throws SQLException {
        try (Connection conn = connections.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean updated = applicationDAO.approveApplication(applicationId, reviewerId, conn);
                if (!updated) {
                    throw new IllegalStateException(
                            "Đơn không tồn tại hoặc không còn ở trạng thái PENDING.");
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }

        // Gửi email SAU KHI commit thành công
        sendDecisionEmail(applicationId, true);
    }

    // ========== HR: Từ chối đơn ==========

    /**
     * HR từ chối đơn.
     *
     * @param applicationId ID đơn cần từ chối
     * @param reviewerId    user_id HR từ session
     * @param rejectReason  lý do từ chối (bắt buộc)
     * @throws IllegalArgumentException nếu thiếu lý do từ chối
     * @throws IllegalStateException    nếu đơn không tồn tại hoặc không PENDING
     */
    public void rejectApplication(int applicationId, long reviewerId, String rejectReason)
            throws SQLException {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        }

        try (Connection conn = connections.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean updated = applicationDAO.rejectApplication(
                        applicationId, reviewerId, rejectReason.trim(), conn);
                if (!updated) {
                    throw new IllegalStateException(
                            "Đơn không tồn tại hoặc không còn ở trạng thái PENDING.");
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }

        // Gửi email SAU KHI commit thành công
        sendDecisionEmail(applicationId, false);
    }

    // ========== Email notification ==========

    /**
     * Gửi email thông báo kết quả.
     * Lỗi email KHÔNG làm mất quyết định đã lưu.
     */
    private void sendDecisionEmail(int applicationId, boolean approved) {
        try {
            Application app = applicationDAO.getApplicationById(applicationId);
            if (app == null || app.getInternEmail() == null || app.getInternEmail().isBlank()) {
                LOGGER.warning("Không thể gửi email: thiếu thông tin đơn hoặc email TTS"
                        + " (applicationId=" + applicationId + ")");
                return;
            }

            String subject;
            String content;

            if (approved) {
                subject = "[IMS] Đơn ứng tuyển của bạn đã được duyệt";
                content = "Xin chào " + app.getInternName() + ",\n\n"
                        + "Đơn ứng tuyển của bạn (mã đơn: " + applicationId + ") đã được DUYỆT.\n"
                        + "Vui lòng theo dõi hệ thống để biết các bước tiếp theo.\n\n"
                        + "Trân trọng,\nInternship Management System";
            } else {
                subject = "[IMS] Đơn ứng tuyển của bạn không được chấp nhận";
                content = "Xin chào " + app.getInternName() + ",\n\n"
                        + "Đơn ứng tuyển của bạn (mã đơn: " + applicationId + ") đã bị TỪ CHỐI.\n"
                        + "Lý do: " + app.getRejectReason() + "\n\n"
                        + "Trân trọng,\nInternship Management System";
            }

            emailService.sendEmail(app.getInternEmail(), subject, content);
            LOGGER.info("Đã gửi email kết quả cho " + app.getInternEmail()
                    + " (applicationId=" + applicationId + ")");

        } catch (Exception e) {
            // Ghi nhận lỗi nhưng KHÔNG throw – quyết định đã được lưu
            LOGGER.log(Level.SEVERE,
                    "Lỗi gửi email thông báo kết quả (applicationId=" + applicationId + "): "
                            + e.getMessage(), e);
        }
    }
}
