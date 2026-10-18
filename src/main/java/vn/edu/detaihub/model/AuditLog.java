package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog extends BaseEntity {
  @Column(name = "actor_id", nullable = true)
  private Long actorId;

  @Column(name = "action", nullable = false)
  private String action;

  @Column(name = "object_type", nullable = false)
  private String objectType;

  @Column(name = "object_id", nullable = true)
  private Long objectId;

  @Column(name = "details", nullable = true, columnDefinition = "TEXT")
  private String details;
}
