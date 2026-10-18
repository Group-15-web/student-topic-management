package vn.edu.detaihub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.detaihub.model.AuditLog;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
public class AuditService {
  private final DomainRepository db;
  private final ActorService actor;

  public void record(String action, String type, Long id, String details) {
    var a = new AuditLog();
    a.setActorId(actor.user().getId());
    a.setAction(action);
    a.setObjectType(type);
    a.setObjectId(id);
    a.setDetails(details);
    db.save(a);
  }
}
