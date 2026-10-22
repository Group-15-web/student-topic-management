package vn.edu.detaihub;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.service.GroupService;
import vn.edu.detaihub.service.SubmissionService;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:securityedge;MODE=MySQL;DB_CLOSE_DELAY=-1}",
      "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}",
      "spring.datasource.username=${TEST_DB_USER:sa}",
      "spring.datasource.password=${TEST_DB_PASSWORD:}",
      "app.mail-worker=false"
    })
class SecurityEdgeCasesTest {
  @Autowired GroupService groups;
  @Autowired SubmissionService submissions;
  @Autowired UserDetailsService users;
  @Autowired StudentRepository studentRepo;
  @Autowired StudentGroupRepository groupRepo;
  @Autowired GroupMemberRepository memberRepo;
  @Autowired TopicRegistrationRepository registrationRepo;
  @Autowired SubmissionRepository submissionRepo;
  @Autowired TransactionTemplate tx;

  @Value("${app.storage}")
  private String storage;

  private void asUser(String email) {
    var u = users.loadUserByUsername(email);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
  }

  @Test
  void nonStudentCannotCreateGroup() {
    // Giảng viên (gv1@demo.local) cố tình gọi create group
    asUser("gv1@demo.local");
    var f = new Forms.Group();
    f.setName("Nhóm của giảng viên");
    f.setCycleId(1L);

    assertThatThrownBy(() -> groups.create(f))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void nonStudentCannotInviteMember() {
    // Giảng viên cố tình gọi invite member vào nhóm
    asUser("gv1@demo.local");
    assertThatThrownBy(() -> groups.invite(1L, "SV003"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void nonLeaderCannotInviteMember() {
    // sv2 là thành viên nhóm 1 (ACTIVE), nhưng không phải leader (sv1 là leader)
    asUser("sv2@demo.local");
    assertThatThrownBy(() -> groups.invite(1L, "SV003"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void nonStudentCannotUploadSubmission() {
    // Giảng viên cố tình upload bài nộp
    asUser("gv1@demo.local");
    var file = new MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes());

    assertThatThrownBy(() -> submissions.upload(1L, file))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void nonLeaderStudentCannotUploadSubmission() {
    // sv5 là thành viên của Nhóm 2 nhưng leader của Nhóm 2 là sv4
    asUser("sv5@demo.local");
    var file = new MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes());

    assertThatThrownBy(() -> submissions.upload(1L, file))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void outsiderStudentCannotDownloadSubmission() throws IOException {
    // DemoData tạo r (Registration id = 1L) thuộc work (Nhóm 2: sv4, sv5, sv6)
    var dir = Path.of(storage).toAbsolutePath().normalize();
    Files.createDirectories(dir);
    String key = UUID.randomUUID() + ".pdf";
    Path filePath = dir.resolve(key);
    Files.write(filePath, "%PDF-1.4 test download".getBytes());

    Long subId = tx.execute(status -> {
      var s = new Submission();
      s.setRegistrationId(1L); // Nhóm 2
      s.setVersionNo(1);
      s.setStorageKey(key);
      s.setFileName("report.pdf");
      s.setContentType("application/pdf");
      s.setFileSize(100L);
      s.setSubmittedBy(4L);
      submissionRepo.save(s);
      return s.getId();
    });

    try {
      // sv1 là sinh viên thuộc Nhóm 1 (ngoài Nhóm 2) -> bị cấm 403
      asUser("sv1@demo.local");
      assertThatThrownBy(() -> submissions.download(subId))
          .isInstanceOf(AccessDeniedException.class);

      // sv4 là leader Nhóm 2 -> download thành công
      asUser("sv4@demo.local");
      var download = submissions.download(subId);
      assertThat(download).isNotNull();
      assertThat(download.name()).isEqualTo("report.pdf");
    } finally {
      Files.deleteIfExists(filePath);
    }
  }
}
