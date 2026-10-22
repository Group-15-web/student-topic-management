package vn.edu.detaihub.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.Evaluation;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
  List<Evaluation> findByRegistrationId(Long registrationId);

  boolean existsByRegistrationIdAndScoreIsNotNull(Long registrationId);

  boolean existsByRegistrationIdAndLecturerId(Long registrationId, Long lecturerId);
}
