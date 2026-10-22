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
  private final UserRoleRepository userRoleRepo;
  private final StudentRepository studentRepo;
  private final LecturerRepository lecturerRepo;
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
    // Tối ưu hóa: Query trực tiếp qua B-Tree Index của MySQL thay vì Full Table Scan db.all()
    return new HashSet<>(userRoleRepo.findRoleCodesByUserId(userId));
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
    // Tối ưu hóa: Tìm profile theo userId bằng B-Tree Index
    return studentRepo.findByUserId(user().getId())
        .orElseThrow(() -> new AccessDeniedException("Cần hồ sơ sinh viên."));
  }

  public Lecturer lecturer() {
    // Tối ưu hóa: Tìm profile theo userId bằng B-Tree Index
    return lecturerRepo.findByUserId(user().getId())
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
