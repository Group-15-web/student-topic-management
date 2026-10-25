package vn.edu.detaihub.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.detaihub.controller.mvc.*;

class MvcControllerStructureTest {
  @Test
  void splitControllersPreserveAllOriginalRoutesAndKnownFormRoutes() {
    var controllers = List.of(
        HomeController.class,
        TopicController.class,
        RegistrationCycleController.class,
        GroupController.class,
        TopicRegistrationController.class,
        SubmissionController.class,
        AssessmentController.class,
        ResultController.class,
        AnnouncementController.class,
        AccountController.class,
        DepartmentController.class,
        MailController.class,
        AuditController.class,
        ProfileController.class,
        FormFallbackController.class);
    var routes = new ArrayList<String>();
    for (Class<?> controller : controllers) {
      assertThat(controller.isAnnotationPresent(Controller.class)).isTrue();
      for (var method : controller.getDeclaredMethods()) {
        var mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null) continue;
        for (var verb : mapping.method()) {
          for (var path : mapping.value()) routes.add(verb.name() + " " + path);
        }
      }
    }
    assertThat(routes).doesNotHaveDuplicates();
    assertThat(routes).containsExactlyInAnyOrderElementsOf(List.of(
        "GET /",
        "GET /accounts",
        "GET /announcements",
        "GET /audit",
        "GET /councils",
        "GET /cycles",
        "GET /departments",
        "GET /evaluations",
        "GET /evaluations/{id}",
        "GET /forms/accounts",
        "GET /forms/announcements",
        "GET /forms/councils",
        "GET /forms/cycles",
        "GET /forms/departments",
        "GET /forms/groups",
        "GET /forms/topics",
        "GET /forms/{type}",
        "GET /groups",
        "GET /groups/{id}",
        "GET /login",
        "GET /mail",
        "GET /mail/{id}/preview",
        "GET /profile",
        "GET /registrations",
        "GET /registrations/{id}",
        "GET /results",
        "GET /submissions/{id}/download",
        "GET /topics",
        "GET /topics/{id}",
        "POST /accounts/{id}/delete",
        "POST /accounts/{id}/roles",
        "POST /accounts/{id}/toggle",
        "POST /announcements/{id}/delete",
        "POST /councils/{id}/delete",
        "POST /cycles/{id}/delete",
        "POST /cycles/{id}/import-whitelist",
        "POST /departments/{id}/delete",
        "POST /evaluations/{id}",
        "POST /forms/accounts",
        "POST /forms/announcements",
        "POST /forms/councils",
        "POST /forms/cycles",
        "POST /forms/departments",
        "POST /forms/groups",
        "POST /forms/topics",
        "POST /groups/{id}/invite",
        "POST /groups/{id}/leader",
        "POST /groups/{id}/leave",
        "POST /groups/{id}/respond",
        "POST /mail/{id}/retry",
        "POST /profile",
        "POST /profile/password",
        "POST /registrations",
        "POST /registrations/{id}/cancel",
        "POST /registrations/{id}/complete",
        "POST /registrations/{id}/confirm",
        "POST /registrations/{id}/council",
        "POST /registrations/{id}/publish",
        "POST /registrations/{id}/reopen",
        "POST /registrations/{id}/reviewers",
        "POST /registrations/{id}/submissions",
        "POST /registrations/{id}/{action:approve|reject|withdraw}",
        "POST /results/publish",
        "POST /topics/{id}/delete",
        "POST /topics/{id}/review",
        "POST /topics/{id}/supervisors",
        "POST /topics/{id}/{action:submit|approve|publish|return|reject}"));
  }
}
