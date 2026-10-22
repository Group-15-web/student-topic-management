package vn.edu.detaihub.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
  @Query("SELECT d.id FROM Department d WHERE d.headUserId = :headUserId")
  List<Long> findDepartmentIdsByHeadUserId(@Param("headUserId") Long headUserId);
}
