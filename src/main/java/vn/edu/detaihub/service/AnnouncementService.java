package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {
  private final DomainRepository db;
  private final ActorService actor;
  private final AuditService audit;

  public boolean visible(Announcement a) {
    if (actor.faculty() || actor.admin() || a.getAuthorId().equals(actor.user().getId()))
      return true;
    if (!a.getStatus().equals("PUBLISHED")) return false;
    if (!a.getAudience().equals("ALL") && !actor.has(a.getAudience())) return false;
    if (a.getDepartmentId() != null)
      return actor.departmentHead(a.getDepartmentId())
          || db.all(Lecturer.class).stream()
              .anyMatch(
                  l ->
                      l.getUserId().equals(actor.user().getId())
                          && l.getDepartmentId().equals(a.getDepartmentId()));
    return true;
  }

  public Long save(Forms.Announcement f) {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER", "DEPARTMENT_HEAD");
    require(
        Set.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(f.getStatus()),
        "Trạng thái không hợp lệ.");
    require(
        Set.of("ALL", "STUDENT", "LECTURER", "FACULTY_MANAGER", "DEPARTMENT_HEAD", "SYSTEM_ADMIN")
            .contains(f.getAudience()),
        "Đối tượng không hợp lệ.");
    if (f.getDepartmentId() != null) db.get(Department.class, f.getDepartmentId());
    var a = f.getId() == null ? new Announcement() : db.lock(Announcement.class, f.getId());
    if (!actor.admin() && !actor.faculty()) {
      require(
          f.getDepartmentId() != null && actor.departmentHead(f.getDepartmentId()),
          "Chỉ tạo nháp thuộc bộ môn mình.");
      require(f.getStatus().equals("DRAFT"), "Trưởng khoa công bố thông báo.");
      if (a.getId() != null)
        actor.allow(a.getAuthorId().equals(actor.user().getId()) && a.getStatus().equals("DRAFT"));
    }
    if (a.getId() == null) a.setAuthorId(actor.user().getId());
    a.setTitle(f.getTitle());
    a.setContent(f.getContent());
    a.setAudience(f.getAudience());
    a.setDepartmentId(f.getDepartmentId());
    a.setStatus(f.getStatus());
    db.save(a);
    audit.record("SAVE_ANNOUNCEMENT", "announcement", a.getId(), a.getStatus());
    return a.getId();
  }

  public void delete(Long id) {
    var a = db.get(Announcement.class, id);
    actor.allow(
        actor.admin()
            || actor.faculty()
            || a.getAuthorId().equals(actor.user().getId()) && a.getStatus().equals("DRAFT"));
    db.delete(a);
    audit.record("DELETE_ANNOUNCEMENT", "announcement", id, "");
  }
}
