package vn.edu.detaihub.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.Lecturer;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
  Optional<Lecturer> findByUserId(Long userId);

  Optional<Lecturer> findByCode(String code);
}
