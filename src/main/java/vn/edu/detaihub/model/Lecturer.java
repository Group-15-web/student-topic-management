package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lecturer")
@Getter
@Setter
public class Lecturer extends BaseEntity {
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "department_id", nullable = false)
  private Long departmentId;
}
