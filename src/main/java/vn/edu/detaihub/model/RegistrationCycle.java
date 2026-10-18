package vn.edu.detaihub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "registration_cycle")
@Getter
@Setter
public class RegistrationCycle extends BaseEntity {
  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "cycle_type", nullable = false)
  private String cycleType;

  @Column(name = "lecturer_start", nullable = false)
  private LocalDateTime lecturerStart;

  @Column(name = "lecturer_end", nullable = false)
  private LocalDateTime lecturerEnd;

  @Column(name = "student_start", nullable = false)
  private LocalDateTime studentStart;

  @Column(name = "student_end", nullable = false)
  private LocalDateTime studentEnd;

  @Column(name = "submission_deadline", nullable = false)
  private LocalDateTime submissionDeadline;

  @Column(name = "reviewer_deadline", nullable = true)
  private LocalDateTime reviewerDeadline;

  @Column(name = "council_date", nullable = true)
  private LocalDateTime councilDate;

  @Column(name = "min_members", nullable = false)
  private Integer minMembers;

  @Column(name = "max_members", nullable = false)
  private Integer maxMembers;

  @Column(name = "reviewer_count", nullable = false)
  private Integer reviewerCount;

  @Column(name = "council_required", nullable = false)
  private Boolean councilRequired;

  @Column(name = "status", nullable = false)
  private String status;
}
