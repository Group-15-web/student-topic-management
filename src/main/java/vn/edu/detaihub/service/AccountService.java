package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
  private final DomainRepository db;
  private final UserAccountRepository users;
  private final ActorService actor;
  private final PasswordEncoder encoder;
  private final AuditService audit;

  @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','FACULTY_MANAGER')")
  public Long save(Forms.Account f) {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    require(
        Set.of("STUDENT", "LECTURER", "DEPARTMENT_HEAD", "FACULTY_MANAGER", "SYSTEM_ADMIN")
            .contains(f.getRole()),
        "Vai trò không hợp lệ.");
    var email = f.getEmail().trim().toLowerCase(Locale.ROOT);
    require(
        users
            .findByEmailIgnoreCase(email)
            .map(u -> Objects.equals(u.getId(), f.getId()))
            .orElse(true),
        "Email đã tồn tại.");
    var u = f.getId() == null ? new UserAccount() : db.lock(UserAccount.class, f.getId());
    if (!actor.admin()) {
      require(f.getId() != null, "Quản trị hệ thống tạo tài khoản; Khoa cập nhật hồ sơ đã có.");
      require(
          actor.roles(u.getId()).contains("STUDENT") || actor.roles(u.getId()).contains("LECTURER"),
          "Khoa chỉ quản lý hồ sơ GV/SV.");
      require(actor.roles(u.getId()).contains(f.getRole()), "Chỉ Admin thay đổi vai trò.");
    }
    boolean fresh = u.getId() == null;
    if (!fresh && !actor.roles(u.getId()).contains(f.getRole()))
      require(false, "Đổi loại hồ sơ qua tạo tài khoản mới; vai trò bổ sung được quản lý riêng.");
    if (fresh) {
      require(actor.admin(), "Chỉ Admin tạo tài khoản.");
      require(
          f.getPassword() != null && f.getPassword().length() >= 10, "Mật khẩu ít nhất 10 ký tự.");
      u.setPasswordHash(encoder.encode(f.getPassword()));
      u.setSecurityVersion(0);
    } else if (actor.admin() && f.getPassword() != null && !f.getPassword().isBlank()) {
      require(f.getPassword().length() >= 10, "Mật khẩu ít nhất 10 ký tự.");
      u.setPasswordHash(encoder.encode(f.getPassword()));
      u.setSecurityVersion(u.getSecurityVersion() + 1);
    }
    if (actor.admin()) {
      require(
          !Objects.equals(actor.user().getId(), u.getId()) || f.isEnabled(),
          "Không thể tự khóa tài khoản đang dùng.");
      u.setEnabled(f.isEnabled());
    }
    u.setEmail(email);
    u.setFullName(f.getFullName());
    db.save(u);
    if (fresh) {
      addRole(u.getId(), f.getRole());
      if (Set.of("DEPARTMENT_HEAD", "FACULTY_MANAGER").contains(f.getRole()))
        addRole(u.getId(), "LECTURER");
    }
    if (f.getRole().equals("STUDENT")) {
      var s =
          db.all(Student.class).stream()
              .filter(x -> x.getUserId().equals(u.getId()))
              .findFirst()
              .orElse(new Student());
      require(
          db.all(Student.class).stream()
              .noneMatch(
                  x -> x.getCode().equals(f.getCode()) && !Objects.equals(x.getId(), s.getId())),
          "MSSV đã tồn tại.");
      s.setUserId(u.getId());
      s.setCode(f.getCode());
      s.setIntake(f.getIntake());
      db.save(s);
    } else if (!f.getRole().equals("SYSTEM_ADMIN")) {
      db.get(Department.class, f.getDepartmentId());
      var l =
          db.all(Lecturer.class).stream()
              .filter(x -> x.getUserId().equals(u.getId()))
              .findFirst()
              .orElse(new Lecturer());
      require(
          db.all(Lecturer.class).stream()
              .noneMatch(
                  x -> x.getCode().equals(f.getCode()) && !Objects.equals(x.getId(), l.getId())),
          "Mã GV đã tồn tại.");
      l.setUserId(u.getId());
      l.setCode(f.getCode());
      l.setDepartmentId(f.getDepartmentId());
      db.save(l);
    }
    audit.record("SAVE_ACCOUNT", "user", u.getId(), "Cập nhật hồ sơ");
    return u.getId();
  }

  private void addRole(Long userId, String code) {
    var role =
        db.all(Role.class).stream().filter(x -> x.getCode().equals(code)).findFirst().orElseThrow();
    var ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(role.getId());
    db.save(ur);
  }

  public void grant(Long userId, String code, boolean grant) {
    actor.need("SYSTEM_ADMIN");
    require(
        !userId.equals(actor.user().getId()), "Không thay đổi quyền của chính phiên đang dùng.");
    var role =
        db.all(Role.class).stream().filter(x -> x.getCode().equals(code)).findFirst().orElseThrow();
    if (grant) {
      if (!actor.roles(userId).contains(code)) {
        if (Set.of("LECTURER", "DEPARTMENT_HEAD", "FACULTY_MANAGER").contains(code))
          require(
              db.all(Lecturer.class).stream().anyMatch(x -> x.getUserId().equals(userId)),
              "Cần hồ sơ giảng viên trước khi cấp quyền học thuật.");
        if (code.equals("STUDENT"))
          require(
              db.all(Student.class).stream().anyMatch(x -> x.getUserId().equals(userId)),
              "Cần hồ sơ sinh viên.");
        addRole(userId, code);
      }
    } else {
      for (var ur : db.all(UserRole.class))
        if (ur.getUserId().equals(userId) && ur.getRoleId().equals(role.getId())) db.delete(ur);
    }
    audit.record("ROLE_CHANGE", "user", userId, code);
  }

  public void toggle(Long id) {
    actor.need("SYSTEM_ADMIN");
    require(!id.equals(actor.user().getId()), "Không tự khóa tài khoản.");
    var u = db.lock(UserAccount.class, id);
    u.setEnabled(!u.getEnabled());
    u.setSecurityVersion(u.getSecurityVersion() + 1);
    audit.record("TOGGLE_ACCOUNT", "user", id, "Thay đổi trạng thái");
  }

  public void delete(Long id) {
    actor.need("SYSTEM_ADMIN");
    require(!id.equals(actor.user().getId()), "Không xóa tài khoản mình.");
    var u = db.get(UserAccount.class, id);
    for (var s : db.all(Student.class)) {
      if (s.getUserId().equals(id)) {
        boolean hasGroups = db.all(GroupMember.class).stream().anyMatch(m -> m.getStudentId().equals(s.getId()))
            || db.all(StudentGroup.class).stream().anyMatch(g -> Objects.equals(g.getLeaderId(), s.getId()));
        require(!hasGroups, "Không thể xóa tài khoản sinh viên đã tham gia nhóm hoặc làm trưởng nhóm. Hãy vô hiệu hóa tài khoản thay vì xóa.");
        for (var e : db.all(CycleEligibleStudent.class)) {
          if (e.getStudentId().equals(s.getId())) db.delete(e);
        }
        db.delete(s);
      }
    }
    for (var l : db.all(Lecturer.class)) {
      if (l.getUserId().equals(id)) {
        boolean hasTopics = db.all(TopicSupervisor.class).stream().anyMatch(ts -> ts.getLecturerId().equals(l.getId()))
            || db.all(ReviewerAssignment.class).stream().anyMatch(ra -> ra.getLecturerId().equals(l.getId()))
            || db.all(CouncilMember.class).stream().anyMatch(cm -> cm.getLecturerId().equals(l.getId()));
        require(!hasTopics, "Không thể xóa tài khoản giảng viên đã có lịch sử hướng dẫn, phản biện hoặc hội đồng. Hãy vô hiệu hóa tài khoản.");
        db.delete(l);
      }
    }
    for (var r : db.all(UserRole.class)) if (r.getUserId().equals(id)) db.delete(r);
    db.delete(u);
    audit.record("DELETE_ACCOUNT", "user", id, "Xóa tài khoản chưa sử dụng");
  }

  public void department(Forms.Department f) {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    var d = f.getId() == null ? new Department() : db.lock(Department.class, f.getId());
    require(
        db.all(Department.class).stream()
            .noneMatch(
                x -> x.getCode().equals(f.getCode()) && !Objects.equals(x.getId(), f.getId())),
        "Mã bộ môn đã tồn tại.");
    if (f.getHeadUserId() != null) {
      require(
          db.all(Lecturer.class).stream()
              .anyMatch(
                  x ->
                      x.getUserId().equals(f.getHeadUserId())
                          && Objects.equals(x.getDepartmentId(), f.getId())),
          "Trưởng bộ môn phải thuộc bộ môn này.");
      require(
          actor.roles(f.getHeadUserId()).contains("DEPARTMENT_HEAD"),
          "Admin cần cấp role Trưởng bộ môn trước.");
    }
    d.setCode(f.getCode());
    d.setName(f.getName());
    d.setHeadUserId(f.getHeadUserId());
    db.save(d);
    audit.record("SAVE_DEPARTMENT", "department", d.getId(), d.getCode());
  }

  public void deleteDepartment(Long id) {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    boolean hasLecturers = db.all(Lecturer.class).stream().anyMatch(l -> Objects.equals(l.getDepartmentId(), id));
    boolean hasTopics = db.all(Topic.class).stream().anyMatch(t -> Objects.equals(t.getDepartmentId(), id));
    require(!hasLecturers && !hasTopics, "Không thể xóa bộ môn đã có giảng viên hoặc đề tài trực thuộc.");
    db.delete(db.get(Department.class, id));
    audit.record("DELETE_DEPARTMENT", "department", id, "");
  }

  public void profile(Forms.Profile f) {
    actor.user().setFullName(f.getFullName());
  }

  public void password(Forms.Password f) {
    var u = actor.user();
    require(
        encoder.matches(f.getCurrentPassword(), u.getPasswordHash()),
        "Mật khẩu hiện tại không đúng.");
    u.setPasswordHash(encoder.encode(f.getNewPassword()));
    u.setSecurityVersion(u.getSecurityVersion() + 1);
    audit.record("CHANGE_PASSWORD", "user", u.getId(), "");
  }
}
