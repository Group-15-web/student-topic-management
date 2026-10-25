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
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.service.GroupService;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:groupedge;MODE=MySQL;DB_CLOSE_DELAY=-1}",
      "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}",
      "spring.datasource.username=${TEST_DB_USER:sa}",
      "spring.datasource.password=${TEST_DB_PASSWORD:}",
      "app.mail-worker=false"
    })
class GroupEdgeCasesTest {
  @Autowired TransactionTemplate tx;
  @Autowired StudentRepository studentRepo;
  @Autowired StudentGroupRepository groupRepo;
  @Autowired GroupMemberRepository memberRepo;
  @Autowired UserDetailsService users;
  @Autowired GroupService groups;

  private void asUser(String email) {
    var u = users.loadUserByUsername(email);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
  }

  @Test
  void cannotInviteSelf() {
    asUser("sv1@demo.local");
    // sv1 có mã là SV001 trong DemoData
    assertThatThrownBy(() -> groups.invite(1L, "SV001"))
        .isInstanceOf(RuleException.class)
        .hasMessageContaining("Không thể tự mời chính mình");
  }

  @Test
  void cannotInviteAlreadyActiveMember() {
    asUser("sv1@demo.local");
    // sv2 có mã là SV002 và đã là thành viên của Nhóm 1 trong DemoData
    assertThatThrownBy(() -> groups.invite(1L, "SV002"))
        .isInstanceOf(RuleException.class)
        .hasMessageContaining("đã là thành viên");
  }

  @Test
  void autoCancelOtherInvitesWhenAccepted() {
    var sv7 = studentRepo.findByCode("SV007").orElseThrow();

    // Thiết lập 2 lời mời cho sv7
    tx.executeWithoutResult(status -> {
      var m2 = new GroupMember();
      m2.setGroupId(2L);
      m2.setStudentId(sv7.getId());
      m2.setStatus("INVITED");
      memberRepo.save(m2);

      var m1 = new GroupMember();
      m1.setGroupId(1L);
      m1.setStudentId(sv7.getId());
      m1.setStatus("INVITED");
      memberRepo.save(m1);
    });

    // sv7 chấp nhận vào nhóm 1
    asUser("sv7@demo.local");
    groups.respond(1L, true);

    // Kiểm tra: Lời mời ở nhóm 2 phải tự động chuyển sang CANCELLED
    var inviteGroup2 = memberRepo.findByGroupIdAndStudentId(2L, sv7.getId()).orElseThrow();
    assertThat(inviteGroup2.getStatus()).isEqualTo("CANCELLED");
  }

  @Test
  void cannotTransferLeaderToSelf() {
    asUser("sv1@demo.local");
    var sv1 = studentRepo.findByCode("SV001").orElseThrow();
    assertThatThrownBy(() -> groups.transfer(1L, sv1.getId()))
        .isInstanceOf(RuleException.class)
        .hasMessageContaining("Sinh viên này hiện đã là trưởng nhóm");
  }
}