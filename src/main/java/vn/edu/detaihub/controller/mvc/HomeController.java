package vn.edu.detaihub.controller.mvc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class HomeController {
  private final QueryService query;

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
}
