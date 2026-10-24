package vn.edu.detaihub;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.service.*;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:detaihub;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "app.storage=target/test-uploads",
      "app.mail-dir=target/test-mail",
      "app.mail-worker=false"
    })
@AutoConfigureMockMvc
@Transactional
class ApplicationTest {
  @Autowired MockMvc mvc;
  @Autowired DomainRepository db;
  @Autowired UserDetailsService users;
  @Autowired QueryService query;
  @Autowired GroupService groups;
  @Autowired AssessmentService assessment;
  @Autowired SubmissionService submissions;
  @Autowired TopicService topics;
  @Autowired AccountService accounts;

  void as(String email) {
    var u = users.loadUserByUsername(email + "@demo.local");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  static Stream<Arguments> pages() {
    var rows = new ArrayList<Arguments>();
    for (String user : List.of("admin", "khoa", "tbm", "gv1", "gv2", "sv1", "sv4"))
      for (String path :
          List.of(
              "/",
              "/topics",
              "/cycles",
              "/groups",
              "/registrations",
              "/councils",
              "/evaluations",
              "/results",
              "/announcements",
              "/departments",
              "/profile")) rows.add(Arguments.of(user, path));
    for (String user : List.of("admin", "khoa"))
      for (String path :
          List.of(
              "/accounts",
              "/mail",
              "/audit",
              "/forms/accounts",
              "/forms/departments",
              "/forms/announcements")) rows.add(Arguments.of(user, path));
    for (String path :
        List.of(
            "/forms/cycles",
            "/forms/topics",
            "/forms/councils",
            "/registrations/1",
            "/groups/2",
            "/topics/1")) rows.add(Arguments.of("khoa", path));
    rows.add(Arguments.of("sv1", "/forms/groups"));
    rows.add(Arguments.of("sv1", "/groups/1"));
    rows.add(Arguments.of("sv4", "/registrations/1"));
    rows.add(Arguments.of("gv2", "/evaluations/1"));
    return rows.stream();
  }

  @ParameterizedTest(name = "{0} GET {1}")
  @MethodSource("pages")
  void allPagesRender(String name, String path) throws Exception {
    mvc.perform(get(path).with(user(users.loadUserByUsername(name + "@demo.local"))))
        .andExpect(status().isOk());
  }

  @Test
  void updatedLoginAndSidebarShareBrandAndLogoIsPublic() throws Exception {
    var login = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
    assertThat(login.getResponse().getContentAsString())
        .contains("brand login-brand", "WEB ĐĂNG KÝ ĐỀ TÀI", "/images/logo.png", "name=\"_csrf\"")
        .doesNotContain(
            "login-story", "FACULTY WORKSPACE", "demo-access", "data-demo",
            "TRẢI NGHIỆM LOCAL", "@demo.local", "Demo@2026!");

    var script = mvc.perform(get("/js/app.js")).andExpect(status().isOk()).andReturn();
    assertThat(script.getResponse().getContentAsString())
        .contains("menu-toggle", "data-confirm")
        .doesNotContain("data-demo", "Demo@2026!");

    var logo =
        mvc.perform(get("/images/logo.png"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn();
    assertThat(logo.getResponse().getContentAsByteArray()).isNotEmpty();

    var dashboard =
        mvc.perform(get("/").with(user(users.loadUserByUsername("khoa@demo.local"))))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(dashboard.getResponse().getContentAsString())
        .contains("brand sidebar-brand", "WEB ĐĂNG KÝ ĐỀ TÀI", "/images/logo.png")
        .doesNotContain("FACULTY WORKSPACE");
  }

  @Test
  void authenticationAndCsrf() throws Exception {
    mvc.perform(get("/api/v1/topics")).andExpect(status().isUnauthorized());
    mvc.perform(get("/topics")).andExpect(status().is3xxRedirection());
    mvc.perform(
            post("/profile")
                .with(user(users.loadUserByUsername("sv1@demo.local")))
                .param("fullName", "Changed"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "sv1@demo.local")
                .param("password", "Demo@2026!"))
        .andExpect(redirectedUrl("/"));
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "sv1@demo.local")
                .param("password", "incorrect"))
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void denyCrossGroupAndAdministration() throws Exception {
    var student = user(users.loadUserByUsername("sv1@demo.local"));
    mvc.perform(get("/registrations/1").with(student)).andExpect(status().isForbidden());
    mvc.perform(get("/accounts").with(student)).andExpect(status().isForbidden());
    mvc.perform(post("/registrations/1/publish").with(student).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/forms/accounts").with(student)).andExpect(status().isForbidden());
  }

  @Test
  void fullAssessmentLifecycle() throws Exception {
    as("sv4");
    submissions.upload(
        1L,
        new MockMultipartFile(
            "file", "report.pdf", "application/pdf", "%PDF-1.4\nExample test report".getBytes()));
    assertThat(query.submissions(1L)).hasSize(1);
    as("khoa");
    assertThatThrownBy(() -> assessment.publish(1L)).isInstanceOf(RuntimeException.class);
    int i = 0;
    for (var e : db.all(Evaluation.class)) {
      var lecturer = db.get(Lecturer.class, e.getLecturerId());
      var email = db.get(UserAccount.class, lecturer.getUserId()).getEmail();
      as(email.substring(0, email.indexOf('@')));
      var f = new Forms.Grade();
      f.setScore(new BigDecimal(7 + i++));
      f.setComment("Đạt yêu cầu, có minh chứng và sản phẩm.");
      f.setSubmit(true);
      assessment.grade(e.getId(), f);
    }
    as("gv3");
    assessment.confirm(1L);
    as("khoa");
    assessment.publish(1L);
    assessment.publish(1L);
    assertThat(query.results()).hasSize(1);
    assertThat(query.results().getFirst().score()).isEqualTo("8.50");
    assertThat(db.all(EmailDelivery.class)).hasSize(3);
    as("sv4");
    assertThat(query.results()).hasSize(1);
    as("sv1");
    assertThat(query.results()).isEmpty();
    as("admin");
    assertThat(query.results()).isEmpty();
    as("tbm");
    assertThat(query.results()).isEmpty();
    assertThat(query.registration(1L).result()).isNull();
    as("khoa");
    assessment.reopen(1L, "Điều chỉnh điểm theo biên bản hội đồng.");
    assertThat(db.all(EmailDelivery.class)).allMatch(m -> m.getStatus().equals("CANCELLED"));
    as("sv4");
    assertThat(query.results()).isEmpty();
  }

  @Test
  void leaderCanRegisterAndFacultyApproves() {
    as("sv1");
    var f = new Forms.Registration();
    f.setGroupId(1L);
    f.setTopicId(1L);
    Long id = groups.register(f);
    assertThat(db.get(StudentGroup.class, 1L).getStatus()).isEqualTo("PENDING");
    as("khoa");
    groups.decision(id, "approve", "");
    assertThat(db.get(StudentGroup.class, 1L).getStatus()).isEqualTo("LOCKED");
  }

  @Test
  void duplicateGroupRejected() {
    as("sv1");
    var f = new Forms.Group();
    f.setName("Another group");
    f.setCycleId(2L);
    assertThatThrownBy(() -> groups.create(f)).hasMessageContaining("đã có nhóm");
  }

  @Test
  void supervisorCannotReviewOwnTopic() {
    as("khoa");
    var supervisor =
        db.all(TopicSupervisor.class).stream()
            .filter(s -> s.getTopicId().equals(db.get(TopicRegistration.class, 1L).getTopicId()))
            .findFirst()
            .orElseThrow();
    assertThatThrownBy(() -> assessment.reviewers(1L, List.of(supervisor.getLecturerId())))
        .hasMessageContaining("GVHD");
  }

  @Test
  void nonLeaderCannotUpload() {
    as("sv5");
    assertThatThrownBy(
            () ->
                submissions.upload(
                    1L,
                    new MockMultipartFile(
                        "file", "report.pdf", "application/pdf", "%PDF-1.4".getBytes())))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }

  @Test
  void badFileSignatureRejected() {
    as("sv4");
    assertThatThrownBy(
            () ->
                submissions.upload(
                    1L,
                    new MockMultipartFile(
                        "file",
                        "report.pdf",
                        "application/pdf",
                        "<script>bad</script>".getBytes())))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void invalidFormRendersErrors() throws Exception {
    mvc.perform(
            post("/forms/topics")
                .with(user(users.loadUserByUsername("gv1@demo.local")))
                .with(csrf())
                .param("title", ""))
        .andExpect(status().isOk())
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "form", "title", "description", "cycleId", "departmentId"));
  }

  @Test
  void passwordResetInvalidatesExistingSession() throws Exception {
    var login =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", "sv1@demo.local")
                    .param("password", "Demo@2026!"))
            .andReturn();
    var session =
        (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
    var u =
        db.all(UserAccount.class).stream()
            .filter(x -> x.getEmail().equals("sv1@demo.local"))
            .findFirst()
            .orElseThrow();
    u.setSecurityVersion(u.getSecurityVersion() + 1);
    db.flush();
    mvc.perform(get("/topics").session(session)).andExpect(status().is3xxRedirection());
  }

  @Test
  void secretarySeesSubmissionStatusButNotOthersScores() {
    as("gv4");
    assertThat(query.councilProgress(1L)).hasSize(3);
    assertThat(query.evaluations(1L)).hasSize(1).allMatch(e -> e.mine());
  }

  @Test
  void readinessExplainsMissingConfirmation() {
    as("khoa");
    assertThat(assessment.readiness())
        .hasSize(1)
        .allMatch(r -> !r.ready() && r.reason().contains("xác nhận"));
  }

  @Test
  void malformedGradesAreRejected() throws Exception {
    mvc.perform(
            post("/evaluations/1")
                .with(user(users.loadUserByUsername("gv2@demo.local")))
                .with(csrf())
                .param("score", "11")
                .param("comment", "Test"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "score"));
  }

  @Test
  void departmentCrudAndDuplicateCode() {
    as("admin");
    var f = new Forms.Department();
    f.setCode("TEST");
    f.setName("Test department");
    accounts.department(f);
    var id =
        db.all(Department.class).stream()
            .filter(d -> d.getCode().equals("TEST"))
            .findFirst()
            .orElseThrow()
            .getId();
    f.setId(id);
    f.setName("Updated");
    accounts.department(f);
    assertThat(db.get(Department.class, id).getName()).isEqualTo("Updated");
    accounts.deleteDepartment(id);
    assertThat(db.all(Department.class)).noneMatch(d -> d.getId().equals(id));
  }

  @Test
  void invitationAcceptLeaveAndTransfer() {
    as("sv1");
    groups.invite(1L, "SV007");
    as("sv7");
    groups.respond(1L, true);
    assertThat(groups.member(1L)).isTrue();
    groups.leave(1L);
    assertThat(groups.member(1L)).isFalse();
    as("sv1");
    groups.transfer(1L, 2L);
    assertThat(groups.leader(db.get(StudentGroup.class, 1L))).isFalse();
    as("sv2");
    assertThat(groups.leader(db.get(StudentGroup.class, 1L))).isTrue();
  }

  @Test
  void proposalCanBeCreatedEditedAndDeleted() {
    as("gv1");
    var f = new Forms.Topic();
    f.setTitle("Test proposal");
    f.setDescription("Detailed proposal");
    f.setCycleId(1L);
    f.setDepartmentId(1L);
    var id = topics.save(f);
    f.setId(id);
    f.setTitle("Updated proposal");
    topics.save(f);
    assertThat(query.topic(id).title()).isEqualTo("Updated proposal");
    topics.delete(id);
    assertThat(db.all(Topic.class)).noneMatch(t -> t.getId().equals(id));
  }
}
