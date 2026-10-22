package vn.edu.detaihub;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.model.TopicRegistration;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.service.GroupService;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:registrationedge;MODE=MySQL;DB_CLOSE_DELAY=-1}",
      "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}",
      "spring.datasource.username=${TEST_DB_USER:sa}",
      "spring.datasource.password=${TEST_DB_PASSWORD:}",
      "app.mail-worker=false"
    })
class RegistrationEdgeCasesTest {
  @Autowired GroupService groups;
  @Autowired UserDetailsService users;
  @Autowired TopicRegistrationRepository registrationRepo;
  @Autowired TransactionTemplate tx;

  private void asUser(String email) {
    var u = users.loadUserByUsername(email);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
  }

  @Test
  void cannotRejectWithoutValidReason() {
    // Tạo 1 đơn PENDING cho nhóm 1 để test
    Long regId = tx.execute(status -> {
      var r = new TopicRegistration();
      r.setGroupId(1L);
      r.setTopicId(1L);
      r.setStatus("PENDING");
      registrationRepo.save(r);
      return r.getId();
    });

    asUser("khoa@demo.local");
    assertThatThrownBy(() -> groups.decision(regId, "reject", " "))
        .isInstanceOf(RuleException.class)
        .hasMessageContaining("lý do từ chối");

    assertThatThrownBy(() -> groups.decision(regId, "reject", "k ok"))
        .isInstanceOf(RuleException.class)
        .hasMessageContaining("tối thiểu 5 ký tự");
  }

  @Test
  void nonLeaderCannotRegisterTopic() {
    asUser("sv2@demo.local"); // sv2 là thành viên nhóm 1, không phải trưởng nhóm
    var f = new Forms.Registration();
    f.setGroupId(1L);
    f.setTopicId(1L);

    assertThatThrownBy(() -> groups.register(f))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }
}
