package com.internship.service;

import com.internship.dao.TaskAccessDAO;
import com.internship.dao.TaskAssignmentDAO;
import com.internship.dao.TaskDAO;
import com.internship.dao.TaskProgressDAO;
import com.internship.model.Task;
import com.internship.model.TaskAssignment;
import com.internship.model.TaskAssignmentStatus;
import com.internship.model.TaskProgress;
import com.internship.model.TaskStatus;
import com.internship.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;

/** Business rules for mentor tasks and intern progress. No servlet trusts a submitted user ID. */
public class TaskService {

    @FunctionalInterface
    public interface ConnectionFactory {
        Connection open() throws SQLException;
    }

    private final ConnectionFactory connections;
    private final TaskDAO tasks;
    private final TaskAssignmentDAO assignments;
    private final TaskProgressDAO progress;
    private final TaskAccessDAO access;

    public TaskService() {
        this(DBConnection::getConnection, new TaskDAO(), new TaskAssignmentDAO(),
                new TaskProgressDAO(), new TaskAccessDAO());
    }

    TaskService(ConnectionFactory connections, TaskDAO tasks, TaskAssignmentDAO assignments,
                TaskProgressDAO progress, TaskAccessDAO access) {
        this.connections = connections;
        this.tasks = tasks;
        this.assignments = assignments;
        this.progress = progress;
        this.access = access;
    }

    public Task createTask(long mentorUserId, String title, String description, LocalDate dueDate)
            throws SQLException {
        requirePositive(mentorUserId, "mentorUserId");
        if (title == null || title.isBlank() || title.trim().length() > 255) {
            throw new IllegalArgumentException("Tiêu đề task phải có từ 1 đến 255 ký tự");
        }
        try (Connection connection = connections.open()) {
            if (!access.isActiveMentor(connection, mentorUserId)) {
                throw new SecurityException("Chỉ mentor đang hoạt động được tạo task");
            }
            Task task = new Task();
            task.setTitle(title.trim());
            task.setDescription(description);
            task.setCreatedByUserId(mentorUserId);
            task.setStatus(TaskStatus.OPEN);
            task.setDueDate(dueDate);
            return tasks.create(connection, task);
        }
    }

    public TaskAssignment assignTask(long mentorUserId, long taskId, int enrollmentId)
            throws SQLException {
        requirePositive(mentorUserId, "mentorUserId");
        requirePositive(taskId, "taskId");
        requirePositive(enrollmentId, "enrollmentId");
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                access.lockEnrollmentForMentorWrite(connection, enrollmentId);
                if (!access.isCurrentMentor(connection, mentorUserId, enrollmentId)) {
                    throw new SecurityException("Mentor không được phân cho đợt thực tập này");
                }
                Task task = tasks.findById(connection, taskId);
                if (task == null || task.getCreatedByUserId() != mentorUserId
                        || task.getStatus() != TaskStatus.OPEN) {
                    throw new IllegalArgumentException("Task không tồn tại, không thuộc mentor hoặc không còn mở");
                }
                TaskAssignment assignment;
                try {
                    assignment = assignments.create(connection, taskId, enrollmentId);
                } catch (SQLIntegrityConstraintViolationException exception) {
                    if (exception.getErrorCode() == 1062) {
                        throw new IllegalStateException("Task đã được giao cho TTS trong đợt này", exception);
                    }
                    throw exception;
                }
                connection.commit();
                return assignment;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public TaskProgress updateProgress(long internUserId, long assignmentId,
                                       int completionPercentage, String note) throws SQLException {
        requirePositive(internUserId, "internUserId");
        requirePositive(assignmentId, "assignmentId");
        if (completionPercentage < 0 || completionPercentage > 100) {
            throw new IllegalArgumentException("Tiến độ phải trong khoảng 0–100");
        }
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                // Locking the assignment serializes concurrent progress updates for this task.
                TaskAssignment assignment = assignments.findByIdForUpdate(connection, assignmentId);
                if (assignment == null) {
                    throw new IllegalArgumentException("Không tìm thấy task được giao");
                }
                if (!access.isActiveInternOwner(connection, internUserId, assignment.getEnrollmentId())) {
                    throw new SecurityException("TTS không sở hữu task này hoặc đợt thực tập không ACTIVE");
                }
                Task task = tasks.findById(connection, assignment.getTaskId());
                if (task == null || task.getStatus() != TaskStatus.OPEN
                        || assignment.getStatus() == TaskAssignmentStatus.DONE) {
                    throw new IllegalStateException("Task đã hoàn tất hoặc không còn mở");
                }
                Integer previous = progress.findLatestPercentage(connection, assignmentId);
                if (previous != null && completionPercentage < previous) {
                    throw new IllegalArgumentException("Không được giảm tiến độ đã ghi");
                }
                TaskAssignmentStatus nextStatus = statusFor(completionPercentage);
                TaskProgress entry = progress.append(connection, assignmentId, completionPercentage, note);
                assignments.updateStatus(connection, assignmentId, nextStatus);
                connection.commit();
                return entry;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public List<TaskAssignment> findAssignmentsForIntern(long internUserId, int enrollmentId)
            throws SQLException {
        requirePositive(internUserId, "internUserId");
        requirePositive(enrollmentId, "enrollmentId");
        try (Connection connection = connections.open()) {
            if (!access.isInternOwner(connection, internUserId, enrollmentId)) {
                throw new SecurityException("TTS không sở hữu đợt thực tập này");
            }
            return assignments.findByEnrollment(connection, enrollmentId);
        }
    }

    public List<Task> findTasksForIntern(long internUserId, int enrollmentId) throws SQLException {
        requirePositive(internUserId, "internUserId");
        requirePositive(enrollmentId, "enrollmentId");
        try (Connection connection = connections.open()) {
            if (!access.isInternOwner(connection, internUserId, enrollmentId)) {
                throw new SecurityException("TTS không sở hữu đợt thực tập này");
            }
            return tasks.findByEnrollment(connection, enrollmentId);
        }
    }

    public List<TaskAssignment> findAssignmentsForMentor(long mentorUserId, int enrollmentId)
            throws SQLException {
        requirePositive(mentorUserId, "mentorUserId");
        requirePositive(enrollmentId, "enrollmentId");
        try (Connection connection = connections.open()) {
            if (!access.isAssignedMentor(connection, mentorUserId, enrollmentId)) {
                throw new SecurityException("Mentor không được phân cho đợt thực tập này");
            }
            return assignments.findByEnrollment(connection, enrollmentId);
        }
    }

    public List<Task> findTasksForMentor(long mentorUserId, int enrollmentId) throws SQLException {
        requirePositive(mentorUserId, "mentorUserId");
        requirePositive(enrollmentId, "enrollmentId");
        try (Connection connection = connections.open()) {
            if (!access.isAssignedMentor(connection, mentorUserId, enrollmentId)) {
                throw new SecurityException("Mentor không được phân cho đợt thực tập này");
            }
            return tasks.findByEnrollment(connection, enrollmentId);
        }
    }

    public List<TaskProgress> findProgressHistory(long actorUserId, long assignmentId) throws SQLException {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(assignmentId, "assignmentId");
        try (Connection connection = connections.open()) {
            TaskAssignment assignment = assignments.findById(connection, assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Không tìm thấy task được giao");
            }
            boolean allowed = access.isInternOwner(connection, actorUserId, assignment.getEnrollmentId())
                    || access.isAssignedMentor(connection, actorUserId, assignment.getEnrollmentId());
            if (!allowed) {
                throw new SecurityException("Không có quyền xem lịch sử tiến độ");
            }
            return progress.findByAssignment(connection, assignmentId);
        }
    }

    static TaskAssignmentStatus statusFor(int percentage) {
        if (percentage == 0) {
            return TaskAssignmentStatus.TODO;
        }
        return percentage == 100 ? TaskAssignmentStatus.DONE : TaskAssignmentStatus.IN_PROGRESS;
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " phải lớn hơn 0");
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
