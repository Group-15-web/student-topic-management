package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "topic_supervisor")
@Getter
@Setter
public class TopicSupervisor extends BaseEntity {
  @Column(name = "topic_id", nullable = false)
  private Long topicId;

  @Column(name = "lecturer_id", nullable = false)
  private Long lecturerId;
}
