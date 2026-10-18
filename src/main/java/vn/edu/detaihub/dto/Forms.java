package vn.edu.detaihub.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/** Explicit form contracts: never bind an HTTP request directly onto a JPA entity. */
public final class Forms {
  @Data
  public static class Account {
    private Long id;
    @NotBlank @Email private String email;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @Size(max = 72)
    private String password;

    @NotBlank private String role = "STUDENT";

    @NotBlank
    @Size(max = 40)
    private String code;

    private Long departmentId;
    private String intake = "2026";
    private boolean enabled = true;
  }

  @Data
  public static class Department {
    private Long id;

    @NotBlank
    @Size(max = 30)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    private Long headUserId;
  }

  @Data
  public static class Cycle {
    private Long id;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank private String cycleType = "GRADUATION_THESIS";

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime lecturerStart;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime lecturerEnd;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime studentStart;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime studentEnd;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime submissionDeadline;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime reviewerDeadline;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime councilDate;

    @Min(3)
    @Max(5)
    private int minMembers = 3;

    @Min(3)
    @Max(5)
    private int maxMembers = 5;

    @Min(0)
    @Max(5)
    private int reviewerCount = 1;

    private boolean councilRequired = true;
    private String status = "ACTIVE";
  }

  @Data
  public static class Topic {
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String description;

    @NotNull private Long cycleId;
    @NotNull private Long departmentId;

    @Min(1)
    @Max(20)
    private int maxGroups = 1;

    private List<Long> supervisorIds = new ArrayList<>();
  }

  @Data
  public static class Group {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull private Long cycleId;
  }

  @Data
  public static class Registration {
    @NotNull private Long groupId;
    @NotNull private Long topicId;
  }

  @Data
  public static class Council {
    private Long id;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull private Long cycleId;
    @NotNull private Long presidentId;
    @NotNull private Long secretaryId;
    private List<Long> memberIds = new ArrayList<>();

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime reportAt;

    @NotBlank
    @Size(max = 150)
    private String location;
  }

  @Data
  public static class Grade {
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal score;

    @NotBlank
    @Size(max = 5000)
    private String comment;

    private boolean submit;
  }

  @Data
  public static class Announcement {
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 15000)
    private String content;

    private String audience = "ALL";
    private Long departmentId;
    private String status = "DRAFT";
  }

  @Data
  public static class Profile {
    @NotBlank
    @Size(max = 100)
    private String fullName;
  }

  @Data
  public static class Password {
    @NotBlank private String currentPassword;

    @Size(min = 10, max = 72)
    @NotBlank
    private String newPassword;
  }
}
