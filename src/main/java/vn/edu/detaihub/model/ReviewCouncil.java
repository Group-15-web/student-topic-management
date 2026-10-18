package vn.edu.detaihub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "review_council")
@Getter
@Setter
public class ReviewCouncil extends BaseEntity {
  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "report_at", nullable = false)
  private LocalDateTime reportAt;

  @Column(name = "location", nullable = false)
  private String location;
}
