package vn.edu.detaihub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "evaluation_result")
@Getter
@Setter
public class EvaluationResult extends BaseEntity {
  @Column(name = "registration_id", nullable = false)
  private Long registrationId;

  @Column(name = "final_score", nullable = false, precision = 5, scale = 2)
  private BigDecimal finalScore;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "publication_version", nullable = false)
  private Integer publicationVersion;

  @Column(name = "published_by", nullable = false)
  private Long publishedBy;

  @Column(name = "published_at", nullable = false)
  private LocalDateTime publishedAt;
}
