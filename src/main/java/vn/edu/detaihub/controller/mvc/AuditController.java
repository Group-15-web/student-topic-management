package vn.edu.detaihub.controller.mvc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class AuditController {
  private final QueryService query;

  @GetMapping("/audit")
  public String audit(Model m) {
    m.addAttribute("audits", query.audits());
    return "audit";
  }
}
