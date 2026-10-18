package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "submission")
@Getter
@Setter
public class Submission extends BaseEntity {
  @Column(name = "registration_id", nullable = false)
  private Long registrationId;

  @Column(name = "version_no", nullable = false)
  private Integer versionNo;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Column(name = "submitted_by", nullable = false)
  private Long submittedBy;
}
