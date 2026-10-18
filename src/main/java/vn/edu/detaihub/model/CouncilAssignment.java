package vn.edu.detaihub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "council_assignment")
@Getter
@Setter
public class CouncilAssignment extends BaseEntity {
  @Column(name = "council_id", nullable = false)
  private Long councilId;

  @Column(name = "registration_id", nullable = false)
  private Long registrationId;

  @Column(name = "confirmed_by", nullable = true)
  private Long confirmedBy;

  @Column(name = "confirmed_at", nullable = true)
  private LocalDateTime confirmedAt;
}
