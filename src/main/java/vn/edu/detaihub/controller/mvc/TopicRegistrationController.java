package vn.edu.detaihub.controller.mvc;

import static vn.edu.detaihub.controller.support.FormViewSupport.done;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.security.ActorService;
import vn.edu.detaihub.service.GroupService;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class TopicRegistrationController {
  private final QueryService query;
  private final ActorService actor;
  private final GroupService groups;

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
}
