package vn.edu.detaihub.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.CycleEligibleStudent;

@Repository
public interface CycleEligibleStudentRepository extends JpaRepository<CycleEligibleStudent, Long> {
  boolean existsByCycleIdAndStudentId(Long cycleId, Long studentId);

  List<CycleEligibleStudent> findByCycleId(Long cycleId);

  Optional<CycleEligibleStudent> findByCycleIdAndStudentId(Long cycleId, Long studentId);

  long countByCycleId(Long cycleId);

  void deleteByCycleIdAndStudentId(Long cycleId, Long studentId);

  @Query("SELECT e.studentId FROM CycleEligibleStudent e WHERE e.cycleId = :cycleId")
  List<Long> findStudentIdsByCycleId(@Param("cycleId") Long cycleId);
}
