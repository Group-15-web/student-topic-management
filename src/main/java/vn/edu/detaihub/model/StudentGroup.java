package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student_group")
@Getter
@Setter
public class StudentGroup extends BaseEntity {
  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "leader_id", nullable = false)
  private Long leaderId;

  @Column(name = "status", nullable = false)
  private String status;
}
