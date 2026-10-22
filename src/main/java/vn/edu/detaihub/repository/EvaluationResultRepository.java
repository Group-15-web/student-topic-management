package vn.edu.detaihub.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.EvaluationResult;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {
  Optional<EvaluationResult> findByRegistrationIdAndStatus(Long registrationId, String status);
}
