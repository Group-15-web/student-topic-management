-- V3: Quan ly danh sach sinh vien du dieu kien tham gia dot (Academic Whitelist)

ALTER TABLE registration_cycle ADD COLUMN whitelist_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE registration_cycle ADD COLUMN target_intakes VARCHAR(255);

CREATE TABLE cycle_eligible_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    cycle_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    note VARCHAR(255),
    UNIQUE (cycle_id, student_id)
);

CREATE INDEX idx_eligible_cycle_student ON cycle_eligible_student(cycle_id, student_id);
