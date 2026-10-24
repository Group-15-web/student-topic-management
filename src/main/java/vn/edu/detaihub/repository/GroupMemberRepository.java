package vn.edu.detaihub.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.GroupMember;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
  List<GroupMember> findByGroupIdAndStatus(Long groupId, String status);

  List<GroupMember> findByGroupId(Long groupId);

  List<GroupMember> findByStudentId(Long studentId);

  List<GroupMember> findByStudentIdAndStatus(Long studentId, String status);

  Optional<GroupMember> findByGroupIdAndStudentId(Long groupId, Long studentId);

  Optional<GroupMember> findByGroupIdAndStudentIdAndStatus(Long groupId, Long studentId, String status);

  long countByGroupIdAndStatus(Long groupId, String status);

  long countByGroupIdAndStatusIn(Long groupId, List<String> statuses);

  List<GroupMember> findByGroupIdAndStatusIn(Long groupId, List<String> statuses);

  @Query("SELECT m.groupId FROM GroupMember m WHERE m.studentId = :studentId AND m.status IN :statuses")
  List<Long> findGroupIdsByStudentIdAndStatusIn(@Param("studentId") Long studentId, @Param("statuses") List<String> statuses);

  @Query("SELECT COUNT(m) > 0 FROM GroupMember m JOIN StudentGroup g ON m.groupId = g.id WHERE m.studentId = :studentId AND g.cycleId = :cycleId AND m.status = 'ACTIVE' AND g.status IN ('FORMING', 'PENDING', 'LOCKED')")
  boolean hasActiveGroupInCycle(@Param("studentId") Long studentId, @Param("cycleId") Long cycleId);
}
