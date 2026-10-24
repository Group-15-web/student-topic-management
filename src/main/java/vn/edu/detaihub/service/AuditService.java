package vn.edu.detaihub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.model.AuditLog;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
  private final DomainRepository db;
  private final ActorService actor;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(String action, String type, Long id, String details) {
    try {
      var a = new AuditLog();
      a.setActorId(actor.user() != null ? actor.user().getId() : null);
      a.setAction(action);
      a.setObjectType(type);
      a.setObjectId(id);
      a.setDetails(details);
      db.save(a);
    } catch (Exception e) {
      log.error("Không thể ghi log kiểm toán: {} - {}: {}", action, type, e.getMessage());
    }
  }
}
