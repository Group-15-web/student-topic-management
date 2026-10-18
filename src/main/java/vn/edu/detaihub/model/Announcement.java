package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "announcement")
@Getter
@Setter
public class Announcement extends BaseEntity {
  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "author_id", nullable = false)
  private Long authorId;

  @Column(name = "audience", nullable = false)
  private String audience;

  @Column(name = "department_id", nullable = true)
  private Long departmentId;

  @Column(name = "status", nullable = false)
  private String status;
}
