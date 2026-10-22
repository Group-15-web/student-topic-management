package vn.edu.detaihub.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.TopicRegistration;

@Repository
public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {
  List<TopicRegistration> findByGroupId(Long groupId);

  List<TopicRegistration> findByTopicId(Long topicId);

  List<TopicRegistration> findByStatusIn(List<String> statuses);

  long countByTopicIdAndStatusIn(Long topicId, List<String> statuses);

  boolean existsByGroupIdAndStatusIn(Long groupId, List<String> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM TopicRegistration r WHERE r.id = :id")
  Optional<TopicRegistration> findByIdWithLock(@Param("id") Long id);

  @Query("SELECT r FROM TopicRegistration r WHERE r.groupId IN :groupIds")
  List<TopicRegistration> findByGroupIdIn(@Param("groupIds") List<Long> groupIds);

  @Query("SELECT r FROM TopicRegistration r WHERE r.topicId IN :topicIds")
  List<TopicRegistration> findByTopicIdIn(@Param("topicIds") List<Long> topicIds);

  @Query("SELECT r FROM TopicRegistration r WHERE r.id IN :ids")
  List<TopicRegistration> findByIdIn(@Param("ids") List<Long> ids);
}
