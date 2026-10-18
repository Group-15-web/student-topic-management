CREATE TABLE user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    security_version INT NOT NULL,
    UNIQUE (email)
);

CREATE TABLE app_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    code VARCHAR(255) NOT NULL,
    UNIQUE (code)
);

CREATE TABLE user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE (user_id, role_id)
);

CREATE TABLE department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    head_user_id BIGINT,
    UNIQUE (code)
);

CREATE TABLE student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    code VARCHAR(255) NOT NULL,
    intake VARCHAR(255),
    active_group_id BIGINT,
    UNIQUE (user_id),
    UNIQUE (code)
);

CREATE TABLE lecturer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    code VARCHAR(255) NOT NULL,
    department_id BIGINT NOT NULL,
    UNIQUE (user_id),
    UNIQUE (code)
);

CREATE TABLE registration_cycle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    name VARCHAR(255) NOT NULL,
    cycle_type VARCHAR(255) NOT NULL,
    lecturer_start DATETIME(6) NOT NULL,
    lecturer_end DATETIME(6) NOT NULL,
    student_start DATETIME(6) NOT NULL,
    student_end DATETIME(6) NOT NULL,
    submission_deadline DATETIME(6) NOT NULL,
    reviewer_deadline DATETIME(6),
    council_date DATETIME(6),
    min_members INT NOT NULL,
    max_members INT NOT NULL,
    reviewer_count INT NOT NULL,
    council_required BOOLEAN NOT NULL,
    status VARCHAR(255) NOT NULL
);

CREATE TABLE topic (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    cycle_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    max_groups INT NOT NULL,
    decision_note TEXT,
    approved_by BIGINT
);

CREATE TABLE topic_supervisor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    topic_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    UNIQUE (topic_id, lecturer_id)
);

CREATE TABLE student_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    cycle_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    leader_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL
);

CREATE TABLE group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    UNIQUE (group_id, student_id)
);

CREATE TABLE topic_registration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    group_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    note TEXT,
    decided_by BIGINT
);

CREATE TABLE submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    registration_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,
    UNIQUE (registration_id, version_no)
);

CREATE TABLE reviewer_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    registration_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    UNIQUE (registration_id, lecturer_id)
);

CREATE TABLE review_council (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    cycle_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    report_at DATETIME(6) NOT NULL,
    location VARCHAR(255) NOT NULL
);

CREATE TABLE council_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    council_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    member_role VARCHAR(255) NOT NULL,
    UNIQUE (council_id, lecturer_id)
);

CREATE TABLE council_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    council_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    confirmed_by BIGINT,
    confirmed_at DATETIME(6),
    UNIQUE (registration_id)
);

CREATE TABLE evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    registration_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    evaluation_type VARCHAR(255) NOT NULL,
    score DECIMAL(5,2),
    comment TEXT,
    status VARCHAR(255) NOT NULL,
    UNIQUE (registration_id, lecturer_id)
);

CREATE TABLE evaluation_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    registration_id BIGINT NOT NULL,
    final_score DECIMAL(5,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    publication_version INT NOT NULL,
    published_by BIGINT NOT NULL,
    published_at DATETIME(6) NOT NULL,
    UNIQUE (registration_id)
);

CREATE TABLE announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    audience VARCHAR(255) NOT NULL,
    department_id BIGINT,
    status VARCHAR(255) NOT NULL
);

CREATE TABLE email_delivery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    result_id BIGINT NOT NULL,
    publication_version INT NOT NULL,
    recipient_id BIGINT NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    attempts INT NOT NULL,
    last_error VARCHAR(255),
    UNIQUE (result_id, publication_version, recipient_id)
);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    actor_id BIGINT,
    action VARCHAR(255) NOT NULL,
    object_type VARCHAR(255) NOT NULL,
    object_id BIGINT,
    details TEXT
);

ALTER TABLE user_role ADD CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id) REFERENCES user_account(id);

ALTER TABLE user_role ADD CONSTRAINT fk_user_role_role_id FOREIGN KEY (role_id) REFERENCES app_role(id);

ALTER TABLE department ADD CONSTRAINT fk_department_head_user_id FOREIGN KEY (head_user_id) REFERENCES user_account(id);

ALTER TABLE student ADD CONSTRAINT fk_student_user_id FOREIGN KEY (user_id) REFERENCES user_account(id);

ALTER TABLE student ADD CONSTRAINT fk_student_active_group_id FOREIGN KEY (active_group_id) REFERENCES student_group(id);

ALTER TABLE lecturer ADD CONSTRAINT fk_lecturer_user_id FOREIGN KEY (user_id) REFERENCES user_account(id);

ALTER TABLE lecturer ADD CONSTRAINT fk_lecturer_department_id FOREIGN KEY (department_id) REFERENCES department(id);

ALTER TABLE topic ADD CONSTRAINT fk_topic_cycle_id FOREIGN KEY (cycle_id) REFERENCES registration_cycle(id);

ALTER TABLE topic ADD CONSTRAINT fk_topic_department_id FOREIGN KEY (department_id) REFERENCES department(id);

ALTER TABLE topic ADD CONSTRAINT fk_topic_author_id FOREIGN KEY (author_id) REFERENCES user_account(id);

ALTER TABLE topic ADD CONSTRAINT fk_topic_approved_by FOREIGN KEY (approved_by) REFERENCES user_account(id);

ALTER TABLE topic_supervisor ADD CONSTRAINT fk_topic_supervisor_topic_id FOREIGN KEY (topic_id) REFERENCES topic(id);

ALTER TABLE topic_supervisor ADD CONSTRAINT fk_topic_supervisor_lecturer_id FOREIGN KEY (lecturer_id) REFERENCES lecturer(id);

ALTER TABLE student_group ADD CONSTRAINT fk_student_group_cycle_id FOREIGN KEY (cycle_id) REFERENCES registration_cycle(id);

ALTER TABLE student_group ADD CONSTRAINT fk_student_group_leader_id FOREIGN KEY (leader_id) REFERENCES student(id);

ALTER TABLE group_member ADD CONSTRAINT fk_group_member_group_id FOREIGN KEY (group_id) REFERENCES student_group(id);

ALTER TABLE group_member ADD CONSTRAINT fk_group_member_student_id FOREIGN KEY (student_id) REFERENCES student(id);

ALTER TABLE topic_registration ADD CONSTRAINT fk_topic_registration_group_id FOREIGN KEY (group_id) REFERENCES student_group(id);

ALTER TABLE topic_registration ADD CONSTRAINT fk_topic_registration_topic_id FOREIGN KEY (topic_id) REFERENCES topic(id);

ALTER TABLE topic_registration ADD CONSTRAINT fk_topic_registration_decided_by FOREIGN KEY (decided_by) REFERENCES user_account(id);

ALTER TABLE submission ADD CONSTRAINT fk_submission_registration_id FOREIGN KEY (registration_id) REFERENCES topic_registration(id);

ALTER TABLE submission ADD CONSTRAINT fk_submission_submitted_by FOREIGN KEY (submitted_by) REFERENCES user_account(id);

ALTER TABLE reviewer_assignment ADD CONSTRAINT fk_reviewer_assignment_registration_id FOREIGN KEY (registration_id) REFERENCES topic_registration(id);

ALTER TABLE reviewer_assignment ADD CONSTRAINT fk_reviewer_assignment_lecturer_id FOREIGN KEY (lecturer_id) REFERENCES lecturer(id);

ALTER TABLE review_council ADD CONSTRAINT fk_review_council_cycle_id FOREIGN KEY (cycle_id) REFERENCES registration_cycle(id);

ALTER TABLE council_member ADD CONSTRAINT fk_council_member_council_id FOREIGN KEY (council_id) REFERENCES review_council(id);

ALTER TABLE council_member ADD CONSTRAINT fk_council_member_lecturer_id FOREIGN KEY (lecturer_id) REFERENCES lecturer(id);

ALTER TABLE council_assignment ADD CONSTRAINT fk_council_assignment_council_id FOREIGN KEY (council_id) REFERENCES review_council(id);

ALTER TABLE council_assignment ADD CONSTRAINT fk_council_assignment_registration_id FOREIGN KEY (registration_id) REFERENCES topic_registration(id);

ALTER TABLE council_assignment ADD CONSTRAINT fk_council_assignment_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES user_account(id);

ALTER TABLE evaluation ADD CONSTRAINT fk_evaluation_registration_id FOREIGN KEY (registration_id) REFERENCES topic_registration(id);

ALTER TABLE evaluation ADD CONSTRAINT fk_evaluation_lecturer_id FOREIGN KEY (lecturer_id) REFERENCES lecturer(id);

ALTER TABLE evaluation_result ADD CONSTRAINT fk_evaluation_result_registration_id FOREIGN KEY (registration_id) REFERENCES topic_registration(id);

ALTER TABLE evaluation_result ADD CONSTRAINT fk_evaluation_result_published_by FOREIGN KEY (published_by) REFERENCES user_account(id);

ALTER TABLE announcement ADD CONSTRAINT fk_announcement_author_id FOREIGN KEY (author_id) REFERENCES user_account(id);

ALTER TABLE announcement ADD CONSTRAINT fk_announcement_department_id FOREIGN KEY (department_id) REFERENCES department(id);

ALTER TABLE email_delivery ADD CONSTRAINT fk_email_delivery_result_id FOREIGN KEY (result_id) REFERENCES evaluation_result(id);

ALTER TABLE email_delivery ADD CONSTRAINT fk_email_delivery_recipient_id FOREIGN KEY (recipient_id) REFERENCES user_account(id);

ALTER TABLE audit_log ADD CONSTRAINT fk_audit_log_actor_id FOREIGN KEY (actor_id) REFERENCES user_account(id);

ALTER TABLE evaluation ADD CONSTRAINT ck_score CHECK (score IS NULL OR (score >= 0 AND score <= 10));

ALTER TABLE registration_cycle ADD CONSTRAINT ck_members CHECK (min_members >= 3 AND max_members >= min_members AND max_members <= 5);
