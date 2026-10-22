package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class GroupService {
  private final DomainRepository db;
  private final StudentRepository studentRepo;
  private final StudentGroupRepository groupRepo;
  private final GroupMemberRepository memberRepo;
  private final TopicRegistrationRepository registrationRepo;
  private final TopicRepository topicRepo;
  private final RegistrationCycleRepository cycleRepo;
  private final LecturerRepository lecturerRepo;
  private final ReviewerAssignmentRepository reviewerAssignmentRepo;
  private final CouncilAssignmentRepository councilAssignmentRepo;
  private final CouncilMemberRepository councilMemberRepo;
  private final DepartmentRepository departmentRepo;
  private final TopicSupervisorRepository supervisorRepo;
  private final ActorService actor;
  private final TopicService topics;
  private final AuditService audit;

  public List<GroupMember> members(Long id) {
    return memberRepo.findByGroupIdAndStatus(id, "ACTIVE");
  }

  public boolean member(Long id) {
    return studentRepo
        .findByUserId(actor.user().getId())
        .map(s -> memberRepo.findByGroupIdAndStudentIdAndStatus(id, s.getId(), "ACTIVE").isPresent())
        .orElse(false);
  }

  public boolean leader(StudentGroup g) {
    return studentRepo
        .findById(g.getLeaderId())
        .map(s -> s.getUserId().equals(actor.user().getId()))
        .orElse(false);
  }

  public boolean canView(TopicRegistration r) {
    if (actor.faculty() || member(r.getGroupId())) return true;
    var t = topicRepo.findById(r.getTopicId()).orElse(null);
    if (t == null) return false;
    if (actor.departmentHead(t.getDepartmentId())
        || topics.supervises(t.getId(), actor.user().getId())) return true;
    
    var currentLecturer = lecturerRepo.findByUserId(actor.user().getId());
    if (currentLecturer.isPresent()) {
      Long lecturerId = currentLecturer.get().getId();
      if (reviewerAssignmentRepo.existsByRegistrationIdAndLecturerId(r.getId(), lecturerId)) {
        return true;
      }
      var councilIds = councilAssignmentRepo.findByRegistrationId(r.getId()).stream()
          .map(CouncilAssignment::getCouncilId)
          .toList();
      if (!councilIds.isEmpty() && councilMemberRepo.existsByCouncilIdInAndLecturerId(councilIds, lecturerId)) {
        return true;
      }
    }
    return false;
  }

  public boolean canViewGroup(StudentGroup g) {
    if (actor.faculty() || member(g.getId())) return true;
    var currentStudent = studentRepo.findByUserId(actor.user().getId());
    if (currentStudent.isPresent()) {
      boolean invited =
          memberRepo
              .findByGroupIdAndStudentIdAndStatus(g.getId(), currentStudent.get().getId(), "INVITED")
              .isPresent();
      if (invited) return true;
    }
    return registrationRepo.findByGroupId(g.getId()).stream().anyMatch(this::canView);
  }

  public List<TopicRegistration> visibleRegistrations() {
    if (actor.faculty()) {
      return registrationRepo.findAll();
    }
    var currentStudent = studentRepo.findByUserId(actor.user().getId());
    if (currentStudent.isPresent()) {
      var groupIds = memberRepo.findGroupIdsByStudentIdAndStatusIn(currentStudent.get().getId(), List.of("ACTIVE", "INVITED"));
      return groupIds.isEmpty() ? List.of() : registrationRepo.findByGroupIdIn(groupIds);
    }
    var currentLecturer = lecturerRepo.findByUserId(actor.user().getId());
    if (currentLecturer.isPresent()) {
      Long lecturerId = currentLecturer.get().getId();
      Set<Long> regIds = new HashSet<>();
      
      // 1. Giảng viên hướng dẫn
      var supervisedTopicIds = supervisorRepo.findTopicIdsByLecturerId(lecturerId);
      if (!supervisedTopicIds.isEmpty()) {
        for (var r : registrationRepo.findByTopicIdIn(supervisedTopicIds)) {
          regIds.add(r.getId());
        }
      }
      // 2. Trưởng bộ môn
      var headDeptIds = departmentRepo.findDepartmentIdsByHeadUserId(actor.user().getId());
      if (!headDeptIds.isEmpty()) {
        var deptTopicIds = topicRepo.findTopicIdsByDepartmentIdIn(headDeptIds);
        if (!deptTopicIds.isEmpty()) {
          for (var r : registrationRepo.findByTopicIdIn(deptTopicIds)) {
            regIds.add(r.getId());
          }
        }
      }
      // 3. Phản biện
      regIds.addAll(reviewerAssignmentRepo.findRegistrationIdsByLecturerId(lecturerId));
      // 4. Hội đồng
      var councilIds = councilMemberRepo.findCouncilIdsByLecturerId(lecturerId);
      if (!councilIds.isEmpty()) {
        regIds.addAll(councilAssignmentRepo.findRegistrationIdsByCouncilIdIn(councilIds));
      }
      return regIds.isEmpty() ? List.of() : registrationRepo.findByIdIn(new ArrayList<>(regIds));
    }
    return List.of();
  }

  public List<StudentGroup> visibleGroups() {
    if (actor.faculty()) {
      return groupRepo.findAll();
    }
    var currentStudent = studentRepo.findByUserId(actor.user().getId());
    if (currentStudent.isPresent()) {
      var groupIds = memberRepo.findGroupIdsByStudentIdAndStatusIn(currentStudent.get().getId(), List.of("ACTIVE", "INVITED"));
      return groupIds.isEmpty() ? List.of() : groupRepo.findByIdIn(groupIds);
    }
    // Giảng viên thấy các nhóm thuộc registration mà giảng viên có quyền view
    var regs = visibleRegistrations();
    var groupIds = regs.stream().map(TopicRegistration::getGroupId).distinct().toList();
    return groupIds.isEmpty() ? List.of() : groupRepo.findByIdIn(groupIds);
  }

  private void editable(StudentGroup g) {
    require(g.getStatus().equals("FORMING"), "Nhóm đang đăng ký hoặc đã khóa.");
    var c = db.get(RegistrationCycle.class, g.getCycleId());
    require(
        TopicService.running(c) && TopicService.now().isBefore(c.getStudentEnd()),
        "Đã hết thời gian thay đổi nhóm.");
  }

  @PreAuthorize("hasRole('STUDENT')")
  public Long create(Forms.Group f) {
    actor.need("STUDENT");
    var s = studentRepo.findByIdWithLock(actor.student().getId())
        .orElseThrow(() -> new RuleException("Không tìm thấy hồ sơ sinh viên.", 404));
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
    groupRepo.save(g);
    var m = new GroupMember();
    m.setGroupId(g.getId());
    m.setStudentId(s.getId());
    m.setStatus("ACTIVE");
    memberRepo.save(m);
    s.setActiveGroupId(g.getId());
    studentRepo.save(s);
    audit.record("CREATE_GROUP", "group", g.getId(), g.getName());
    return g.getId();
  }

  @PreAuthorize("hasRole('STUDENT')")
  public void invite(Long id, String studentCode) {
    // 1. Luôn khóa StudentGroup trước
    var g = groupRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    actor.allow(leader(g));
    editable(g);

    // 2. Tìm kiếm sinh viên theo index B-Tree của cột code
    var s = studentRepo.findByCode(studentCode.trim())
        .orElseThrow(() -> new RuleException("Không tìm thấy MSSV."));

    // Ca biên 1: Không được tự mời chính mình
    require(!s.getId().equals(g.getLeaderId()), "Không thể tự mời chính mình.");

    // Ca biên 2: Sinh viên đã là thành viên của chính nhóm này
    var m = memberRepo.findByGroupIdAndStudentId(id, s.getId()).orElse(new GroupMember());
    require(!Objects.equals(m.getStatus(), "ACTIVE"), "Sinh viên đã là thành viên của nhóm.");

    // Ca biên 3: Sinh viên đã có nhóm hoạt động khác
    require(s.getActiveGroupId() == null, "SV đã có nhóm đang hoạt động.");

    // Ca biên 4: Kiểm tra tổng số thành viên + số lời mời đang chờ không vượt quá MaxMembers
    var cycle = db.get(RegistrationCycle.class, g.getCycleId());
    long activeAndInvited = memberRepo.countByGroupIdAndStatusIn(id, List.of("ACTIVE", "INVITED"));
    require(activeAndInvited < cycle.getMaxMembers(), "Nhóm đã đủ chỗ (tính cả lời mời đang chờ).");

    m.setGroupId(id);
    m.setStudentId(s.getId());
    m.setStatus("INVITED");
    memberRepo.save(m);
    audit.record("INVITE_MEMBER", "group", id, studentCode);
  }

  @PreAuthorize("hasRole('STUDENT')")
  public void respond(Long id, boolean accept) {
    // Global Lock Ordering: Luôn khóa StudentGroup trước, sau đó khóa Student
    var g = groupRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    editable(g);

    var s = studentRepo.findByIdWithLock(actor.student().getId())
        .orElseThrow(() -> new RuleException("Không tìm thấy hồ sơ sinh viên.", 404));

    var m = memberRepo.findByGroupIdAndStudentIdAndStatus(id, s.getId(), "INVITED")
        .orElseThrow(() -> new RuleException("Không có lời mời đang chờ."));

    if (accept) {
      require(s.getActiveGroupId() == null, "Bạn đã có nhóm khác.");
      var cycle = db.get(RegistrationCycle.class, g.getCycleId());
      require(members(id).size() < cycle.getMaxMembers(), "Nhóm đã đủ thành viên.");
      s.setActiveGroupId(id);
      studentRepo.save(s);

      // Ca biên 4: Tự động hủy (CANCELLED) tất cả lời mời từ các nhóm khác gửi tới sinh viên này
      for (var otherInvite : memberRepo.findByStudentIdAndStatus(s.getId(), "INVITED")) {
        if (!otherInvite.getGroupId().equals(id)) {
          otherInvite.setStatus("CANCELLED");
          memberRepo.save(otherInvite);
        }
      }
    }
    m.setStatus(accept ? "ACTIVE" : "REJECTED");
    memberRepo.save(m);
    audit.record("RESPOND_INVITATION", "group", id, accept ? "Đồng ý" : "Từ chối");
  }

  @PreAuthorize("hasRole('STUDENT')")
  public void leave(Long id) {
    // Global Lock Ordering: Luôn khóa StudentGroup trước, sau đó khóa Student
    var g = groupRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    editable(g);

    var s = studentRepo.findByIdWithLock(actor.student().getId())
        .orElseThrow(() -> new RuleException("Không tìm thấy hồ sơ sinh viên.", 404));

    actor.allow(member(id));

    var activeMembers = members(id);
    // Ca biên 5: Nếu Trưởng nhóm là thành viên duy nhất còn lại -> Giải tán nhóm
    if (g.getLeaderId().equals(s.getId())) {
      if (activeMembers.size() <= 1) {
        // Trưởng nhóm 1 mình -> Cho phép rời và giải tán nhóm
        for (var m : activeMembers) {
          m.setStatus("LEFT");
          memberRepo.save(m);
        }
        s.setActiveGroupId(null);
        studentRepo.save(s);
        g.setStatus("DISBANDED");
        groupRepo.save(g);
        audit.record("DISBAND_GROUP", "group", id, "Nhóm giải tán do trưởng nhóm rời");
        return;
      } else {
        require(false, "Hãy chuyển trưởng nhóm trước khi rời.");
      }
    }

    for (var m : activeMembers) {
      if (m.getStudentId().equals(s.getId())) {
        m.setStatus("LEFT");
        memberRepo.save(m);
      }
    }
    s.setActiveGroupId(null);
    studentRepo.save(s);
    audit.record("LEAVE_GROUP", "group", id, "");
  }

  @PreAuthorize("hasRole('STUDENT')")
  public void transfer(Long id, Long studentId) {
    var g = groupRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    actor.allow(leader(g));
    editable(g);

    // Ca biên 6: Chỉ chuyển quyền cho thành viên ACTIVE
    boolean isActiveMember =
        memberRepo.findByGroupIdAndStudentIdAndStatus(id, studentId, "ACTIVE").isPresent();
    require(isActiveMember, "Người nhận phải là thành viên chính thức của nhóm.");

    g.setLeaderId(studentId);
    groupRepo.save(g);
    audit.record("TRANSFER_LEADER", "group", id, "Chuyển trưởng nhóm");
  }

  @PreAuthorize("hasRole('STUDENT')")
  public Long register(Forms.Registration f) {
    // 1. Luôn khóa StudentGroup trước
    var g = groupRepo.findByIdWithLock(f.getGroupId())
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    actor.allow(leader(g));
    editable(g);

    // 2. Sau đó khóa Topic bằng Pessimistic Write Lock (chống race condition vượt maxGroups)
    var t = topicRepo.findByIdWithLock(f.getTopicId())
        .orElseThrow(() -> new RuleException("Không tìm thấy đề tài.", 404));
    var c = cycleRepo.findById(g.getCycleId())
        .orElseThrow(() -> new RuleException("Không tìm thấy đợt đăng ký.", 404));

    // Ca biên 1: Nhóm và đề tài phải cùng thuộc một đợt đăng ký
    require(t.getCycleId().equals(g.getCycleId()), "Nhóm và đề tài không cùng đợt.");

    // Ca biên 2: Đợt phải đang ACTIVE và thời gian đăng ký phải hợp lệ
    require(
        TopicService.running(c) && TopicService.between(c.getStudentStart(), c.getStudentEnd()),
        "Chưa mở hoặc đã hết hạn SV đăng ký.");

    // Ca biên 3: Đề tài phải đã được phê duyệt công bố (PUBLISHED)
    require(t.getStatus().equals("PUBLISHED"), "Đề tài chưa công bố.");

    // Ca biên 4: Kiểm tra sĩ số nhóm đủ chuẩn của đợt
    int n = members(g.getId()).size();
    require(
        n >= c.getMinMembers() && n <= c.getMaxMembers(),
        "Nhóm phải có " + c.getMinMembers() + "–" + c.getMaxMembers() + " thành viên.");

    // Ca biên 5: Nhóm không được có đơn PENDING hoặc APPROVED khác
    boolean alreadyRegistered =
        registrationRepo.existsByGroupIdAndStatusIn(g.getId(), List.of("PENDING", "APPROVED"));
    require(!alreadyRegistered, "Nhóm đã đăng ký một đề tài.");

    // Ca biên 6: Kiểm tra số chỗ trống của đề tài qua DB count
    long occupied =
        registrationRepo.countByTopicIdAndStatusIn(t.getId(), List.of("PENDING", "APPROVED"));
    require(occupied < t.getMaxGroups(), "Đề tài đã hết chỗ.");

    var r = new TopicRegistration();
    r.setGroupId(g.getId());
    r.setTopicId(t.getId());
    r.setStatus("PENDING");
    registrationRepo.save(r);

    g.setStatus("PENDING");
    groupRepo.save(g);
    audit.record("REGISTER_TOPIC", "registration", r.getId(), t.getTitle());
    return r.getId();
  }

  @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY_MANAGER')")
  public void decision(Long id, String decision, String note) {
    // 1. Khóa Registration
    var r = registrationRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy đơn đăng ký.", 404));
    // 2. Khóa StudentGroup
    var g = groupRepo.findByIdWithLock(r.getGroupId())
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    // 3. Khóa Topic
    topicRepo.findByIdWithLock(r.getTopicId())
        .orElseThrow(() -> new RuleException("Không tìm thấy đề tài.", 404));

    require(r.getStatus().equals("PENDING"), "Đăng ký không còn chờ duyệt.");
    if (decision.equals("withdraw")) {
      actor.need("STUDENT");
      actor.allow(leader(g));
      var c = cycleRepo.findById(g.getCycleId())
          .orElseThrow(() -> new RuleException("Không tìm thấy đợt đăng ký.", 404));
      // Ca biên: Chỉ được rút đơn trong khung giờ cho phép của đợt
      require(
          TopicService.between(c.getStudentStart(), c.getStudentEnd()), "Đã hết hạn rút đăng ký.");
      r.setStatus("WITHDRAWN");
    } else {
      actor.need("FACULTY_MANAGER");
      require(Set.of("approve", "reject").contains(decision), "Thao tác không hợp lệ.");
      if (decision.equals("reject")) {
        require(note != null && note.trim().length() >= 5, "Nhập lý do từ chối cụ thể (tối thiểu 5 ký tự).");
      }
      r.setStatus(decision.equals("approve") ? "APPROVED" : "REJECTED");
      r.setDecidedBy(actor.user().getId());
    }
    r.setNote(note);
    g.setStatus(r.getStatus().equals("APPROVED") ? "LOCKED" : "FORMING");
    registrationRepo.save(r);
    groupRepo.save(g);
    audit.record("REGISTRATION_" + decision.toUpperCase(), "registration", id, note);
  }
}
