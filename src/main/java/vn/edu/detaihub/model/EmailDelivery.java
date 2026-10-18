package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_delivery")
@Getter
@Setter
public class EmailDelivery extends BaseEntity {
  @Column(name = "result_id", nullable = false)
  private Long resultId;

  @Column(name = "publication_version", nullable = false)
  private Integer publicationVersion;

  @Column(name = "recipient_id", nullable = false)
  private Long recipientId;

  @Column(name = "recipient", nullable = false)
  private String recipient;

  @Column(name = "subject", nullable = false)
  private String subject;

  @Column(name = "body", nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "attempts", nullable = false)
  private Integer attempts;

  @Column(name = "last_error", nullable = true)
  private String lastError;
}
