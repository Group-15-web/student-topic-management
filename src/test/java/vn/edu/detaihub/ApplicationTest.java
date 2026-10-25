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
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.SessionVersionFilter;
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
  @Autowired ApplicationContext applicationContext;
  @Autowired SecurityFilterChain securityChain;

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
        .andExpect(status().isOk())
        .andExpect(model().attribute("path", path))
        .andExpect(model().attributeExists("me", "mailMode", "demoSeed"));
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
    mvc.perform(get("/api/v1/topics"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().string("{\"error\":\"UNAUTHENTICATED\"}"));
    mvc.perform(get("/topics")).andExpect(redirectedUrl("/login"));
    mvc.perform(
            post("/login")
                .param("username", "sv1@demo.local")
                .param("password", "Demo@2026!"))
        .andExpect(status().isForbidden());
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
  void unknownFormTypeStillReturnsTheNotFoundErrorPage() throws Exception {
    mvc.perform(
            get("/forms/not-a-real-type")
                .with(user(users.loadUserByUsername("sv1@demo.local"))))
        .andExpect(status().isNotFound())
        .andExpect(view().name("error"))
        .andExpect(model().attribute("code", 404))
        .andExpect(model().attribute("message", "Không tìm thấy biểu mẫu."));
  }

  @Test
  void malformedFormIdStillReturnsTheBadRequestErrorPage() throws Exception {
    mvc.perform(
            get("/forms/topics")
                .param("id", "not-a-number")
                .with(user(users.loadUserByUsername("gv1@demo.local"))))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("error"))
        .andExpect(model().attribute("code", 400));
  }

  @Test
  void groupCreationFormContinuesToIgnoreAnIdParameter() throws Exception {
    var result =
        mvc.perform(
                get("/forms/groups")
                    .param("id", "999999")
                    .with(user(users.loadUserByUsername("sv1@demo.local"))))
            .andExpect(status().isOk())
            .andExpect(view().name("form"))
            .andExpect(model().attribute("type", "groups"))
            .andExpect(model().attribute("title", "Tạo nhóm thực hiện"))
            .andReturn();
    var model = result.getModelAndView().getModel();
    assertThat(model.get("form")).isInstanceOf(Forms.Group.class);
    var form = (Forms.Group) model.get("form");
    assertThat(form.getName()).isNull();
    assertThat(form.getCycleId()).isNull();
    assertThat((List<?>) model.get("fields"))
        .extracting("name")
        .containsExactly("name", "cycleId");
  }

  @Test
  void councilEditFormPreservesTheCouncilAndItsMemberAssignments() throws Exception {
    var council = db.all(ReviewCouncil.class).getFirst();
    var members =
        db.all(CouncilMember.class).stream()
            .filter(member -> member.getCouncilId().equals(council.getId()))
            .toList();
    var result =
        mvc.perform(
                get("/forms/councils")
                    .param("id", council.getId().toString())
                    .with(user(users.loadUserByUsername("khoa@demo.local"))))
            .andExpect(status().isOk())
            .andExpect(view().name("form"))
            .andExpect(model().attribute("type", "councils"))
            .andExpect(model().attribute("title", "Thành lập hội đồng"))
            .andReturn();
    var model = result.getModelAndView().getModel();
    assertThat(model.get("form")).isInstanceOf(Forms.Council.class);
    var form = (Forms.Council) model.get("form");
    assertThat(form.getId()).isEqualTo(council.getId());
    assertThat(form.getName()).isEqualTo(council.getName());
    assertThat(form.getCycleId()).isEqualTo(council.getCycleId());
    assertThat(form.getReportAt()).isEqualTo(council.getReportAt());
    assertThat(form.getLocation()).isEqualTo(council.getLocation());
    assertThat(form.getPresidentId())
        .isEqualTo(
            members.stream()
                .filter(member -> member.getMemberRole().equals("PRESIDENT"))
                .findFirst()
                .orElseThrow()
                .getLecturerId());
    assertThat(form.getSecretaryId())
        .isEqualTo(
            members.stream()
                .filter(member -> member.getMemberRole().equals("SECRETARY"))
                .findFirst()
                .orElseThrow()
                .getLecturerId());
    assertThat(form.getMemberIds())
        .containsExactlyInAnyOrderElementsOf(
            members.stream()
                .filter(member -> !Set.of("PRESIDENT", "SECRETARY").contains(member.getMemberRole()))
                .map(CouncilMember::getLecturerId)
                .toList());
    assertThat((List<?>) model.get("fields"))
        .extracting("name")
        .containsExactly(
            "name", "cycleId", "presidentId", "secretaryId", "memberIds", "reportAt", "location");
  }

  static Stream<Arguments> invalidFormCases() {
    return Stream.of(
        Arguments.of(
            "topics", "gv1", Forms.Topic.class, "title", "description", "Thông tin đề tài"),
        Arguments.of(
            "cycles", "khoa", Forms.Cycle.class, "name", "lecturerStart", "Thiết lập đợt đăng ký"),
        Arguments.of(
            "accounts", "admin", Forms.Account.class, "fullName", "email", "Tài khoản & hồ sơ"),
        Arguments.of(
            "departments", "admin", Forms.Department.class, "name", "code", "Thông tin bộ môn"),
        Arguments.of(
            "groups", "sv1", Forms.Group.class, "name", "cycleId", "Tạo nhóm thực hiện"),
        Arguments.of(
            "councils", "khoa", Forms.Council.class, "name", "cycleId", "Thành lập hội đồng"),
        Arguments.of(
            "announcements", "admin", Forms.Announcement.class, "title", "content", "Biên soạn thông báo"));
  }

  @ParameterizedTest(name = "Invalid POST /forms/{0} keeps binding and metadata")
  @MethodSource("invalidFormCases")
  void invalidFormsKeepTheirDtoFieldErrorsSubmittedValuesAndMetadata(
      String type,
      String name,
      Class<?> formType,
      String retainedField,
      String requiredField,
      String title)
      throws Exception {
    String retainedValue = "Dữ liệu đã nhập";
    var result =
        mvc.perform(
                post("/forms/" + type)
                    .with(user(users.loadUserByUsername(name + "@demo.local")))
                    .with(csrf())
                    .param(retainedField, retainedValue))
            .andExpect(status().isOk())
            .andExpect(view().name("form"))
            .andExpect(model().attributeHasFieldErrors("form", requiredField))
            .andExpect(model().attribute("type", type))
            .andExpect(model().attribute("title", title))
            .andExpect(model().attribute("path", "/forms/" + type))
            .andExpect(model().attributeExists("me", "mailMode", "demoSeed"))
            .andReturn();
    var model = result.getModelAndView().getModel();
    assertThat(model.get("form")).isInstanceOf(formType);
    assertThat(model.get("form")).extracting(retainedField).isEqualTo(retainedValue);
    assertThat((List<?>) model.get("fields"))
        .isNotEmpty()
        .extracting("name")
        .contains(retainedField, requiredField);
  }

  @Test
  void successfulDepartmentFormStillRedirectsWithTheSuccessFlash() throws Exception {
    mvc.perform(
            post("/forms/departments")
                .with(user(users.loadUserByUsername("admin@demo.local")))
                .with(csrf())
                .param("code", "MVC_SPLIT_TEST")
                .param("name", "Bộ môn kiểm thử MVC"))
        .andExpect(redirectedUrl("/departments"))
        .andExpect(flash().attribute("success", "Đã lưu dữ liệu thành công."));
    assertThat(db.all(Department.class))
        .anyMatch(
            department ->
                department.getCode().equals("MVC_SPLIT_TEST")
                    && department.getName().equals("Bộ môn kiểm thử MVC"));
  }

  @Test
  void duplicateDepartmentFormStillKeepsTheInputAndDisplaysTheRuleError() throws Exception {
    String code = db.all(Department.class).getFirst().getCode();
    String submittedName = "Tên bộ môn đã nhập";
    var result =
        mvc.perform(
                post("/forms/departments")
                    .with(user(users.loadUserByUsername("admin@demo.local")))
                    .with(csrf())
                    .param("code", code)
                    .param("name", submittedName))
            .andExpect(status().isOk())
            .andExpect(view().name("form"))
            .andExpect(model().attribute("type", "departments"))
            .andExpect(model().attribute("title", "Thông tin bộ môn"))
            .andExpect(model().attribute("error", "Mã bộ môn đã tồn tại."))
            .andReturn();
    var form = (Forms.Department) result.getModelAndView().getModel().get("form");
    assertThat(form.getId()).isNull();
    assertThat(form.getCode()).isEqualTo(code);
    assertThat(form.getName()).isEqualTo(submittedName);
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
    mvc.perform(get("/topics").session(session)).andExpect(redirectedUrl("/login"));
    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void sessionFilterIsRegisteredOnceAndOnlyInsideSpringSecurity() {
    assertThat(applicationContext.getBeansOfType(UserDetailsService.class)).hasSize(1);
    assertThat(applicationContext.getBeansOfType(SessionVersionFilter.class)).isEmpty();

    var filters = securityChain.getFilters();
    assertThat(filters.stream().filter(SessionVersionFilter.class::isInstance).count()).isEqualTo(1);
    var sessionFilter =
        filters.stream().filter(SessionVersionFilter.class::isInstance).findFirst().orElseThrow();
    var loginFilter =
        filters.stream()
            .filter(UsernamePasswordAuthenticationFilter.class::isInstance)
            .findFirst()
            .orElseThrow();
    assertThat(filters.indexOf(sessionFilter)).isGreaterThan(filters.indexOf(loginFilter));
  }

  @Test
  void successfulLoginInitializesSessionAndCachesAuthoritiesOnTheNextRequest() throws Exception {
    var session = loginAsStudent();
    var account = studentAccount();
    assertThat(session.getAttribute("securityVersion")).isEqualTo(account.getSecurityVersion());
    assertThat(session.getAttribute("loadedSecurityVersion")).isNull();

    mvc.perform(get("/topics").session(session)).andExpect(status().isOk());
    assertThat(session.getAttribute("loadedSecurityVersion"))
        .isEqualTo(account.getSecurityVersion());
    mvc.perform(get("/topics").session(session)).andExpect(status().isOk());
  }

  @Test
  void disabledAccountInvalidatesItsExistingBrowserSession() throws Exception {
    var session = loginAsStudent();
    studentAccount().setEnabled(false);
    db.flush();

    mvc.perform(get("/topics").session(session)).andExpect(redirectedUrl("/login"));
    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void revokedApiSessionReturnsJsonInsteadOfRedirecting() throws Exception {
    var session = loginAsStudent();
    var account = studentAccount();
    account.setSecurityVersion(account.getSecurityVersion() + 1);
    db.flush();

    mvc.perform(get("/api/v1/topics").session(session))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().string("{\"error\":\"UNAUTHENTICATED\"}"));
    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void logoutInvalidatesTheSessionAndClearsItsCookie() throws Exception {
    var session = loginAsStudent();

    mvc.perform(post("/logout").session(session).with(csrf()))
        .andExpect(redirectedUrl("/login?logout"))
        .andExpect(cookie().maxAge("JSESSIONID", 0));
    assertThat(session.isInvalid()).isTrue();
  }

  private MockHttpSession loginAsStudent() throws Exception {
    var login =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", "sv1@demo.local")
                    .param("password", "Demo@2026!"))
            .andExpect(redirectedUrl("/"))
            .andReturn();
    return (MockHttpSession) login.getRequest().getSession(false);
  }

  private UserAccount studentAccount() {
    return db.all(UserAccount.class).stream()
        .filter(account -> account.getEmail().equals("sv1@demo.local"))
        .findFirst()
        .orElseThrow();
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
