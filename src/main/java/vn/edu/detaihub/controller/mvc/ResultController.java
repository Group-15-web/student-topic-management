package vn.edu.detaihub.controller.mvc;

import static vn.edu.detaihub.controller.support.FormViewSupport.done;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.dto.Views;
import vn.edu.detaihub.security.ActorService;
import vn.edu.detaihub.service.AssessmentService;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class ResultController {
  private final QueryService query;
  private final ActorService actor;
  private final AssessmentService assessments;

  @PostMapping("/results/publish")
  public String publishBatch(
      @RequestParam(defaultValue = "") List<Long> registrationIds, RedirectAttributes ra) {
    assessments.publishBatch(registrationIds);
    return done("/results", ra);
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
}
