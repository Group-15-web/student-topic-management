package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "topic")
@Getter
@Setter
public class Topic extends BaseEntity {
  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "department_id", nullable = false)
  private Long departmentId;

  @Column(name = "author_id", nullable = false)
  private Long authorId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "max_groups", nullable = false)
  private Integer maxGroups;

  @Column(name = "decision_note", nullable = true, columnDefinition = "TEXT")
  private String decisionNote;

  @Column(name = "approved_by", nullable = true)
  private Long approvedBy;
}
