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
public class TopicRegistrationService {
  private final TopicRegistrationRepository registrationRepo;
  private final StudentGroupRepository groupRepo;
  private final TopicRepository topicRepo;
  private final RegistrationCycleRepository cycleRepo;
  private final StudentRepository studentRepo;
  private final LecturerRepository lecturerRepo;
  private final ReviewerAssignmentRepository reviewerAssignmentRepo;
  private final CouncilAssignmentRepository councilAssignmentRepo;
  private final CouncilMemberRepository councilMemberRepo;
  private final DepartmentRepository departmentRepo;
  private final TopicSupervisorRepository supervisorRepo;
  private final GroupMemberRepository memberRepo;
  private final ActorService actor;
  private final TopicService topics;
  private final GroupService groups;
  private final AuditService audit;
  private final CycleEligibleStudentRepository eligibleRepo;

  public boolean canView(TopicRegistration r) {
    if (actor.faculty() || groups.member(r.getGroupId())) return true;
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

  @PreAuthorize("hasRole('STUDENT')")
  public Long register(Forms.Registration f) {
    // 1. Luôn khóa StudentGroup trước
    var g = groupRepo.findByIdWithLock(f.getGroupId())
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    actor.allow(groups.leader(g));
    groups.assertEditable(g);

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
    var activeMembers = groups.members(g.getId());
    int n = activeMembers.size();
    require(
        n >= c.getMinMembers() && n <= c.getMaxMembers(),
        "Nhóm phải có " + c.getMinMembers() + "–" + c.getMaxMembers() + " thành viên.");

    // Ca biên 4.1: Kiểm tra tư cách toàn bộ thành viên trong nhóm (Target Intakes & Whitelist)
    for (var m : activeMembers) {
      var s = studentRepo.findById(m.getStudentId())
          .orElseThrow(() -> new RuleException("Không tìm thấy thông tin thành viên.", 404));
      if (c.getTargetIntakes() != null && !c.getTargetIntakes().trim().isEmpty()) {
        List<String> allowed = Arrays.stream(c.getTargetIntakes().split("[,;\s]+"))
            .map(String::trim).filter(str -> !str.isEmpty()).toList();
        String studentIntake = s.getIntake() != null ? s.getIntake().trim() : "";
        require(allowed.contains(studentIntake),
            "Thành viên " + s.getCode() + " (Khóa: " + (studentIntake.isEmpty() ? "Chưa cập nhật" : studentIntake)
                + ") không thuộc đối tượng tham gia đợt này (Yêu cầu: " + c.getTargetIntakes() + ").");
      }
      if (Boolean.TRUE.equals(c.getWhitelistRequired())) {
        require(eligibleRepo.existsByCycleIdAndStudentId(c.getId(), s.getId()),
            "Thành viên " + s.getCode() + " không có tên trong danh sách đủ điều kiện của Khoa.");
      }
    }

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
    // 0. Đọc snapshot trước để lấy khóa ngoại, chuẩn hóa Global Lock Ordering (tránh Deadlock)
    var target = registrationRepo.findById(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy đơn đăng ký.", 404));

    // 1. Luôn khóa StudentGroup trước theo thứ tự chuẩn toàn hệ thống
    var g = groupRepo.findByIdWithLock(target.getGroupId())
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));

    // 2. Tiếp theo khóa Topic
    var t = topicRepo.findByIdWithLock(target.getTopicId())
        .orElseThrow(() -> new RuleException("Không tìm thấy đề tài.", 404));

    // 3. Sau cùng khóa TopicRegistration
    var r = registrationRepo.findByIdWithLock(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy đơn đăng ký.", 404));

    require(r.getStatus().equals("PENDING"), "Đăng ký không còn chờ duyệt.");
    if (decision.equals("withdraw")) {
      actor.need("STUDENT");
      actor.allow(groups.leader(g));
      var c = cycleRepo.findById(g.getCycleId())
          .orElseThrow(() -> new RuleException("Không tìm thấy đợt đăng ký.", 404));
      // Ca biên: Chỉ được rút đơn trong khung giờ cho phép của đợt
      require(
          TopicService.between(c.getStudentStart(), c.getStudentEnd()), "Đã hết hạn rút đăng ký.");
      r.setStatus("WITHDRAWN");
    } else {
      actor.need("FACULTY_MANAGER");
      require(Set.of("approve", "reject").contains(decision), "Thao tác không hợp lệ.");
      if (decision.equals("approve")) {
        long approvedCount = registrationRepo.countByTopicIdAndStatusIn(t.getId(), List.of("APPROVED"));
        require(approvedCount < t.getMaxGroups(), "Đề tài đã đủ số lượng nhóm được phê duyệt tối đa.");
      } else {
        require(note != null && note.trim().length() >= 5, "Nhập lý do từ chối cụ thể (tối thiểu 5 ký tự).");
      }
      r.setStatus(decision.equals("approve") ? "APPROVED" : "REJECTED");
      r.setDecidedBy(actor.user().getId());
    }
    r.setNote(note);

    // Bảo toàn trạng thái nhóm: Nếu duyệt -> LOCKED; Nếu từ chối/rút -> chỉ về FORMING nếu chưa có đề tài khác được APPROVED
    if ("APPROVED".equals(r.getStatus())) {
      g.setStatus("LOCKED");
    } else {
      boolean hasApproved = registrationRepo.existsByGroupIdAndStatusIn(g.getId(), List.of("APPROVED"));
      if (!hasApproved) {
        g.setStatus("FORMING");
      }
    }

    registrationRepo.save(r);
    groupRepo.save(g);
    audit.record("REGISTRATION_" + decision.toUpperCase(), "registration", id, note);
  }
}
