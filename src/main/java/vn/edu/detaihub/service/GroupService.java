package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class GroupService {
  private final DomainRepository db;
  private final ActorService actor;
  private final TopicService topics;
  private final AuditService audit;

  public List<GroupMember> members(Long id) {
    return db.all(GroupMember.class).stream()
        .filter(m -> m.getGroupId().equals(id) && m.getStatus().equals("ACTIVE"))
        .toList();
  }

  public boolean member(Long id) {
    return db.all(Student.class).stream()
        .filter(s -> s.getUserId().equals(actor.user().getId()))
        .anyMatch(s -> members(id).stream().anyMatch(m -> m.getStudentId().equals(s.getId())));
  }

  public boolean leader(StudentGroup g) {
    return db.get(Student.class, g.getLeaderId()).getUserId().equals(actor.user().getId());
  }

  public boolean canView(TopicRegistration r) {
    if (actor.faculty() || member(r.getGroupId())) return true;
    var t = db.get(Topic.class, r.getTopicId());
    if (actor.departmentHead(t.getDepartmentId())
        || topics.supervises(t.getId(), actor.user().getId())) return true;
    var lecturerIds =
        db.all(Lecturer.class).stream()
            .filter(l -> l.getUserId().equals(actor.user().getId()))
            .map(Lecturer::getId)
            .toList();
    if (db.all(ReviewerAssignment.class).stream()
        .anyMatch(
            a ->
                a.getRegistrationId().equals(r.getId()) && lecturerIds.contains(a.getLecturerId())))
      return true;
    var councilIds =
        db.all(CouncilAssignment.class).stream()
            .filter(a -> a.getRegistrationId().equals(r.getId()))
            .map(CouncilAssignment::getCouncilId)
            .toList();
    return db.all(CouncilMember.class).stream()
        .anyMatch(
            m -> councilIds.contains(m.getCouncilId()) && lecturerIds.contains(m.getLecturerId()));
  }

  public boolean canViewGroup(StudentGroup g) {
    return actor.faculty()
        || member(g.getId())
        || db.all(GroupMember.class).stream()
            .anyMatch(
                m ->
                    m.getGroupId().equals(g.getId())
                        && m.getStatus().equals("INVITED")
                        && db.get(Student.class, m.getStudentId())
                            .getUserId()
                            .equals(actor.user().getId()))
        || db.all(TopicRegistration.class).stream()
            .anyMatch(r -> r.getGroupId().equals(g.getId()) && canView(r));
  }

  private void editable(StudentGroup g) {
    require(g.getStatus().equals("FORMING"), "Nhóm đang đăng ký hoặc đã khóa.");
    var c = db.get(RegistrationCycle.class, g.getCycleId());
    require(
        TopicService.running(c) && TopicService.now().isBefore(c.getStudentEnd()),
        "Đã hết thời gian thay đổi nhóm.");
  }

  public Long create(Forms.Group f) {
    actor.need("STUDENT");
    var s = db.lock(Student.class, actor.student().getId());
    require(s.getActiveGroupId() == null, "Bạn đã có nhóm đang thực hiện.");
    var c = db.get(RegistrationCycle.class, f.getCycleId());
    require(
        TopicService.running(c) && TopicService.now().isBefore(c.getStudentEnd()),
        "Đợt không còn nhận nhóm.");
    var g = new StudentGroup();
    g.setName(f.getName());
    g.setCycleId(f.getCycleId());
    g.setLeaderId(s.getId());
    g.setStatus("FORMING");
    db.save(g);
    var m = new GroupMember();
    m.setGroupId(g.getId());
    m.setStudentId(s.getId());
    m.setStatus("ACTIVE");
    db.save(m);
    s.setActiveGroupId(g.getId());
    audit.record("CREATE_GROUP", "group", g.getId(), g.getName());
    return g.getId();
  }

  public void invite(Long id, String studentCode) {
    var g = db.lock(StudentGroup.class, id);
    actor.allow(leader(g));
    editable(g);
    var s =
        db.all(Student.class).stream()
            .filter(x -> x.getCode().equals(studentCode.trim()))
            .findFirst()
            .orElseThrow(() -> new vn.edu.detaihub.exception.RuleException("Không tìm thấy MSSV."));
    require(s.getActiveGroupId() == null, "SV đã có nhóm đang hoạt động.");
    var m =
        db.all(GroupMember.class).stream()
            .filter(x -> x.getGroupId().equals(id) && x.getStudentId().equals(s.getId()))
            .findFirst()
            .orElse(new GroupMember());
    m.setGroupId(id);
    m.setStudentId(s.getId());
    m.setStatus("INVITED");
    db.save(m);
    audit.record("INVITE_MEMBER", "group", id, studentCode);
  }

  public void respond(Long id, boolean accept) {
    var s = db.lock(Student.class, actor.student().getId());
    var g = db.lock(StudentGroup.class, id);
    editable(g);
    var m =
        db.all(GroupMember.class).stream()
            .filter(
                x ->
                    x.getGroupId().equals(id)
                        && x.getStudentId().equals(s.getId())
                        && x.getStatus().equals("INVITED"))
            .findFirst()
            .orElseThrow(
                () -> new vn.edu.detaihub.exception.RuleException("Không có lời mời đang chờ."));
    if (accept) {
      require(s.getActiveGroupId() == null, "Bạn đã có nhóm khác.");
      require(
          members(id).size() < db.get(RegistrationCycle.class, g.getCycleId()).getMaxMembers(),
          "Nhóm đã đủ thành viên.");
      s.setActiveGroupId(id);
    }
    m.setStatus(accept ? "ACTIVE" : "REJECTED");
    audit.record("RESPOND_INVITATION", "group", id, accept ? "Đồng ý" : "Từ chối");
  }

  public void leave(Long id) {
    var s = db.lock(Student.class, actor.student().getId());
    var g = db.lock(StudentGroup.class, id);
    editable(g);
    actor.allow(member(id));
    require(!g.getLeaderId().equals(s.getId()), "Hãy chuyển trưởng nhóm trước khi rời.");
    for (var m : members(id)) if (m.getStudentId().equals(s.getId())) m.setStatus("LEFT");
    s.setActiveGroupId(null);
    audit.record("LEAVE_GROUP", "group", id, "");
  }

  public void transfer(Long id, Long studentId) {
    var g = db.lock(StudentGroup.class, id);
    actor.allow(leader(g));
    editable(g);
    require(
        members(id).stream().anyMatch(x -> x.getStudentId().equals(studentId)),
        "Người nhận phải là thành viên nhóm.");
    g.setLeaderId(studentId);
    audit.record("TRANSFER_LEADER", "group", id, "Chuyển trưởng nhóm");
  }

  public Long register(Forms.Registration f) {
    var g = db.lock(StudentGroup.class, f.getGroupId());
    actor.allow(leader(g));
    editable(g);
    var t = db.lock(Topic.class, f.getTopicId());
    var c = db.get(RegistrationCycle.class, g.getCycleId());
    require(t.getCycleId().equals(g.getCycleId()), "Nhóm và đề tài không cùng đợt.");
    require(
        TopicService.running(c) && TopicService.between(c.getStudentStart(), c.getStudentEnd()),
        "Chưa mở hoặc đã hết hạn SV đăng ký.");
    require(t.getStatus().equals("PUBLISHED"), "Đề tài chưa công bố.");
    int n = members(g.getId()).size();
    require(
        n >= c.getMinMembers() && n <= c.getMaxMembers(),
        "Nhóm phải có " + c.getMinMembers() + "–" + c.getMaxMembers() + " thành viên.");
    var active =
        db.all(TopicRegistration.class).stream()
            .filter(r -> Set.of("PENDING", "APPROVED").contains(r.getStatus()))
            .toList();
    require(
        active.stream().noneMatch(r -> r.getGroupId().equals(g.getId())),
        "Nhóm đã đăng ký một đề tài.");
    require(
        active.stream().filter(r -> r.getTopicId().equals(t.getId())).count() < t.getMaxGroups(),
        "Đề tài đã hết chỗ.");
    var r = new TopicRegistration();
    r.setGroupId(g.getId());
    r.setTopicId(t.getId());
    r.setStatus("PENDING");
    db.save(r);
    g.setStatus("PENDING");
    audit.record("REGISTER_TOPIC", "registration", r.getId(), t.getTitle());
    return r.getId();
  }

  public void decision(Long id, String decision, String note) {
    var r = db.lock(TopicRegistration.class, id);
    var g = db.lock(StudentGroup.class, r.getGroupId());
    db.lock(Topic.class, r.getTopicId());
    require(r.getStatus().equals("PENDING"), "Đăng ký không còn chờ duyệt.");
    if (decision.equals("withdraw")) {
      actor.allow(leader(g));
      var c = db.get(RegistrationCycle.class, g.getCycleId());
      require(
          TopicService.between(c.getStudentStart(), c.getStudentEnd()), "Đã hết hạn rút đăng ký.");
      r.setStatus("WITHDRAWN");
    } else {
      actor.need("FACULTY_MANAGER");
      require(Set.of("approve", "reject").contains(decision), "Thao tác không hợp lệ.");
      if (decision.equals("reject"))
        require(note != null && !note.isBlank(), "Nhập lý do từ chối.");
      r.setStatus(decision.equals("approve") ? "APPROVED" : "REJECTED");
      r.setDecidedBy(actor.user().getId());
    }
    r.setNote(note);
    g.setStatus(r.getStatus().equals("APPROVED") ? "LOCKED" : "FORMING");
    audit.record("REGISTRATION_" + decision.toUpperCase(), "registration", id, note);
  }
}
