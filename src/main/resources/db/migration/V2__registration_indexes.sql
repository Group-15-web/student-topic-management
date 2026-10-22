-- V2: Bổ sung chỉ mục hiệu năng cho bảng đăng ký đề tài
CREATE INDEX idx_topic_reg_group ON topic_registration(group_id);
CREATE INDEX idx_topic_reg_topic_status ON topic_registration(topic_id, status);
