package vn.edu.detaihub.dto;

import java.util.*;

/** Read contracts contain only presentation-safe fields, never persistence credentials. */
public final class Views {
  public record Readiness(
      Long id,
      Long cycleId,
      String group,
      String topic,
      String cycle,
      boolean ready,
      String score,
      String reason) {}

  public record Progress(String evaluator, String role, String status) {}

  public record User(Long id, String name, String email, Set<String> roles) {
    public boolean has(String role) {
      return roles.contains(role);
    }
  }

  public record Option(Long id, String label) {}

  public record Topic(
      Long id,
      String title,
      String description,
      String status,
      Long cycleId,
      String cycle,
      Long departmentId,
      String department,
      String author,
      String supervisors,
      int maxGroups,
      long occupied,
      boolean canEdit,
      boolean canSubmit,
      boolean faculty,
      boolean canReview,
      String note) {}

  public record Cycle(
      Long id,
      String name,
      String type,
      String status,
      String phase,
      String lecturerStart,
      String lecturerEnd,
      String studentStart,
      String studentEnd,
      String deadline,
      String reviewerDeadline,
      String councilDate,
      int min,
      int max,
      int reviewers,
      boolean council) {}

  public record Member(
      Long id, String code, String name, String status, boolean leader, boolean me) {}

  public record Group(
      Long id,
      String name,
      String status,
      Long cycleId,
      String cycle,
      int min,
      int max,
      boolean leader,
      boolean member,
      boolean invited,
      boolean editable,
      List<Member> members) {}

  public record Registration(
      Long id,
      Long groupId,
      String group,
      Long topicId,
      String topic,
      Long cycleId,
      String cycle,
      String status,
      String note,
      boolean leader,
      boolean faculty,
      String council,
      String schedule,
      Long councilId,
      boolean president,
      boolean confirmed,
      String result) {}

  public record Submission(Long id, int version, String name, long size, String time) {}

  public record Evaluation(
      Long id,
      Long registrationId,
      String group,
      String topic,
      String evaluator,
      String type,
      String status,
      String score,
      String comment,
      boolean mine) {}

  public record Council(
      Long id,
      String name,
      String cycle,
      String schedule,
      String location,
      List<String> members,
      boolean president) {}

  public record Result(
      Long id,
      Long registrationId,
      String topic,
      String group,
      Long cycleId,
      String cycle,
      String score,
      String status,
      String publishedAt) {}

  public record Announcement(
      Long id,
      String title,
      String content,
      String author,
      String audience,
      String status,
      String time,
      boolean editable) {}

  public record Account(
      Long id,
      String name,
      String email,
      String code,
      String department,
      String role,
      boolean enabled) {}

  public record Department(Long id, String code, String name, String head) {}

  public record Mail(
      Long id,
      String recipient,
      String subject,
      String status,
      int attempts,
      String error,
      boolean canPreview) {}

  public record Audit(
      Long id,
      String actor,
      String action,
      String type,
      Long objectId,
      String details,
      String time) {}

  public record Page<T>(List<T> items, int number, int totalPages, int total, String query) {}
}
