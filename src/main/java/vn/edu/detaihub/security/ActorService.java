package vn.edu.detaihub.security;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActorService {
  private final UserAccountRepository users;
  private final DomainRepository db;

  public UserAccount user() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) throw new AccessDeniedException("Vui lòng đăng nhập.");
    return users
        .findByEmailIgnoreCase(auth.getName())
        .filter(UserAccount::getEnabled)
        .orElseThrow(() -> new AccessDeniedException("Tài khoản không hoạt động."));
  }

  public Set<String> roles(Long userId) {
    var ids =
        db.all(UserRole.class).stream()
            .filter(x -> x.getUserId().equals(userId))
            .map(UserRole::getRoleId)
            .toList();
    return new HashSet<>(
        db.all(Role.class).stream()
            .filter(x -> ids.contains(x.getId()))
            .map(Role::getCode)
            .toList());
  }

  public boolean has(String role) {
    return roles(user().getId()).contains(role);
  }

  public boolean faculty() {
    return has("FACULTY_MANAGER");
  }

  public boolean admin() {
    return has("SYSTEM_ADMIN");
  }

  public void need(String... roles) {
    if (Arrays.stream(roles).noneMatch(this::has))
      throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
  }

  public Student student() {
    return db.all(Student.class).stream()
        .filter(x -> x.getUserId().equals(user().getId()))
        .findFirst()
        .orElseThrow(() -> new AccessDeniedException("Cần hồ sơ sinh viên."));
  }

  public Lecturer lecturer() {
    return db.all(Lecturer.class).stream()
        .filter(x -> x.getUserId().equals(user().getId()))
        .findFirst()
        .orElseThrow(() -> new AccessDeniedException("Cần hồ sơ giảng viên."));
  }

  public boolean departmentHead(Long departmentId) {
    return has("DEPARTMENT_HEAD")
        && Objects.equals(db.get(Department.class, departmentId).getHeadUserId(), user().getId());
  }

  public void allow(boolean permitted) {
    if (!permitted) throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu này.");
  }
}
