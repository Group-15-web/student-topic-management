package vn.edu.detaihub.controller;

import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.dto.*;
import vn.edu.detaihub.security.ActorService;
import vn.edu.detaihub.service.*;

@Controller
@RequiredArgsConstructor
public class PageController {
  @PostMapping("/results/publish")
  public String publishBatch(
      @RequestParam(defaultValue = "") List<Long> registrationIds, RedirectAttributes ra) {
    assessments.publishBatch(registrationIds);
    return done("/results", ra);
  }

  private final QueryService query;
  private final ActorService actor;
  private final TopicService topics;
  private final GroupService groups;
  private final AssessmentService assessments;
  private final SubmissionService submissions;
  private final AnnouncementService announcements;
  private final AccountService accounts;
  private final EmailService mail;

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/")
  public String home(Model m) {
    m.addAttribute("topics", query.topics().stream().limit(4).toList());
    m.addAttribute("cycles", query.cycles().stream().limit(3).toList());
    m.addAttribute(
        "announcements",
        query.announcements().stream()
            .filter(a -> a.status().equals("PUBLISHED"))
            .limit(3)
            .toList());
    m.addAttribute("topicCount", query.topics().size());
    m.addAttribute("registrationCount", query.registrations().size());
    m.addAttribute("cycleCount", query.cycles().size());
    m.addAttribute("resultCount", query.results().size());
    return "dashboard";
  }

  @GetMapping("/topics")
  public String topicList(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(required = false) Long cycleId,
      @RequestParam(required = false) Long departmentId,
      @RequestParam(defaultValue = "") String cycleType,
      @RequestParam(defaultValue = "") String supervisor,
      @RequestParam(defaultValue = "newest") String sort,
      Model m) {
    var cycleRows=query.cycles();
    Comparator<Views.Topic> order=sort.equals("title")?Comparator.comparing(Views.Topic::title):Comparator.comparing(Views.Topic::id).reversed();
    var rows =
        query.topics().stream()
            .filter(t -> cycleId == null || t.cycleId().equals(cycleId))
            .filter(t -> departmentId == null || t.departmentId().equals(departmentId))
            .filter(t -> cycleType.isBlank() || cycleRows.stream().anyMatch(c->c.id().equals(t.cycleId())&&c.type().equals(cycleType)))
            .filter(t -> supervisor.isBlank() || t.supervisors().toLowerCase(Locale.ROOT).contains(supervisor.trim().toLowerCase(Locale.ROOT)))
            .sorted(order)
            .toList();
    m.addAttribute("data", QueryService.page(rows, q, page));
    m.addAttribute("cycles", query.cycles());
    m.addAttribute("departments", query.departments());
    m.addAttribute("cycleId", cycleId);
    m.addAttribute("departmentId", departmentId);
    m.addAttribute("cycleType",cycleType);
    m.addAttribute("supervisor",supervisor);
    m.addAttribute("sort",sort);
    return "topics";
  }

  @GetMapping("/topics/{id}")
  public String topic(@PathVariable Long id, Model m) {
    m.addAttribute("topic", query.topic(id));
    m.addAttribute("groups", query.groups().stream().filter(Views.Group::leader).toList());
    if (actor.faculty()) m.addAttribute("lecturers", query.lecturers());
    return "topic";
  }

  @PostMapping("/topics/{id}/{action:submit|approve|publish|return|reject}")
  public String topicAction(
      @PathVariable Long id,
      @PathVariable String action,
      @RequestParam(defaultValue = "") String note,
      RedirectAttributes ra) {
    topics.transition(id, action, note);
    return done("/topics/" + id, ra);
  }

  @PostMapping("/topics/{id}/supervisors")
  public String supervisors(
      @PathVariable Long id,
      @RequestParam(defaultValue = "") List<Long> supervisorIds,
      RedirectAttributes ra) {
    topics.supervisors(id, supervisorIds);
    return done("/topics/" + id, ra);
  }

  @PostMapping("/topics/{id}/review")
  public String review(@PathVariable Long id, @RequestParam String note, RedirectAttributes ra) {
    topics.review(id, note);
    return done("/topics/" + id, ra);
  }

  @PostMapping("/topics/{id}/delete")
  public String deleteTopic(@PathVariable Long id, RedirectAttributes ra) {
    topics.delete(id);
    return done("/topics", ra);
  }

  @GetMapping("/cycles")
  public String cycles(Model m) {
    m.addAttribute("cycles", query.cycles());
    return "cycles";
  }


  @PostMapping("/cycles/{id}/import-whitelist")
  public String importWhitelist(
      @PathVariable Long id,
      @RequestParam(name = "file", required = false) MultipartFile file,
      @RequestParam(name = "manualCodes", required = false, defaultValue = "") String manualCodes,
      RedirectAttributes ra) {
    int count = topics.importEligibleStudents(id, file, manualCodes);
    ra.addFlashAttribute("notice", "Đã nạp thành công " + count + " sinh viên đủ điều kiện.");
    return "redirect:/cycles";
  }

  @PostMapping("/cycles/{id}/delete")
  public String deleteCycle(@PathVariable Long id, RedirectAttributes ra) {
    topics.deleteCycle(id);
    return done("/cycles", ra);
  }

  @GetMapping("/groups")
  public String groupList(Model m) {
    m.addAttribute("groups", query.groups());
    return "groups";
  }

  @GetMapping("/groups/{id}")
  public String group(@PathVariable Long id, Model m) {
    m.addAttribute("group", query.group(id));
    m.addAttribute(
        "registrations",
        query.registrations().stream().filter(r -> r.groupId().equals(id)).toList());
    return "group";
  }

  @PostMapping("/groups/{id}/invite")
  public String invite(
      @PathVariable Long id, @RequestParam String studentCode, RedirectAttributes ra) {
    groups.invite(id, studentCode);
    return done("/groups/" + id, ra);
  }

  @PostMapping("/groups/{id}/respond")
  public String respond(
      @PathVariable Long id, @RequestParam boolean accept, RedirectAttributes ra) {
    groups.respond(id, accept);
    return done(accept ? "/groups/" + id : "/groups", ra);
  }

  @PostMapping("/groups/{id}/leave")
  public String leave(@PathVariable Long id, RedirectAttributes ra) {
    groups.leave(id);
    return done("/groups", ra);
  }

  @PostMapping("/groups/{id}/leader")
  public String transfer(
      @PathVariable Long id,
      @RequestParam Long studentId,
      @RequestParam(name = "reason", required = false, defaultValue = "") String reason,
      RedirectAttributes ra) {
    groups.transfer(id, studentId, reason);
    return done("/groups/" + id, ra);
  }

  @PostMapping("/registrations")
  public String register(
      @Valid @ModelAttribute Forms.Registration f, BindingResult br, RedirectAttributes ra) {
    if (br.hasErrors())
      throw new vn.edu.detaihub.exception.RuleException("Chọn nhóm và đề tài hợp lệ.", 400);
    return done("/registrations/" + groups.register(f), ra);
  }

  @GetMapping("/registrations")
  public String registrations(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page,
      Model m) {
    m.addAttribute("data", QueryService.page(query.registrations(), q, page));
    return "registrations";
  }

  @GetMapping("/registrations/{id}")
  public String registration(@PathVariable Long id, Model m) {
    m.addAttribute("registration", query.registration(id));
    m.addAttribute("submissions", query.submissions(id));
    m.addAttribute("progress", query.councilProgress(id));
    m.addAttribute("evaluations", query.evaluations(id));
    if (actor.faculty()) {
      m.addAttribute("lecturers", query.lecturers());
      m.addAttribute("councils", query.councils());
    }
    return "registration";
  }

  @PostMapping("/registrations/{id}/{action:approve|reject|withdraw}")
  public String decide(
      @PathVariable Long id,
      @PathVariable String action,
      @RequestParam(defaultValue = "") String note,
      RedirectAttributes ra) {
    groups.decision(id, action, note);
    return done("/registrations/" + id, ra);
  }

  @PostMapping("/registrations/{id}/cancel")
  public String cancelRegistration(
      @PathVariable Long id,
      @RequestParam(defaultValue = "") String reason,
      RedirectAttributes ra) {
    groups.cancelApprovedRegistration(id, reason);
    return done("/registrations/" + id, ra);
  }

  @PostMapping("/registrations/{id}/submissions")
  public String upload(
      @PathVariable Long id, @RequestParam MultipartFile file, RedirectAttributes ra)
      throws Exception {
    submissions.upload(id, file);
    return done("/registrations/" + id, ra);
  }

  @GetMapping("/submissions/{id}/download")
  public ResponseEntity<FileSystemResource> download(@PathVariable Long id) {
    var d = submissions.download(id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(d.name(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .contentType(MediaType.parseMediaType(d.contentType()))
        .body(new FileSystemResource(d.path()));
  }

  @PostMapping("/registrations/{id}/reviewers")
  public String reviewers(
      @PathVariable Long id,
      @RequestParam(defaultValue = "") List<Long> lecturerIds,
      RedirectAttributes ra) {
    assessments.reviewers(id, lecturerIds);
    return done("/registrations/" + id, ra);
  }

  @PostMapping("/registrations/{id}/council")
  public String assign(@PathVariable Long id, @RequestParam Long councilId, RedirectAttributes ra) {
    assessments.assignCouncil(councilId, id);
    return done("/registrations/" + id, ra);
  }

  @GetMapping("/councils")
  public String councils(Model m) {
    m.addAttribute("councils", query.councils());
    return "councils";
  }

  @PostMapping("/councils/{id}/delete")
  public String deleteCouncil(@PathVariable Long id, RedirectAttributes ra) {
    assessments.removeCouncil(id);
    return done("/councils", ra);
  }

  @GetMapping("/evaluations")
  public String grades(Model m) {
    m.addAttribute("evaluations", query.evaluations(null));
    return "evaluations";
  }

  @GetMapping("/evaluations/{id}")
  public String grade(@PathVariable Long id, Model m) {
    var e = query.evaluation(id);
    actor.allow(e.mine());
    var f = new Forms.Grade();
    if (!e.score().equals("—")) f.setScore(new java.math.BigDecimal(e.score()));
    f.setComment(e.comment());
    m.addAttribute("evaluation", e);
    m.addAttribute("form", f);
    return "grade";
  }

  @PostMapping("/evaluations/{id}")
  public String gradeSave(
      @PathVariable Long id,
      @Valid @ModelAttribute("form") Forms.Grade f,
      BindingResult br,
      Model m,
      RedirectAttributes ra) {
    m.addAttribute("evaluation", query.evaluation(id));
    if (br.hasErrors()) return "grade";
    try {
      assessments.grade(id, f);
    } catch (vn.edu.detaihub.exception.RuleException e) {
      m.addAttribute("error", e.getMessage());
      return "grade";
    }
    return done("/evaluations", ra);
  }

  @PostMapping("/registrations/{id}/confirm")
  public String confirm(@PathVariable Long id, RedirectAttributes ra) {
    assessments.confirm(id);
    return done("/registrations/" + id, ra);
  }

  @PostMapping("/registrations/{id}/publish")
  public String publish(@PathVariable Long id, RedirectAttributes ra) {
    assessments.publish(id);
    return done("/registrations/" + id, ra);
  }

  @PostMapping("/registrations/{id}/reopen")
  public String reopen(@PathVariable Long id, @RequestParam String reason, RedirectAttributes ra) {
    assessments.reopen(id, reason);
    return done("/registrations/" + id, ra);
  }

  @PostMapping("/registrations/{id}/complete")
  public String complete(@PathVariable Long id, RedirectAttributes ra) {
    assessments.complete(id);
    return done("/registrations/" + id, ra);
  }

  @GetMapping("/results")
  public String results(@RequestParam(required = false) Long cycleId, Model m) {
    if (actor.faculty())
      m.addAttribute(
          "readiness",
          assessments.readiness().stream()
              .filter(r -> cycleId == null || r.cycleId().equals(cycleId))
              .toList());
    var results = query.results();
    m.addAttribute(
        "results",
        results.stream().filter(r -> cycleId == null || r.cycleId().equals(cycleId)).toList());
    m.addAttribute(
        "resultCycles",
        actor.faculty()
            ? query.cycles().stream().map(c -> new Views.Option(c.id(), c.name())).toList()
            : results.stream()
                .map(r -> new Views.Option(r.cycleId(), r.cycle()))
                .distinct()
                .toList());
    m.addAttribute("cycleId", cycleId);
    return "results";
  }

  @GetMapping("/announcements")
  public String announcements(Model m) {
    m.addAttribute("announcements", query.announcements());
    return "announcements";
  }

  @PostMapping("/announcements/{id}/delete")
  public String deleteAnnouncement(@PathVariable Long id, RedirectAttributes ra) {
    announcements.delete(id);
    return done("/announcements", ra);
  }

  @GetMapping("/accounts")
  public String accounts(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page,
      Model m) {
    m.addAttribute("data", QueryService.page(query.accounts(), q, page));
    return "accounts";
  }

  @PostMapping("/accounts/{id}/toggle")
  public String toggle(@PathVariable Long id, RedirectAttributes ra) {
    accounts.toggle(id);
    return done("/accounts", ra);
  }

  @PostMapping("/accounts/{id}/delete")
  public String deleteAccount(@PathVariable Long id, RedirectAttributes ra) {
    accounts.delete(id);
    return done("/accounts", ra);
  }

  @PostMapping("/accounts/{id}/roles")
  public String roles(
      @PathVariable Long id,
      @RequestParam String role,
      @RequestParam boolean grant,
      RedirectAttributes ra) {
    accounts.grant(id, role, grant);
    return done("/accounts", ra);
  }

  @GetMapping("/departments")
  public String departments(Model m) {
    m.addAttribute("departments", query.departmentRows());
    return "departments";
  }

  @PostMapping("/departments/{id}/delete")
  public String deleteDepartment(@PathVariable Long id, RedirectAttributes ra) {
    accounts.deleteDepartment(id);
    return done("/departments", ra);
  }

  @GetMapping("/mail")
  public String mails(Model m) {
    m.addAttribute("mails", query.mails());
    return "mail-list";
  }

  @PostMapping("/mail/{id}/retry")
  public String retry(@PathVariable Long id, RedirectAttributes ra) {
    mail.retry(id);
    return done("/mail", ra);
  }

  @GetMapping("/mail/{id}/preview")
  public ResponseEntity<FileSystemResource> preview(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=notification-" + id + ".eml")
        .contentType(MediaType.parseMediaType("message/rfc822"))
        .body(new FileSystemResource(mail.preview(id)));
  }

  @GetMapping("/audit")
  public String audit(Model m) {
    m.addAttribute("audits", query.audits());
    return "audit";
  }

  @GetMapping("/profile")
  public String profile(Model m) {
    return "profile";
  }

  @PostMapping("/profile")
  public String profileSave(
      @Valid @ModelAttribute Forms.Profile f, BindingResult br, RedirectAttributes ra) {
    if (br.hasErrors())
      throw new vn.edu.detaihub.exception.RuleException("Họ tên không hợp lệ.", 400);
    accounts.profile(f);
    return done("/profile", ra);
  }

  @PostMapping("/profile/password")
  public String password(
      @Valid @ModelAttribute Forms.Password f, BindingResult br, RedirectAttributes ra) {
    if (br.hasErrors())
      throw new vn.edu.detaihub.exception.RuleException("Mật khẩu mới phải có 10–72 ký tự.", 400);
    accounts.password(f);
    return done("/profile", ra);
  }

  private String done(String path, RedirectAttributes ra) {
    ra.addFlashAttribute("success", "Đã lưu thay đổi thành công.");
    return "redirect:" + path;
  }
}
