package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "topic_registration")
@Getter
@Setter
public class TopicRegistration extends BaseEntity {
  @Column(name = "group_id", nullable = false)
  private Long groupId;

  @Column(name = "topic_id", nullable = false)
  private Long topicId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "note", nullable = true, columnDefinition = "TEXT")
  private String note;

  @Column(name = "decided_by", nullable = true)
  private Long decidedBy;
}
