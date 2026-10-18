package vn.edu.detaihub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "evaluation")
@Getter
@Setter
public class Evaluation extends BaseEntity {
  @Column(name = "registration_id", nullable = false)
  private Long registrationId;

  @Column(name = "lecturer_id", nullable = false)
  private Long lecturerId;

  @Column(name = "evaluation_type", nullable = false)
  private String evaluationType;

  @Column(name = "score", nullable = true, precision = 5, scale = 2)
  private BigDecimal score;

  @Column(name = "comment", nullable = true, columnDefinition = "TEXT")
  private String comment;

  @Column(name = "status", nullable = false)
  private String status;
}
