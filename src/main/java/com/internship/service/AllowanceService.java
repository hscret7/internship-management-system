package com.internship.service;

import com.internship.dao.AllowanceDAO;
import com.internship.model.Allowance;
import com.internship.model.User;
import com.internship.util.ConnectionProvider;
import com.internship.util.DBConnection;
import com.internship.util.UserRoleChecks;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AllowanceService {

    private final AllowanceDAO allowanceDAO;
    private final ConnectionProvider connections;

    public AllowanceService() {
        this(new AllowanceDAO(), DBConnection::getConnection);
    }

    public AllowanceService(AllowanceDAO allowanceDAO, ConnectionProvider connections) {
        this.allowanceDAO = allowanceDAO;
        this.connections = connections;
    }

    public void createAllowance(User hrUser, Allowance allowance) throws SQLException {
        if (hrUser == null || !UserRoleChecks.canManageHr(hrUser)) {
            throw new IllegalArgumentException("Không có quyền tạo phụ cấp.");
        }
        if (allowance == null) throw new IllegalArgumentException("Thiếu dữ liệu phụ cấp.");
        if (allowance.getEnrollmentId() <= 0 || allowance.getAllowanceType() == null || !List.of("MEAL", "TRAVEL", "SALARY", "OTHER").contains(allowance.getAllowanceType())) throw new IllegalArgumentException("Ghi danh hoặc loại phụ cấp không hợp lệ.");
        try {
            if (allowance.getPeriod() == null || !allowance.getPeriod().matches("[0-9]{4}-[0-9]{2}")) throw new IllegalArgumentException();
            java.time.YearMonth.parse(allowance.getPeriod());
        } catch (RuntimeException e) { throw new IllegalArgumentException("Kỳ phụ cấp phải có dạng YYYY-MM."); }
        if (allowance.getAmount() == null || allowance.getAmount().signum() < 0 || allowance.getAmount().scale() > 2 || allowance.getAmount().compareTo(new java.math.BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Số tiền phụ cấp không được để trống hoặc âm.");
        }
        allowance.setCreatedBy(hrUser.getUserId());

        try (Connection c = connections.getConnection()) {
            allowanceDAO.requireEnrollment(allowance.getEnrollmentId(), c);
            allowanceDAO.create(allowance, c);
        }
    }

    public void updateStatus(User hrUser, int allowanceId, String status) throws SQLException {
        if (hrUser == null || !UserRoleChecks.canManageHr(hrUser)) {
            throw new IllegalArgumentException("Không có quyền cập nhật phụ cấp.");
        }
        if (allowanceId <= 0 || !"PAID".equals(status)) throw new IllegalArgumentException("Chỉ có thể đánh dấu phụ cấp đã chi.");
        try (Connection c = connections.getConnection()) {
            if (!allowanceDAO.updateStatus(allowanceId, status, c)) throw new IllegalArgumentException("Không tìm thấy phụ cấp hoặc phụ cấp đã được chi.");
        }
    }

    public List<Allowance> getAllowances(User user, Integer enrollmentId) throws SQLException {
        if (user == null) {
            throw new IllegalArgumentException("Chưa đăng nhập.");
        }
        if (!UserRoleChecks.isIntern(user) && !UserRoleChecks.canManageHr(user)) throw new IllegalArgumentException("Không có quyền xem phụ cấp.");
        Long ownerId = UserRoleChecks.isIntern(user) ? user.getUserId() : null;
        try (Connection c = connections.getConnection()) {
            return allowanceDAO.list(enrollmentId, ownerId, c);
        }
    }
}
