package vn.edu.detaihub.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.ReviewerAssignment;

@Repository
public interface ReviewerAssignmentRepository extends JpaRepository<ReviewerAssignment, Long> {
  boolean existsByRegistrationIdAndLecturerId(Long registrationId, Long lecturerId);
  List<ReviewerAssignment> findByRegistrationId(Long registrationId);

  @Query("SELECT a.registrationId FROM ReviewerAssignment a WHERE a.lecturerId = :lecturerId")
  List<Long> findRegistrationIdsByLecturerId(@Param("lecturerId") Long lecturerId);
}
