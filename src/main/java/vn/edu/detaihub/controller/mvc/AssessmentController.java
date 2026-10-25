package vn.edu.detaihub.controller.mvc;

import static vn.edu.detaihub.controller.support.FormViewSupport.done;
import static vn.edu.detaihub.controller.support.FormViewSupport.field;
import static vn.edu.detaihub.controller.support.FormViewSupport.select;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
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
import vn.edu.detaihub.controller.support.FormViewSupport;
import vn.edu.detaihub.controller.support.FormViewSupport.Field;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.dto.Views;
import vn.edu.detaihub.security.ActorService;
import vn.edu.detaihub.service.AssessmentService;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class AssessmentController {
  private final QueryService query;
  private final ActorService actor;
  private final AssessmentService assessments;
  private final FormViewSupport formView;

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

  @GetMapping("/forms/councils")
  public String form(@RequestParam(required = false) Long id, Model model) {
    actor.need("FACULTY_MANAGER");
    return formView.show("councils", "Thành lập hội đồng", assessments.councilForm(id), model, this::fields);
  }

  @PostMapping("/forms/councils")
  public String save(
      @Valid @ModelAttribute("form") Forms.Council form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    return formView.save(
        "councils", "Thành lập hội đồng", form, binding, model, redirect,
        () -> assessments.council(form), this::fields);
  }

  private List<Field> fields(List<Views.Option> cycleOptions) {
    var fields = new ArrayList<Field>();
    fields.add(field("name", "Tên hội đồng", "text"));
    fields.add(select("cycleId", "Đợt đăng ký", cycleOptions, false));
    fields.add(select("presidentId", "Chủ tịch", query.lecturers(), false));
    fields.add(select("secretaryId", "Thư ký", query.lecturers(), false));
    fields.add(select("memberIds", "Thành viên khác (1–3 người)", query.lecturers(), true));
    fields.add(field("reportAt", "Thời gian báo cáo", "datetime-local"));
    fields.add(field("location", "Phòng / địa điểm", "text"));
    return fields;
  }
}
