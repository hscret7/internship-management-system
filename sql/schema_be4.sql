-- =============================================================================
-- BE4 Schema: Task, Report & Evaluation
-- =============================================================================
--
-- THỨ TỰ CHẠY SCHEMA:
--   1. database/schema.sql          (BE1 – roles, users)
--   2. sql/schema_be2.sql           (BE2 – intern_profiles, applications, …)
--   3. sql/schema_be3.sql           (BE3 – departments, internship_programs,
--                                     mentors, program_enrollments,
--                                     mentor_assignments)
--   4. sql/schema_be4.sql           (file này)
--
-- Các bảng BE4 phụ thuộc:
--   • users              (BE1)  – created_by_user_id, reviewer_user_id
--   • program_enrollments (BE3) – enrollment_id  (INT)
--   • mentors             (BE3) – mentor_id      (INT)
--
-- Chưa chạy được trên DB chỉ có BE1 vì thiếu bảng BE3.
-- Regression SQL thật: mvn -Dbe4.mysql=true test (database tạm riêng).
-- Sprint 2 giữ nguyên DDL và khóa enrollment_id; xem docs/BE4_SPRINT2_HANDOFF.md.
--
-- QUY ƯỚC ĐÃ CHỐT:
--   • week_number: tuần thứ N trong đợt thực tập (enrollment), bắt đầu
--     từ 1. KHÔNG phải tuần lịch (ISO week). CHECK (week_number >= 1).
--     UNIQUE(enrollment_id, week_number) → mỗi enrollment 1 report/tuần.
--   • evaluations: mỗi enrollment chỉ có 1 đánh giá tổng kết duy nhất.
--     UNIQUE(enrollment_id). mentor_id lưu người thực hiện đánh giá.
--   • task_progress.completion_percentage: NOT NULL, 0–100.
--   • report_feedbacks.rating: nullable – feedback có thể chỉ có comment.
--   • evaluations.score: bắt buộc, thang điểm 0–100, tối đa 2 số thập phân.
-- =============================================================================

USE internship_management;

-- -----------------------------------------------------------------------------
-- 1. tasks – Nhiệm vụ do mentor / HR tạo
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    task_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    title              VARCHAR(255)   NOT NULL,
    description        TEXT,
    created_by_user_id BIGINT         NOT NULL,

    status ENUM('OPEN', 'CLOSED', 'CANCELLED')
        NOT NULL DEFAULT 'OPEN',

    due_date           DATE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_tasks_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(user_id)
);

-- -----------------------------------------------------------------------------
-- 2. task_assignments – Giao task cho intern (qua enrollment)
--    enrollment_id là INT – khớp program_enrollments.enrollment_id (BE3)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_assignments (
    assignment_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id        BIGINT NOT NULL,
    enrollment_id  INT    NOT NULL,

    status ENUM('TODO', 'IN_PROGRESS', 'DONE')
        NOT NULL DEFAULT 'TODO',

    assigned_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ta_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(task_id),

    CONSTRAINT fk_ta_enrollment
        FOREIGN KEY (enrollment_id)
        REFERENCES program_enrollments(enrollment_id),

    CONSTRAINT uq_ta_task_enrollment
        UNIQUE (task_id, enrollment_id)
);

-- -----------------------------------------------------------------------------
-- 3. task_progress – Lịch sử cập nhật tiến độ (append-only, không ghi đè)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_progress (
    progress_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id  BIGINT NOT NULL,
    note           TEXT,
    completion_percentage TINYINT UNSIGNED NOT NULL
                       CHECK (completion_percentage BETWEEN 0 AND 100),

    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tp_assignment
        FOREIGN KEY (assignment_id)
        REFERENCES task_assignments(assignment_id)
);

-- -----------------------------------------------------------------------------
-- 4. weekly_reports – Báo cáo tuần của intern
--    enrollment_id là INT – khớp program_enrollments.enrollment_id (BE3)
--    week_number = tuần thứ N trong đợt thực tập, bắt đầu từ 1
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS weekly_reports (
    report_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id  INT    NOT NULL,
    week_number    SMALLINT NOT NULL
                       CHECK (week_number >= 1),
    content        TEXT,

    status ENUM('DRAFT', 'SUBMITTED', 'REVIEWED')
        NOT NULL DEFAULT 'DRAFT',

    submitted_at   TIMESTAMP NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_wr_enrollment
        FOREIGN KEY (enrollment_id)
        REFERENCES program_enrollments(enrollment_id),

    CONSTRAINT uq_wr_enrollment_week
        UNIQUE (enrollment_id, week_number)
);

-- -----------------------------------------------------------------------------
-- 5. report_feedbacks – Phản hồi của mentor cho weekly report
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS report_feedbacks (
    feedback_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id        BIGINT NOT NULL,
    reviewer_user_id BIGINT NOT NULL,
    comment          TEXT,
    rating           TINYINT UNSIGNED
                         CHECK (rating BETWEEN 1 AND 5),

    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rf_report
        FOREIGN KEY (report_id)
        REFERENCES weekly_reports(report_id),

    CONSTRAINT fk_rf_reviewer
        FOREIGN KEY (reviewer_user_id)
        REFERENCES users(user_id)
);

-- -----------------------------------------------------------------------------
-- 6. evaluations – Đánh giá tổng kết intern (1 đánh giá / enrollment)
--    enrollment_id là INT – khớp program_enrollments.enrollment_id (BE3)
--    mentor_id     là INT – khớp mentors.mentor_id (BE3) – người đánh giá
--    UNIQUE(enrollment_id): mỗi enrollment chỉ có 1 đánh giá duy nhất
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluations (
    evaluation_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id  INT    NOT NULL,
    mentor_id      INT    NOT NULL,
    score          DECIMAL(5,2) NOT NULL
                       CHECK (score BETWEEN 0 AND 100),
    comment        TEXT,

    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_eval_enrollment
        FOREIGN KEY (enrollment_id)
        REFERENCES program_enrollments(enrollment_id),

    CONSTRAINT fk_eval_mentor
        FOREIGN KEY (mentor_id)
        REFERENCES mentors(mentor_id),

    CONSTRAINT uq_eval_enrollment
        UNIQUE (enrollment_id)
);
