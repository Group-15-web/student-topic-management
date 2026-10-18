package vn.edu.detaihub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_role")
@Getter
@Setter
public class Role extends BaseEntity {
  @Column(name = "code", nullable = false)
  private String code;
}
