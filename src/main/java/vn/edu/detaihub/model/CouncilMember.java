package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "council_member")
@Getter
@Setter
public class CouncilMember extends BaseEntity {
  @Column(name = "council_id", nullable = false)
  private Long councilId;

  @Column(name = "lecturer_id", nullable = false)
  private Long lecturerId;

  @Column(name = "member_role", nullable = false)
  private String memberRole;
}
