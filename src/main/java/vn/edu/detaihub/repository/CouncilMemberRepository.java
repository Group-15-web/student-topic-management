package vn.edu.detaihub.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.CouncilMember;

@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
  boolean existsByCouncilIdInAndLecturerId(List<Long> councilIds, Long lecturerId);

  @Query("SELECT m.councilId FROM CouncilMember m WHERE m.lecturerId = :lecturerId")
  List<Long> findCouncilIdsByLecturerId(@Param("lecturerId") Long lecturerId);
}
