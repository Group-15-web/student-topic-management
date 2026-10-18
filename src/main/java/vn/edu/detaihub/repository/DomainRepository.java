package vn.edu.detaihub.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.model.BaseEntity;

/** Shared typed persistence operations. Business decisions belong to services. */
@Repository
@RequiredArgsConstructor
public class DomainRepository {
  private final EntityManager em;

  public <T> List<T> all(Class<T> type) {
    // Reuse catalog reads only inside a read-only transaction. Write transactions
    // always query the database so locks and capacity checks see current rows.
    var key = new ReadKey(this, type);
    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isCurrentTransactionReadOnly()) {
      @SuppressWarnings("unchecked")
      var cached =
          (List<T>)
              org.springframework.transaction.support.TransactionSynchronizationManager.getResource(
                  key);
      if (cached != null) return cached;
      var rows = readAll(type);
      org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(
          key, rows);
      org.springframework.transaction.support.TransactionSynchronizationManager
          .registerSynchronization(
              new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                  org.springframework.transaction.support.TransactionSynchronizationManager
                      .unbindResourceIfPossible(key);
                }
              });
      return rows;
    }
    return readAll(type);
  }

  private record ReadKey(DomainRepository repository, Class<?> type) {}

  private <T> List<T> readAll(Class<T> type) {
    return em.createQuery("select e from " + type.getSimpleName() + " e order by e.id desc", type)
        .getResultList();
  }

  public <T> T get(Class<T> type, Long id) {
    T result = id == null ? null : em.find(type, id);
    if (result == null) throw new RuleException("Không tìm thấy dữ liệu.", 404);
    return result;
  }

  public <T> T lock(Class<T> type, Long id) {
    T result = get(type, id);
    em.refresh(result, LockModeType.PESSIMISTIC_WRITE);
    return result;
  }

  public <T extends BaseEntity> T save(T entity) {
    if (entity.getId() == null) {
      em.persist(entity);
      return entity;
    }
    return em.merge(entity);
  }

  public void delete(Object entity) {
    em.remove(entity);
    em.flush();
  }

  public void flush() {
    em.flush();
  }
}
