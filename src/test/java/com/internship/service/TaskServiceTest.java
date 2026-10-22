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
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

class TaskServiceTest {

    private ConnectionSpy connection;
    private FakeTasks tasks;
    private FakeAssignments assignments;
    private FakeProgress progress;
    private FakeAccess access;
    private TaskService service;

    public static void main(String[] args) throws Exception {
        TaskServiceTest checks = new TaskServiceTest();
        checks.setUp(); checks.statusFollowsPercentage();
        checks.setUp(); checks.onlyActiveMentorCanCreateTask();
        checks.setUp(); checks.cannotAssignTaskToEnrollmentOfAnotherMentor();
        checks.setUp(); checks.duplicateAssignmentBecomesBusinessErrorAndRollsBack();
        checks.setUp(); checks.onlyOwningInternCanUpdateProgress();
        checks.setUp(); checks.progressCannotDecrease();
        checks.setUp(); checks.completionAppendsHistoryAndChangesStatusInOneTransaction();
        checks.setUp(); checks.updateFailureRollsBackProgressAndStatusTogether();
        checks.setUp(); checks.completedAssignmentCannotBeChanged();
        System.out.println("BE4 TaskService checks passed: 9");
    }

    void setUp() {
        connection = new ConnectionSpy();
        tasks = new FakeTasks();
        assignments = new FakeAssignments();
        progress = new FakeProgress();
        access = new FakeAccess();
        service = new TaskService(() -> connection.proxy, tasks, assignments, progress, access);
    }

    void statusFollowsPercentage() {
        assertEquals(TaskAssignmentStatus.TODO, TaskService.statusFor(0));
        assertEquals(TaskAssignmentStatus.IN_PROGRESS, TaskService.statusFor(1));
        assertEquals(TaskAssignmentStatus.IN_PROGRESS, TaskService.statusFor(99));
        assertEquals(TaskAssignmentStatus.DONE, TaskService.statusFor(100));
    }

    void onlyActiveMentorCanCreateTask() {
        access.activeMentor = false;
        assertThrows(SecurityException.class, () -> service.createTask(10, "Viết báo cáo", null, null));
        assertFalse(tasks.created);
    }

    void cannotAssignTaskToEnrollmentOfAnotherMentor() {
        access.currentMentor = false;
        assertThrows(SecurityException.class, () -> service.assignTask(10, 20, 30));
        assertEquals(0, connection.commits);
        assertEquals(1, connection.rollbacks);
        assertFalse(assignments.created);
    }

    void duplicateAssignmentBecomesBusinessErrorAndRollsBack() {
        assignments.duplicate = true;
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.assignTask(10, 20, 30));
        assertTrue(error.getMessage().contains("đã được giao"));
        assertEquals(0, connection.commits);
        assertEquals(1, connection.rollbacks);
    }

    void onlyOwningInternCanUpdateProgress() {
        access.internOwner = false;
        assertThrows(SecurityException.class, () -> service.updateProgress(40, 50, 30, "Đang làm"));
        assertEquals(0, progress.appends);
        assertEquals(1, connection.rollbacks);
    }

    void progressCannotDecrease() {
        progress.latest = 60;
        assertThrows(IllegalArgumentException.class,
                () -> service.updateProgress(40, 50, 50, "Đi lùi"));
        assertEquals(0, progress.appends);
        assertEquals(1, connection.rollbacks);
    }

    void completionAppendsHistoryAndChangesStatusInOneTransaction() throws SQLException {
        progress.latest = 70;
        TaskProgress entry = service.updateProgress(40, 50, 100, "Hoàn thành");
        assertEquals(100, entry.getCompletionPercentage());
        assertEquals(1, progress.appends);
        assertEquals(TaskAssignmentStatus.DONE, assignments.updatedStatus);
        assertEquals(1, connection.commits);
        assertEquals(0, connection.rollbacks);
    }

    void updateFailureRollsBackProgressAndStatusTogether() {
        assignments.failUpdate = true;
        assertThrows(SQLException.class, () -> service.updateProgress(40, 50, 80, "Đang làm"));
        assertEquals(1, progress.appends);
        assertEquals(0, connection.commits);
        assertEquals(1, connection.rollbacks);
    }

    void completedAssignmentCannotBeChanged() {
        assignments.assignment.setStatus(TaskAssignmentStatus.DONE);
        assertThrows(IllegalStateException.class,
                () -> service.updateProgress(40, 50, 100, "Sửa tiếp"));
        assertEquals(0, progress.appends);
        assertEquals(1, connection.rollbacks);
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static <T extends Throwable> T assertThrows(Class<T> expected, CheckedAction action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return expected.cast(actual);
            }
            throw new AssertionError("Expected " + expected.getName() + " but got " + actual, actual);
        }
        throw new AssertionError("Expected " + expected.getName() + " to be thrown");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("Expected false");
        }
    }

    private static final class ConnectionSpy {
        int commits;
        int rollbacks;
        final Connection proxy = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (ignored, method, args) -> {
                    return switch (method.getName()) {
                        case "commit" -> { commits++; yield null; }
                        case "rollback" -> { rollbacks++; yield null; }
                        case "setAutoCommit", "setTransactionIsolation", "close" -> null;
                        case "getAutoCommit" -> false;
                        case "isClosed" -> false;
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
    }

    private static final class FakeTasks extends TaskDAO {
        boolean created;
        final Task task;

        FakeTasks() {
            task = new Task();
            task.setTaskId(20);
            task.setCreatedByUserId(10);
            task.setStatus(TaskStatus.OPEN);
        }

        @Override
        public Task create(Connection connection, Task newTask) {
            created = true;
            newTask.setTaskId(20);
            return newTask;
        }

        @Override
        public Task findById(Connection connection, long taskId) {
            return task;
        }
    }

    private static final class FakeAssignments extends TaskAssignmentDAO {
        boolean created;
        boolean duplicate;
        boolean failUpdate;
        TaskAssignmentStatus updatedStatus;
        final TaskAssignment assignment;

        FakeAssignments() {
            assignment = new TaskAssignment();
            assignment.setAssignmentId(50);
            assignment.setTaskId(20);
            assignment.setEnrollmentId(30);
            assignment.setStatus(TaskAssignmentStatus.TODO);
        }

        @Override
        public TaskAssignment create(Connection connection, long taskId, int enrollmentId)
                throws SQLException {
            if (duplicate) {
                throw new SQLIntegrityConstraintViolationException("Duplicate uq_ta_task_enrollment", "23000", 1062);
            }
            created = true;
            return assignment;
        }

        @Override
        public TaskAssignment findByIdForUpdate(Connection connection, long assignmentId) {
            return assignment;
        }

        @Override
        public void updateStatus(Connection connection, long assignmentId, TaskAssignmentStatus status)
                throws SQLException {
            if (failUpdate) {
                throw new SQLException("Simulated update failure");
            }
            updatedStatus = status;
        }
    }

    private static final class FakeProgress extends TaskProgressDAO {
        Integer latest;
        int appends;

        @Override
        public Integer findLatestPercentage(Connection connection, long assignmentId) {
            return latest;
        }

        @Override
        public TaskProgress append(Connection connection, long assignmentId, int percentage, String note) {
            appends++;
            TaskProgress entry = new TaskProgress();
            entry.setAssignmentId(assignmentId);
            entry.setCompletionPercentage(percentage);
            entry.setNote(note);
            return entry;
        }
    }

    private static final class FakeAccess extends TaskAccessDAO {
        boolean activeMentor = true;
        boolean currentMentor = true;
        boolean internOwner = true;

        @Override
        public void lockEnrollmentForMentorWrite(Connection connection, int enrollmentId) {
            // Fake DAO: actual locking is covered by the opt-in MySQL regression tests.
        }

        @Override
        public boolean isActiveMentor(Connection connection, long userId) {
            return activeMentor;
        }

        @Override
        public boolean isCurrentMentor(Connection connection, long userId, int enrollmentId) {
            return currentMentor;
        }

        @Override
        public boolean isActiveInternOwner(Connection connection, long userId, int enrollmentId) {
            return internOwner;
        }
    }
}
