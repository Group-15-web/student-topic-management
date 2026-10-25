package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cycle_eligible_student")
@Getter
@Setter
public class CycleEligibleStudent extends BaseEntity {
  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "note", nullable = true)
  private String note;
}
