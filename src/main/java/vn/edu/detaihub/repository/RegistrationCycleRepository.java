package vn.edu.detaihub.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.RegistrationCycle;

@Repository
public interface RegistrationCycleRepository extends JpaRepository<RegistrationCycle, Long> {
  Optional<RegistrationCycle> findByName(String name);
}
