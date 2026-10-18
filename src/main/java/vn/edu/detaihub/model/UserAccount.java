package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_account")
@Getter
@Setter
public class UserAccount extends BaseEntity {
  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled;

  @Column(name = "security_version", nullable = false)
  private Integer securityVersion;
}
