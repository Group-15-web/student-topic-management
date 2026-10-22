package vn.edu.detaihub.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.StudentGroup;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
  List<StudentGroup> findByCycleId(Long cycleId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM StudentGroup g WHERE g.id = :id")
  Optional<StudentGroup> findByIdWithLock(@Param("id") Long id);

  List<StudentGroup> findByLeaderId(Long leaderId);

  @Query("SELECT g FROM StudentGroup g WHERE g.id IN :ids")
  List<StudentGroup> findByIdIn(@Param("ids") List<Long> ids);
}
