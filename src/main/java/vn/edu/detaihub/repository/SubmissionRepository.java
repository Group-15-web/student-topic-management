package vn.edu.detaihub.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.detaihub.model.Submission;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
  List<Submission> findByRegistrationIdOrderByVersionNoDesc(Long registrationId);

  List<Submission> findByRegistrationId(Long registrationId);

  @Query("SELECT COALESCE(MAX(s.versionNo), 0) FROM Submission s WHERE s.registrationId = :registrationId")
  int findMaxVersionNoByRegistrationId(@Param("registrationId") Long registrationId);
}
