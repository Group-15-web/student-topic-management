package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.time.*;
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
@Transactional
public class TopicService {
  private final DomainRepository db;
  private final ActorService actor;
  private final AuditService audit;

  public static LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
  }

  public static boolean between(LocalDateTime start, LocalDateTime end) {
    return !now().isBefore(start) && now().isBefore(end);
  }

  public static boolean running(RegistrationCycle c) {
    return c.getStatus().equals("ACTIVE");
  }

  public boolean supervises(Long topicId, Long userId) {
    var ids =
        db.all(Lecturer.class).stream()
            .filter(l -> l.getUserId().equals(userId))
            .map(Lecturer::getId)
            .toList();
    return db.all(TopicSupervisor.class).stream()
        .anyMatch(s -> s.getTopicId().equals(topicId) && ids.contains(s.getLecturerId()));
  }

  public boolean visible(Topic t) {
    return t.getStatus().equals("PUBLISHED")
        || actor.faculty()
        || t.getAuthorId().equals(actor.user().getId())
        || actor.departmentHead(t.getDepartmentId())
        || supervises(t.getId(), actor.user().getId());
  }

  @PreAuthorize("hasRole('FACULTY_MANAGER')")
  public Long cycle(Forms.Cycle f) {
    actor.need("FACULTY_MANAGER");
    require(
        Set.of("COURSE", "RESEARCH", "SPECIALIZED_PROJECT", "GRADUATION_THESIS")
            .contains(f.getCycleType()),
        "Loại đợt không hợp lệ.");
    require(
        Set.of("ACTIVE", "CLOSED", "DRAFT", "ARCHIVED").contains(f.getStatus()),
        "Trạng thái không hợp lệ.");
    require(
        f.getLecturerStart().isBefore(f.getLecturerEnd())
            && !f.getLecturerEnd().isAfter(f.getStudentStart())
            && f.getStudentStart().isBefore(f.getStudentEnd()),
        "Thời gian GV và SV phải đúng thứ tự, không chồng lấn.");
    require(
        f.getSubmissionDeadline().isAfter(f.getStudentEnd()),
        "Hạn báo cáo phải sau hạn SV đăng ký.");
    require(f.getMinMembers() <= f.getMaxMembers(), "Số thành viên tối thiểu vượt tối đa.");
    boolean reviewed =
        Set.of("SPECIALIZED_PROJECT", "GRADUATION_THESIS").contains(f.getCycleType());
    require(
        reviewed
            ? f.getReviewerDeadline() != null
                && f.getReviewerDeadline().isAfter(f.getSubmissionDeadline())
            : f.getReviewerDeadline() == null,
        "Hạn GVPB chỉ áp dụng TLCN/KLTN và phải sau hạn báo cáo.");
    require(
        f.getCycleType().equals("GRADUATION_THESIS")
            ? f.getCouncilDate() != null && f.getCouncilDate().isAfter(f.getSubmissionDeadline())
            : f.getCouncilDate() == null,
        "Ngày hội đồng chỉ thiết lập cho KLTN, sau hạn báo cáo.");
    require(
        f.getReviewerCount() > 0 || f.isCouncilRequired(),
        "Phải có ít nhất một hình thức đánh giá.");
    var c =
        f.getId() == null ? new RegistrationCycle() : db.lock(RegistrationCycle.class, f.getId());
    if (c.getId() != null
        && db.all(StudentGroup.class).stream().anyMatch(g -> g.getCycleId().equals(c.getId())))
      require(
          c.getMinMembers() == f.getMinMembers()
              && c.getMaxMembers() == f.getMaxMembers()
              && c.getReviewerCount() == f.getReviewerCount()
              && c.getCouncilRequired() == f.isCouncilRequired()
              && c.getCycleType().equals(f.getCycleType()),
          "Đã có nhóm: không đổi chính sách nhóm/đánh giá/loại đợt.");
    c.setName(f.getName());
    c.setCycleType(f.getCycleType());
    c.setLecturerStart(f.getLecturerStart());
    c.setLecturerEnd(f.getLecturerEnd());
    c.setStudentStart(f.getStudentStart());
    c.setStudentEnd(f.getStudentEnd());
    c.setSubmissionDeadline(f.getSubmissionDeadline());
    c.setReviewerDeadline(f.getReviewerDeadline());
    c.setCouncilDate(f.getCouncilDate());
    c.setMinMembers(f.getMinMembers());
    c.setMaxMembers(f.getMaxMembers());
    c.setReviewerCount(f.getReviewerCount());
    c.setCouncilRequired(f.isCouncilRequired());
    c.setStatus(f.getStatus());
    db.save(c);
    audit.record("SAVE_CYCLE", "cycle", c.getId(), c.getName());
    return c.getId();
  }

  public void deleteCycle(Long id) {
    actor.need("FACULTY_MANAGER");
    db.delete(db.get(RegistrationCycle.class, id));
    audit.record("DELETE_CYCLE", "cycle", id, "");
  }

  public Long save(Forms.Topic f) {
    actor.need("LECTURER", "FACULTY_MANAGER");
    var c = db.get(RegistrationCycle.class, f.getCycleId());
    require(
        running(c) && between(c.getLecturerStart(), c.getLecturerEnd()),
        "Ngoài thời gian GV đề xuất đề tài.");
    db.get(Department.class, f.getDepartmentId());
    var t = f.getId() == null ? new Topic() : db.lock(Topic.class, f.getId());
    if (t.getId() != null) {
      actor.allow(actor.faculty() || t.getAuthorId().equals(actor.user().getId()));
      require(
          Set.of("DRAFT", "RETURNED", "REJECTED").contains(t.getStatus()),
          "Chỉ sửa đề tài nháp hoặc được trả lại.");
      require(t.getCycleId().equals(f.getCycleId()), "Không chuyển đợt của đề tài đã tạo.");
    }
    if (!actor.faculty())
      require(
          actor.lecturer().getDepartmentId().equals(f.getDepartmentId()),
          "Chỉ đề xuất đề tài thuộc bộ môn mình.");
    if (t.getId() == null) {
      t.setAuthorId(actor.user().getId());
      t.setStatus("DRAFT");
    }
    t.setTitle(f.getTitle());
    t.setDescription(f.getDescription());
    t.setCycleId(f.getCycleId());
    t.setDepartmentId(f.getDepartmentId());
    t.setMaxGroups(f.getMaxGroups());
    db.save(t);
    if (actor.faculty()) supervisors(t.getId(), f.getSupervisorIds());
    audit.record("SAVE_TOPIC", "topic", t.getId(), t.getTitle());
    return t.getId();
  }

  public void supervisors(Long topicId, List<Long> ids) {
    actor.need("FACULTY_MANAGER");
    var t = db.lock(Topic.class, topicId);
    var selected = new HashSet<>(ids);
    require(
        selected.size() == ids.size() && ids.size() >= 1 && ids.size() <= 2,
        "Chọn 1–2 GVHD khác nhau.");
    require(
        db.all(TopicRegistration.class).stream()
            .noneMatch(r -> r.getTopicId().equals(topicId) && r.getStatus().equals("APPROVED")),
        "Không đổi GVHD khi đã có nhóm được duyệt; cần xử lý ngoại lệ trước.");
    for (Long id : ids) {
      var l = db.get(Lecturer.class, id);
      require(db.get(UserAccount.class, l.getUserId()).getEnabled(), "GV đã bị khóa.");
    }
    for (var s : db.all(TopicSupervisor.class)) if (s.getTopicId().equals(topicId)) db.delete(s);
    for (Long id : ids) {
      var s = new TopicSupervisor();
      s.setTopicId(topicId);
      s.setLecturerId(id);
      db.save(s);
    }
    audit.record("ASSIGN_SUPERVISORS", "topic", t.getId(), "Phân công GVHD");
  }

  public void transition(Long id, String action, String note) {
    var t = db.lock(Topic.class, id);
    var c = db.get(RegistrationCycle.class, t.getCycleId());
    if (action.equals("submit")) {
      actor.allow(actor.faculty() || t.getAuthorId().equals(actor.user().getId()));
      require(
          running(c) && between(c.getLecturerStart(), c.getLecturerEnd()),
          "Đã hết hạn gửi đề tài.");
      require(
          Set.of("DRAFT", "RETURNED", "REJECTED").contains(t.getStatus()),
          "Trạng thái không cho phép gửi duyệt.");
      t.setStatus("SUBMITTED");
    } else {
      actor.need("FACULTY_MANAGER");
      switch (action) {
        case "approve" -> {
          require(t.getStatus().equals("SUBMITTED"), "Đề tài chưa gửi duyệt.");
          t.setStatus("APPROVED");
          t.setApprovedBy(actor.user().getId());
        }
        case "publish" -> {
          require(t.getStatus().equals("APPROVED"), "Cần duyệt trước khi công bố.");
          long n =
              db.all(TopicSupervisor.class).stream().filter(s -> s.getTopicId().equals(id)).count();
          require(n >= 1 && n <= 2, "Cần 1–2 GVHD trước khi công bố.");
          require(running(c), "Đợt chưa hoạt động.");
          t.setStatus("PUBLISHED");
        }
        case "return", "reject" -> {
          require(t.getStatus().equals("SUBMITTED"), "Chỉ trả lại đề tài đang chờ duyệt.");
          require(note != null && !note.isBlank(), "Nhập lý do xử lý.");
          t.setStatus(action.equals("return") ? "RETURNED" : "REJECTED");
        }
        default -> require(false, "Thao tác không hợp lệ.");
      }
    }
    t.setDecisionNote(note);
    audit.record("TOPIC_" + action.toUpperCase(), "topic", id, note);
  }

  public void review(Long id, String note) {
    var t = db.get(Topic.class, id);
    actor.allow(actor.departmentHead(t.getDepartmentId()));
    require(note != null && !note.isBlank(), "Nhập ý kiến rà soát.");
    audit.record("DEPARTMENT_REVIEW", "topic", id, note);
  }

  public void delete(Long id) {
    var t = db.get(Topic.class, id);
    actor.allow(actor.faculty() || t.getAuthorId().equals(actor.user().getId()));
    require(
        Set.of("DRAFT", "RETURNED", "REJECTED").contains(t.getStatus()),
        "Chỉ xóa bản nháp chưa sử dụng.");
    for (var s : db.all(TopicSupervisor.class)) if (s.getTopicId().equals(id)) db.delete(s);
    db.delete(t);
    audit.record("DELETE_TOPIC", "topic", id, "");
  }
}
