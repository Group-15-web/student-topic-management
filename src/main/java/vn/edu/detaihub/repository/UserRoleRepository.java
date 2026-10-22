package vn.edu.detaihub.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.UserRole;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
  List<UserRole> findByUserId(Long userId);

  @Query("SELECT r.code FROM UserRole ur, Role r WHERE ur.roleId = r.id AND ur.userId = :userId")
  List<String> findRoleCodesByUserId(@Param("userId") Long userId);
}
