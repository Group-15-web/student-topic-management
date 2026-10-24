package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.util.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.security.ActorService;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class GroupService {
  private final DomainRepository db;
  private final StudentRepository studentRepo;
  private final StudentGroupRepository groupRepo;
  private final GroupMemberRepository memberRepo;
  private final TopicRegistrationRepository registrationRepo;
  private final ActorService actor;
  private final AuditService audit;
  private final TopicRegistrationService registrationService;
  private final CycleEligibleStudentRepository eligibleRepo;

  public GroupService(
      DomainRepository db,
      StudentRepository studentRepo,
      StudentGroupRepository groupRepo,
      GroupMemberRepository memberRepo,
      TopicRegistrationRepository registrationRepo,
      ActorService actor,
      AuditService audit,
      @Lazy TopicRegistrationService registrationService,
      CycleEligibleStudentRepository eligibleRepo) {
    this.db = db;
    this.studentRepo = studentRepo;
    this.groupRepo = groupRepo;
    this.memberRepo = memberRepo;
    this.registrationRepo = registrationRepo;
    this.actor = actor;
    this.audit = audit;
    this.registrationService = registrationService;
    this.eligibleRepo = eligibleRepo;
  }

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

  public boolean canViewGroup(StudentGroup g) {
    if (actor.admin() || actor.faculty() || member(g.getId())) return true;
    var currentStudent = studentRepo.findByUserId(actor.user().getId());
    if (currentStudent.isPresent()) {
      boolean invited =
          memberRepo
              .findByGroupIdAndStudentIdAndStatus(g.getId(), currentStudent.get().getId(), "INVITED")
              .isPresent();
      if (invited) return true;
    }
    return registrationRepo.findByGroupId(g.getId()).stream().anyMatch(registrationService::canView);
  }

  public List<StudentGroup> visibleGroups() {
    if (actor.admin() || actor.faculty()) {
      return groupRepo.findAll();
    }
    var currentStudent = studentRepo.findByUserId(actor.user().getId());
    if (currentStudent.isPresent()) {
      var groupIds = memberRepo.findGroupIdsByStudentIdAndStatusIn(currentStudent.get().getId(), List.of("ACTIVE", "INVITED"));
      return groupIds.isEmpty() ? List.of() : groupRepo.findByIdIn(groupIds);
    }
    var regs = registrationService.visibleRegistrations();
    var groupIds = regs.stream().map(TopicRegistration::getGroupId).distinct().toList();
    return groupIds.isEmpty() ? List.of() : groupRepo.findByIdIn(groupIds);
  }

  public void assertEditable(StudentGroup g) {
    editable(g);
  }

  private void editable(StudentGroup g) {
    require(g.getStatus().equals("FORMING"), "Nhóm đang đăng ký hoặc đã khóa.");
    var c = db.get(RegistrationCycle.class, g.getCycleId());
    require(
        TopicService.running(c) && TopicService.now().isBefore(c.getStudentEnd()),
        "Đã hết thời gian thay đổi nhóm.");
  }


  private void assertStudentEligibleForCycle(Student s, RegistrationCycle c) {
    if (c.getTargetIntakes() != null && !c.getTargetIntakes().trim().isEmpty()) {
      List<String> allowed = Arrays.stream(c.getTargetIntakes().split("[,;\s]+"))
          .map(String::trim).filter(str -> !str.isEmpty()).toList();
      String studentIntake = s.getIntake() != null ? s.getIntake().trim() : "";
      require(allowed.stream().anyMatch(studentIntake::equalsIgnoreCase),
          "Sinh viên " + s.getCode() + " (Khóa: " + (studentIntake.isEmpty() ? "Chưa cập nhật" : studentIntake)
              + ") không thuộc đối tượng tham gia đợt này (Yêu cầu: " + c.getTargetIntakes() + ").");
    }
    if (Boolean.TRUE.equals(c.getWhitelistRequired())) {
      require(eligibleRepo.existsByCycleIdAndStudentId(c.getId(), s.getId()),
          "Sinh viên " + s.getCode() + " không có tên trong danh sách đủ điều kiện của Khoa cho đợt này.");
    }
  }

  @PreAuthorize("hasRole('STUDENT')")
  public Long create(Forms.Group f) {
    actor.need("STUDENT");
    var s = studentRepo.findByIdWithLock(actor.student().getId())
        .orElseThrow(() -> new RuleException("Không tìm thấy hồ sơ sinh viên.", 404));
    require(!memberRepo.hasActiveGroupInCycle(s.getId(), f.getCycleId()), "Bạn đã có nhóm đang hoạt động trong đợt này.");
    var c = db.get(RegistrationCycle.class, f.getCycleId());
    require(
        TopicService.running(c) && TopicService.now().isBefore(c.getStudentEnd()),
        "Đợt không còn nhận nhóm.");
    assertStudentEligibleForCycle(s, c);
    String trimmedName = f.getName() != null ? f.getName().trim() : "";
    require(!trimmedName.isEmpty(), "Tên nhóm không được để trống.");
    require(!groupRepo.existsByCycleIdAndNameIgnoreCase(f.getCycleId(), trimmedName),
        "Tên nhóm '" + trimmedName + "' đã được sử dụng trong đợt này. Vui lòng chọn tên khác.");
    var g = new StudentGroup();
    g.setName(trimmedName);
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
    var g = groupRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    actor.allow(leader(g));
    editable(g);

    var s = studentRepo.findByCode(studentCode.trim())
        .orElseThrow(() -> new RuleException("Không tìm thấy MSSV."));

    require(!s.getId().equals(g.getLeaderId()), "Không thể tự mời chính mình.");

    var m = memberRepo.findByGroupIdAndStudentId(id, s.getId()).orElse(new GroupMember());
    require(!Objects.equals(m.getStatus(), "ACTIVE"), "Sinh viên đã là thành viên của nhóm.");
    require(!Objects.equals(m.getStatus(), "INVITED"), "Sinh viên đã có lời mời đang chờ phản hồi.");

    require(!memberRepo.hasActiveGroupInCycle(s.getId(), g.getCycleId()), "Sinh viên đã có nhóm đang hoạt động trong đợt này.");

    var cycle = db.get(RegistrationCycle.class, g.getCycleId());
    assertStudentEligibleForCycle(s, cycle);
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
    var g = groupRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    editable(g);

    var s = studentRepo.findByIdWithLock(actor.student().getId())
        .orElseThrow(() -> new RuleException("Không tìm thấy hồ sơ sinh viên.", 404));

    var m = memberRepo.findByGroupIdAndStudentIdAndStatus(id, s.getId(), "INVITED")
        .orElseThrow(() -> new RuleException("Không có lời mời đang chờ."));

    if (accept) {
      require(!memberRepo.hasActiveGroupInCycle(s.getId(), g.getCycleId()), "Bạn đã có nhóm khác trong đợt này.");
      var cycle = db.get(RegistrationCycle.class, g.getCycleId());
      require(members(id).size() < cycle.getMaxMembers(), "Nhóm đã đủ thành viên.");
      s.setActiveGroupId(id);
      studentRepo.save(s);

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
    var g = groupRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    editable(g);

    var s = studentRepo.findByIdWithLock(actor.student().getId())
        .orElseThrow(() -> new RuleException("Không tìm thấy hồ sơ sinh viên.", 404));

    actor.allow(member(id));

    var activeMembers = members(id);
    if (g.getLeaderId().equals(s.getId())) {
      if (activeMembers.size() <= 1) {
        for (var m : activeMembers) {
          m.setStatus("LEFT");
          memberRepo.save(m);
        }
        s.setActiveGroupId(null);
        studentRepo.save(s);
        g.setStatus("DISBANDED");
        groupRepo.save(g);

        // Hủy các đơn đăng ký đang chờ duyệt để giải phóng slot cho đề tài
        var pendings = registrationRepo.findByGroupId(id);
        for (var reg : pendings) {
          if ("PENDING".equals(reg.getStatus())) {
            reg.setStatus("WITHDRAWN");
            reg.setNote("Hệ thống tự động rút do nhóm giải tán.");
            registrationRepo.save(reg);
            audit.record("AUTO_WITHDRAW_REGISTRATION", "registration", reg.getId(), "Nhóm giải tán");
          }
        }

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

  @PreAuthorize("hasAnyRole('STUDENT', 'SYSTEM_ADMIN')")
  public void transfer(Long id, Long studentId, String reason) {
    var g = groupRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));

    boolean isLeader = leader(g);
    boolean isAdmin = actor.admin();
    require(isLeader || isAdmin, "Chỉ trưởng nhóm hoặc Quản trị viên mới có quyền chỉ định trưởng nhóm.");

    if (!isAdmin) {
      editable(g);
    } else {
      String cleanReason = reason != null ? reason.trim() : "";
      require(!cleanReason.isEmpty(), "Admin bắt buộc phải nhập lý do/căn cứ phê duyệt từ Khoa để thực hiện.");
    }

    // Khóa đồng thời bản ghi của sinh viên được chuyển quyền để đảm bảo nhất quán
    require(!studentId.equals(g.getLeaderId()), "Sinh viên này hiện đã là trưởng nhóm.");
    var targetStudent = studentRepo.findByIdWithLock(studentId)
        .orElseThrow(() -> new RuleException("Không tìm thấy sinh viên được chỉ định.", 404));
    require(Objects.equals(targetStudent.getActiveGroupId(), id), "Sinh viên không thuộc nhóm này.");

    boolean isActiveMember =
        memberRepo.findByGroupIdAndStudentIdAndStatus(id, studentId, "ACTIVE").isPresent();
    require(isActiveMember, "Người nhận phải là thành viên chính thức của nhóm.");

    Long oldLeaderId = g.getLeaderId();
    g.setLeaderId(studentId);
    groupRepo.save(g);

    if (isAdmin) {
      audit.record("ADMIN_OVERRIDE_LEADER", "group", id,
          String.format("Admin đổi trưởng nhóm (Cũ: SV %s -> Mới: SV %s). Căn cứ: %s",
              oldLeaderId, targetStudent.getCode(), reason.trim()));
    } else {
      audit.record("TRANSFER_LEADER", "group", id, "Trưởng nhóm chuyển quyền cho SV " + targetStudent.getCode());
    }
  }

  public void transfer(Long id, Long studentId) {
    transfer(id, studentId, null);
  }

  // Delegation methods for backward compatibility during transition
  public boolean canView(TopicRegistration r) {
    return registrationService.canView(r);
  }

  public List<TopicRegistration> visibleRegistrations() {
    return registrationService.visibleRegistrations();
  }

  @PreAuthorize("hasRole('STUDENT')")
  public Long register(Forms.Registration f) {
    return registrationService.register(f);
  }

  @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY_MANAGER')")
  public void decision(Long id, String decision, String note) {
    registrationService.decision(id, decision, note);
  }

  @PreAuthorize("hasAnyRole('FACULTY_MANAGER', 'SYSTEM_ADMIN')")
  public void cancelApprovedRegistration(Long id, String reason) {
    registrationService.cancelApprovedRegistration(id, reason);
  }
}
