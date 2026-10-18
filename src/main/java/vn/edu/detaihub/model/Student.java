package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student")
@Getter
@Setter
public class Student extends BaseEntity {
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "intake", nullable = true)
  private String intake;

  @Column(name = "active_group_id", nullable = true)
  private Long activeGroupId;
}
