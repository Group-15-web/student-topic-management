package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class AssessmentService {
  public List<vn.edu.detaihub.dto.Views.Readiness> readiness() {
    actor.need("FACULTY_MANAGER");
    return db.all(TopicRegistration.class).stream()
        .filter(r -> r.getStatus().equals("APPROVED"))
        .filter(
            r ->
                db.all(EvaluationResult.class).stream()
                    .noneMatch(
                        e ->
                            e.getRegistrationId().equals(r.getId())
                                && e.getStatus().equals("PUBLISHED")))
        .map(
            r -> {
              var t = db.get(Topic.class, r.getTopicId());
              String score = null;
              String reason = "Đủ điều kiện công bố";
              try {
                score = finalScore(r).toString();
              } catch (vn.edu.detaihub.exception.RuleException e) {
                reason = e.getMessage();
              }
              return new vn.edu.detaihub.dto.Views.Readiness(
                  r.getId(),
                  t.getCycleId(),
                  db.get(StudentGroup.class, r.getGroupId()).getName(),
                  t.getTitle(),
                  cycle(r).getName(),
                  score != null,
                  score,
                  reason);
            })
        .toList();
  }

  public void publishBatch(List<Long> ids) {
    actor.need("FACULTY_MANAGER");
    require(ids != null && !ids.isEmpty(), "Chọn ít nhất một hồ sơ đủ điều kiện.");
    for (Long id : ids.stream().distinct().sorted().toList()) publish(id);
  }

  private final DomainRepository db;
  private final ActorService actor;
  private final TopicService topics;
  private final GroupService groups;
  private final AuditService audit;

  public List<Evaluation> evaluations(Long registrationId) {
    return db.all(Evaluation.class).stream()
        .filter(e -> e.getRegistrationId().equals(registrationId))
        .toList();
  }

  public Optional<CouncilAssignment> councilAssignment(Long registrationId) {
    return db.all(CouncilAssignment.class).stream()
        .filter(a -> a.getRegistrationId().equals(registrationId))
        .findFirst();
  }

  public boolean president(Long councilId) {
    return db.all(CouncilMember.class).stream()
        .anyMatch(
            m ->
                m.getCouncilId().equals(councilId)
                    && m.getMemberRole().equals("PRESIDENT")
                    && db.get(Lecturer.class, m.getLecturerId())
                        .getUserId()
                        .equals(actor.user().getId()));
  }

  public boolean councilVisible(ReviewCouncil c) {
    return actor.faculty()
        || db.all(CouncilMember.class).stream()
            .anyMatch(
                m ->
                    m.getCouncilId().equals(c.getId())
                        && db.get(Lecturer.class, m.getLecturerId())
                            .getUserId()
                            .equals(actor.user().getId()))
        || db.all(CouncilAssignment.class).stream()
            .anyMatch(
                a ->
                    a.getCouncilId().equals(c.getId())
                        && groups.canView(db.get(TopicRegistration.class, a.getRegistrationId())));
  }

  private RegistrationCycle cycle(TopicRegistration r) {
    return db.get(RegistrationCycle.class, db.get(Topic.class, r.getTopicId()).getCycleId());
  }

  private void eligible(TopicRegistration r, Long lecturerId) {
    var l = db.get(Lecturer.class, lecturerId);
    require(db.get(UserAccount.class, l.getUserId()).getEnabled(), "GV không hoạt động.");
    require(
        !topics.supervises(r.getTopicId(), l.getUserId()),
        "GVHD/đồng GVHD không được chấm đề tài mình hướng dẫn.");
  }

  private void approved(TopicRegistration r) {
    require(r.getStatus().equals("APPROVED"), "Nhóm chưa được duyệt.");
    require(
        db.all(EvaluationResult.class).stream()
            .noneMatch(
                e -> e.getRegistrationId().equals(r.getId()) && e.getStatus().equals("PUBLISHED")),
        "Kết quả đã công bố; cần thu hồi trước.");
  }

  public void reviewers(Long registrationId, List<Long> lecturerIds) {
    actor.need("FACULTY_MANAGER");
    var r = db.lock(TopicRegistration.class, registrationId);
    approved(r);
    require(new HashSet<>(lecturerIds).size() == lecturerIds.size(), "GV phản biện bị trùng.");
    require(
        lecturerIds.size() == cycle(r).getReviewerCount(),
        "Phải chọn đủ số GVPB theo đợt: " + cycle(r).getReviewerCount());
    require(
        evaluations(registrationId).stream()
            .filter(e -> e.getEvaluationType().equals("REVIEWER"))
            .noneMatch(e -> e.getStatus().equals("SUBMITTED")),
        "Đã có phiếu gửi; không thay phân công.");
    for (Long id : lecturerIds) {
      eligible(r, id);
      require(
          evaluations(registrationId).stream()
              .noneMatch(
                  e -> e.getEvaluationType().equals("COUNCIL") && e.getLecturerId().equals(id)),
          "Không phân công cùng GV ở cả phản biện và hội đồng của đề tài.");
    }
    for (var e : evaluations(registrationId))
      if (e.getEvaluationType().equals("REVIEWER")) db.delete(e);
    for (var a : db.all(ReviewerAssignment.class))
      if (a.getRegistrationId().equals(registrationId)) db.delete(a);
    for (Long id : lecturerIds) {
      var a = new ReviewerAssignment();
      a.setRegistrationId(registrationId);
      a.setLecturerId(id);
      db.save(a);
      createEvaluation(registrationId, id, "REVIEWER");
    }
    audit.record("ASSIGN_REVIEWERS", "registration", registrationId, "");
  }

  private void createEvaluation(Long registrationId, Long lecturerId, String type) {
    var e = new Evaluation();
    e.setRegistrationId(registrationId);
    e.setLecturerId(lecturerId);
    e.setEvaluationType(type);
    e.setStatus("DRAFT");
    db.save(e);
  }

  public Long council(Forms.Council f) {
    actor.need("FACULTY_MANAGER");
    var c = db.get(RegistrationCycle.class, f.getCycleId());
    require(c.getCouncilRequired(), "Đợt này không yêu cầu hội đồng.");
    require(!f.getPresidentId().equals(f.getSecretaryId()), "Chủ tịch và thư ký phải khác nhau.");
    var ids = new ArrayList<>(f.getMemberIds());
    ids.add(f.getPresidentId());
    ids.add(f.getSecretaryId());
    require(
        ids.size() >= 3 && ids.size() <= 5 && new HashSet<>(ids).size() == ids.size(),
        "Hội đồng phải có 3–5 GV không trùng nhau.");
    require(
        f.getReportAt().isAfter(c.getSubmissionDeadline()),
        "Lịch báo cáo phải sau hạn nộp báo cáo.");
    for (var existing : db.all(ReviewCouncil.class))
      if (!Objects.equals(existing.getId(), f.getId())
          && existing.getReportAt().equals(f.getReportAt())) {
        require(
            !existing.getLocation().equalsIgnoreCase(f.getLocation()),
            "Phòng đã có lịch hội đồng cùng giờ.");
        require(
            db.all(CouncilMember.class).stream()
                .noneMatch(
                    m ->
                        m.getCouncilId().equals(existing.getId())
                            && ids.contains(m.getLecturerId())),
            "GV bị trùng lịch hội đồng.");
      }
    for (Long id : ids) {
      var l = db.get(Lecturer.class, id);
      require(db.get(UserAccount.class, l.getUserId()).getEnabled(), "GV không hoạt động.");
    }
    var council = f.getId() == null ? new ReviewCouncil() : db.lock(ReviewCouncil.class, f.getId());
    if (f.getId() != null) {
      require(
          councilAssignmentForCouncil(f.getId()).isEmpty(),
          "Hội đồng đã được giao nhóm; không sửa thành viên hoặc lịch qua CRUD.");
      for (var member : db.all(CouncilMember.class))
        if (member.getCouncilId().equals(f.getId())) db.delete(member);
    }
    council.setName(f.getName());
    council.setCycleId(f.getCycleId());
    council.setReportAt(f.getReportAt());
    council.setLocation(f.getLocation());
    db.save(council);
    for (Long id : ids) {
      var m = new CouncilMember();
      m.setCouncilId(council.getId());
      m.setLecturerId(id);
      m.setMemberRole(
          id.equals(f.getPresidentId())
              ? "PRESIDENT"
              : id.equals(f.getSecretaryId()) ? "SECRETARY" : "MEMBER");
      db.save(m);
    }
    audit.record("CREATE_COUNCIL", "council", council.getId(), council.getName());
    return council.getId();
  }

  private List<CouncilAssignment> councilAssignmentForCouncil(Long id) {
    return db.all(CouncilAssignment.class).stream()
        .filter(a -> a.getCouncilId().equals(id))
        .toList();
  }

  public Forms.Council councilForm(Long id) {
    actor.need("FACULTY_MANAGER");
    var f = new Forms.Council();
    if (id == null) return f;
    var c = db.get(ReviewCouncil.class, id);
    org.springframework.beans.BeanUtils.copyProperties(c, f);
    var members =
        db.all(CouncilMember.class).stream().filter(m -> m.getCouncilId().equals(id)).toList();
    for (var m : members) {
      switch (m.getMemberRole()) {
        case "PRESIDENT" -> f.setPresidentId(m.getLecturerId());
        case "SECRETARY" -> f.setSecretaryId(m.getLecturerId());
        default -> f.getMemberIds().add(m.getLecturerId());
      }
    }
    return f;
  }

  public void assignCouncil(Long councilId, Long registrationId) {
    actor.need("FACULTY_MANAGER");
    var r = db.lock(TopicRegistration.class, registrationId);
    approved(r);
    var c = db.get(ReviewCouncil.class, councilId);
    require(c.getCycleId().equals(cycle(r).getId()), "Hội đồng và nhóm phải cùng đợt.");
    require(councilAssignment(registrationId).isEmpty(), "Nhóm đã có hội đồng.");
    var members =
        db.all(CouncilMember.class).stream()
            .filter(m -> m.getCouncilId().equals(councilId))
            .toList();
    require(members.size() >= 3 && members.size() <= 5, "Hội đồng chưa đủ người.");
    for (var m : members) {
      eligible(r, m.getLecturerId());
      require(
          evaluations(registrationId).stream()
              .noneMatch(e -> e.getLecturerId().equals(m.getLecturerId())),
          "GV đã được phân công phản biện đề tài này.");
    }
    var a = new CouncilAssignment();
    a.setRegistrationId(registrationId);
    a.setCouncilId(councilId);
    db.save(a);
    for (var m : members) createEvaluation(registrationId, m.getLecturerId(), "COUNCIL");
    audit.record("ASSIGN_COUNCIL", "registration", registrationId, c.getName());
  }

  public void removeCouncil(Long id) {
    actor.need("FACULTY_MANAGER");
    require(
        db.all(CouncilAssignment.class).stream().noneMatch(a -> a.getCouncilId().equals(id)),
        "Hội đồng đã có nhóm; không thể xóa.");
    for (var m : db.all(CouncilMember.class)) if (m.getCouncilId().equals(id)) db.delete(m);
    db.delete(db.get(ReviewCouncil.class, id));
    audit.record("DELETE_COUNCIL", "council", id, "");
  }

  @PreAuthorize("hasRole('LECTURER')")
  public void grade(Long evaluationId, Forms.Grade f) {
    var e0 = db.get(Evaluation.class, evaluationId);
    var r = db.lock(TopicRegistration.class, e0.getRegistrationId());
    var e = db.lock(Evaluation.class, evaluationId);
    approved(r);
    actor.allow(db.get(Lecturer.class, e.getLecturerId()).getUserId().equals(actor.user().getId()));
    eligible(r, e.getLecturerId());
    require(e.getStatus().equals("DRAFT"), "Phiếu đã gửi, không được tự sửa.");
    require(
        f.getScore() != null
            && f.getScore().compareTo(BigDecimal.ZERO) >= 0
            && f.getScore().compareTo(BigDecimal.TEN) <= 0,
        "Điểm phải từ 0 đến 10.");
    require(
        db.all(Submission.class).stream().anyMatch(s -> s.getRegistrationId().equals(r.getId())),
        "Nhóm chưa nộp báo cáo.");
    var c = cycle(r);
    require(TopicService.running(c), "Đợt không hoạt động.");
    if (e.getEvaluationType().equals("REVIEWER")) {
      require(
          db.all(ReviewerAssignment.class).stream()
              .anyMatch(
                  a ->
                      a.getRegistrationId().equals(r.getId())
                          && a.getLecturerId().equals(e.getLecturerId())),
          "Phân công không còn hiệu lực.");
      if (c.getReviewerDeadline() != null)
        require(TopicService.now().isBefore(c.getReviewerDeadline()), "Đã hết hạn GVPB nộp điểm.");
    } else {
      var a = councilAssignment(r.getId()).orElseThrow();
      require(
          db.all(CouncilMember.class).stream()
              .anyMatch(
                  m ->
                      m.getCouncilId().equals(a.getCouncilId())
                          && m.getLecturerId().equals(e.getLecturerId())),
          "Không thuộc hội đồng này.");
    }
    e.setScore(f.getScore().setScale(2, RoundingMode.HALF_UP));
    e.setComment(f.getComment());
    e.setStatus(f.isSubmit() ? "SUBMITTED" : "DRAFT");
    audit.record(
        f.isSubmit() ? "SUBMIT_GRADE" : "SAVE_GRADE",
        "evaluation",
        e.getId(),
        "Phiếu " + e.getEvaluationType());
  }

  public void confirm(Long registrationId) {
    var r = db.lock(TopicRegistration.class, registrationId);
    approved(r);
    var a = councilAssignment(registrationId).orElseThrow();
    actor.allow(president(a.getCouncilId()));
    var es =
        evaluations(registrationId).stream()
            .filter(e -> e.getEvaluationType().equals("COUNCIL"))
            .toList();
    require(
        es.size() >= 3 && es.stream().allMatch(e -> e.getStatus().equals("SUBMITTED")),
        "Chưa đủ phiếu hội đồng đã gửi.");
    a.setConfirmedBy(actor.user().getId());
    a.setConfirmedAt(TopicService.now());
    audit.record("CONFIRM_COUNCIL", "registration", registrationId, "");
  }

  public BigDecimal finalScore(TopicRegistration r) {
    var c = cycle(r);
    var es = evaluations(r.getId());
    long reviewer = es.stream().filter(e -> e.getEvaluationType().equals("REVIEWER")).count();
    require(reviewer == c.getReviewerCount(), "Chưa đủ phân công phản biện.");
    if (c.getCouncilRequired()) {
      var a =
          councilAssignment(r.getId())
              .orElseThrow(
                  () -> new vn.edu.detaihub.exception.RuleException("Chưa phân công hội đồng."));
      require(a.getConfirmedAt() != null, "Chủ tịch chưa xác nhận kết quả.");
      long expected =
          db.all(CouncilMember.class).stream()
              .filter(m -> m.getCouncilId().equals(a.getCouncilId()))
              .count();
      require(
          es.stream().filter(e -> e.getEvaluationType().equals("COUNCIL")).count() == expected,
          "Thiếu phiếu thành viên hội đồng.");
    }
    require(
        !es.isEmpty()
            && es.stream().allMatch(e -> e.getStatus().equals("SUBMITTED") && e.getScore() != null),
        "Chưa đủ điểm đã gửi.");
    for (var e : es) eligible(r, e.getLecturerId());
    return es.stream()
        .map(Evaluation::getScore)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(es.size()), 2, RoundingMode.HALF_UP);
  }

  public void publish(Long registrationId) {
    actor.need("FACULTY_MANAGER");
    var r = db.lock(TopicRegistration.class, registrationId);
    require(r.getStatus().equals("APPROVED"), "Đăng ký chưa được duyệt.");
    var result =
        db.all(EvaluationResult.class).stream()
            .filter(x -> x.getRegistrationId().equals(registrationId))
            .findFirst()
            .orElse(new EvaluationResult());
    if ("PUBLISHED".equals(result.getStatus())) return;
    var score = finalScore(r);
    result.setRegistrationId(registrationId);
    result.setFinalScore(score);
    result.setStatus("PUBLISHED");
    result.setPublicationVersion(
        result.getPublicationVersion() == null ? 1 : result.getPublicationVersion() + 1);
    result.setPublishedBy(actor.user().getId());
    result.setPublishedAt(TopicService.now());
    db.save(result);
    var t = db.get(Topic.class, r.getTopicId());
    for (var m : groups.members(r.getGroupId())) {
      var s = db.get(Student.class, m.getStudentId());
      var u = db.get(UserAccount.class, s.getUserId());
      var mail = new EmailDelivery();
      mail.setResultId(result.getId());
      mail.setPublicationVersion(result.getPublicationVersion());
      mail.setRecipientId(u.getId());
      mail.setRecipient(u.getEmail());
      mail.setSubject("Kết quả đề tài: " + t.getTitle());
      mail.setBody(
          "Chào "
              + u.getFullName()
              + ",\nKết quả đề tài “"
              + t.getTitle()
              + "” thuộc đợt "
              + cycle(r).getName()
              + " đã được công bố.\nĐiểm trung bình: "
              + score
              + " / 10.\nVui lòng đăng nhập Đề Tài Hub để xem chi tiết.");
      mail.setStatus("PENDING");
      mail.setAttempts(0);
      db.save(mail);
    }
    audit.record(
        "PUBLISH_RESULT",
        "registration",
        registrationId,
        "Phiên bản " + result.getPublicationVersion());
  }

  public void reopen(Long registrationId, String reason) {
    actor.need("FACULTY_MANAGER");
    require(
        reason != null && reason.trim().length() >= 10,
        "Nhập lý do điều chỉnh (ít nhất 10 ký tự).");
    var r = db.lock(TopicRegistration.class, registrationId);
    var g = db.get(StudentGroup.class, r.getGroupId());
    require(!g.getStatus().equals("COMPLETED"), "Nhóm đã lưu trữ; cần xử lý quản trị riêng.");
    for (var result : db.all(EvaluationResult.class))
      if (result.getRegistrationId().equals(registrationId)) {
        result.setStatus("WITHDRAWN");
        for (var mail : db.all(EmailDelivery.class))
          if (mail.getResultId().equals(result.getId())
              && Set.of("PENDING", "FAILED").contains(mail.getStatus()))
            mail.setStatus("CANCELLED");
      }
    for (var e : evaluations(registrationId)) {
      audit.record(
          "GRADE_SNAPSHOT", "evaluation", e.getId(), "Điểm cũ=" + e.getScore() + "; " + reason);
      e.setStatus("DRAFT");
    }
    councilAssignment(registrationId)
        .ifPresent(
            a -> {
              a.setConfirmedAt(null);
              a.setConfirmedBy(null);
            });
    audit.record("REOPEN_RESULT", "registration", registrationId, reason);
  }

  public void complete(Long registrationId) {
    actor.need("FACULTY_MANAGER");
    var r = db.lock(TopicRegistration.class, registrationId);
    require(
        db.all(EvaluationResult.class).stream()
            .anyMatch(
                e ->
                    e.getRegistrationId().equals(registrationId)
                        && e.getStatus().equals("PUBLISHED")),
        "Cần công bố kết quả trước khi lưu trữ nhóm.");
    var g = db.lock(StudentGroup.class, r.getGroupId());
    g.setStatus("COMPLETED");
    for (var m : groups.members(g.getId())) {
      var s = db.lock(Student.class, m.getStudentId());
      if (Objects.equals(s.getActiveGroupId(), g.getId())) s.setActiveGroupId(null);
    }
    audit.record("COMPLETE_GROUP", "group", g.getId(), "");
  }
}
