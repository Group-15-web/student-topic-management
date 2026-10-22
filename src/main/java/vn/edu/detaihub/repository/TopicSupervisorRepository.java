package vn.edu.detaihub.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.TopicSupervisor;

@Repository
public interface TopicSupervisorRepository extends JpaRepository<TopicSupervisor, Long> {
  List<TopicSupervisor> findByTopicId(Long topicId);

  boolean existsByTopicIdAndLecturerId(Long topicId, Long lecturerId);

  @Query("SELECT s.topicId FROM TopicSupervisor s WHERE s.lecturerId = :lecturerId")
  List<Long> findTopicIdsByLecturerId(@Param("lecturerId") Long lecturerId);
}
