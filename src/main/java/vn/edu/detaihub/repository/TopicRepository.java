package vn.edu.detaihub.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
  List<Topic> findByCycleId(Long cycleId);

  List<Topic> findByDepartmentId(Long departmentId);

  List<Topic> findByStatus(String status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM Topic t WHERE t.id = :id")
  Optional<Topic> findByIdWithLock(@Param("id") Long id);

  @Query("SELECT t.id FROM Topic t WHERE t.departmentId IN :departmentIds")
  List<Long> findTopicIdsByDepartmentIdIn(@Param("departmentIds") List<Long> departmentIds);
}
