package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reviewer_assignment")
@Getter
@Setter
public class ReviewerAssignment extends BaseEntity {
  @Column(name = "registration_id", nullable = false)
  private Long registrationId;

  @Column(name = "lecturer_id", nullable = false)
  private Long lecturerId;
}
