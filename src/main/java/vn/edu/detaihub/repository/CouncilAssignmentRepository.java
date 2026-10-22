package vn.edu.detaihub.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.CouncilAssignment;

@Repository
public interface CouncilAssignmentRepository extends JpaRepository<CouncilAssignment, Long> {
  List<CouncilAssignment> findByRegistrationId(Long registrationId);

  @Query("SELECT a.registrationId FROM CouncilAssignment a WHERE a.councilId IN :councilIds")
  List<Long> findRegistrationIdsByCouncilIdIn(@Param("councilIds") List<Long> councilIds);
}
