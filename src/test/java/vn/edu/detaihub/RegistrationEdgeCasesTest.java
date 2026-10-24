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
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.service.TopicService;
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
  @Autowired StudentGroupRepository groupRepo;
  @Autowired StudentRepository studentRepo;
  @Autowired TopicService topicService;
  @Autowired CycleEligibleStudentRepository eligibleRepo;

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

  @Test
  void cancelApprovedRegistrationDisbandsGroupAndReleasesMembers() {
    Long regId = tx.execute(status -> {
      var g = groupRepo.findById(1L).orElseThrow();
      g.setStatus("LOCKED");
      groupRepo.save(g);

      var r = new TopicRegistration();
      r.setGroupId(1L);
      r.setTopicId(1L);
      r.setStatus("APPROVED");
      registrationRepo.save(r);
      return r.getId();
    });

    asUser("khoa@demo.local");
    groups.cancelApprovedRegistration(regId, "Sinh viên bỏ dở khóa luận giữa kỳ");

    tx.executeWithoutResult(status -> {
      var r = registrationRepo.findById(regId).orElseThrow();
      assertThat(r.getStatus()).isEqualTo("CANCELLED");

      var g = groupRepo.findById(1L).orElseThrow();
      assertThat(g.getStatus()).isEqualTo("DISBANDED");

      var sv1 = studentRepo.findByCode("SV001").orElseThrow();
      assertThat(sv1.getActiveGroupId()).isNull();
    });
  }

  @Test
  void bomHandlingInWhitelistImport() {
    asUser("khoa@demo.local");
    // Giả lập chuỗi copy-paste có ký tự UTF-8 BOM (﻿)
    String bomContent = "\uFEFFSV008\nSV009";
    int count = topicService.importEligibleStudents(1L, null, bomContent);
    assertThat(count).isGreaterThanOrEqualTo(1);

    var sv8 = studentRepo.findByCode("SV008").orElseThrow();
    assertThat(eligibleRepo.existsByCycleIdAndStudentId(1L, sv8.getId())).isTrue();
  }
}