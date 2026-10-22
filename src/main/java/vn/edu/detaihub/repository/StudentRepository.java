package vn.edu.detaihub.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
  Optional<Student> findByCode(String code);

  Optional<Student> findByUserId(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Student s WHERE s.id = :id")
  Optional<Student> findByIdWithLock(@Param("id") Long id);

  List<Student> findByActiveGroupId(Long activeGroupId);
}
