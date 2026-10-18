package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "group_member")
@Getter
@Setter
public class GroupMember extends BaseEntity {
  @Column(name = "group_id", nullable = false)
  private Long groupId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "status", nullable = false)
  private String status;
}
