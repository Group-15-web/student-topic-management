package vn.edu.detaihub.controller.mvc;

import static vn.edu.detaihub.controller.support.FormViewSupport.done;
import static vn.edu.detaihub.controller.support.FormViewSupport.field;
import static vn.edu.detaihub.controller.support.FormViewSupport.select;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
import vn.edu.detaihub.service.QueryService;
import vn.edu.detaihub.service.TopicService;

@Controller
@RequiredArgsConstructor
public class TopicController {
  private final QueryService query;
  private final ActorService actor;
  private final TopicService topics;
  private final FormViewSupport formView;

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
    var cycleRows = query.cycles();
    Comparator<Views.Topic> order =
        sort.equals("title")
            ? Comparator.comparing(Views.Topic::title)
            : Comparator.comparing(Views.Topic::id).reversed();
    var rows =
        query.topics().stream()
            .filter(t -> cycleId == null || t.cycleId().equals(cycleId))
            .filter(t -> departmentId == null || t.departmentId().equals(departmentId))
            .filter(
                t ->
                    cycleType.isBlank()
                        || cycleRows.stream()
                            .anyMatch(c -> c.id().equals(t.cycleId()) && c.type().equals(cycleType)))
            .filter(
                t ->
                    supervisor.isBlank()
                        || t.supervisors().toLowerCase(Locale.ROOT)
                            .contains(supervisor.trim().toLowerCase(Locale.ROOT)))
            .sorted(order)
            .toList();
    m.addAttribute("data", QueryService.page(rows, q, page));
    m.addAttribute("cycles", query.cycles());
    m.addAttribute("departments", query.departments());
    m.addAttribute("cycleId", cycleId);
    m.addAttribute("departmentId", departmentId);
    m.addAttribute("cycleType", cycleType);
    m.addAttribute("supervisor", supervisor);
    m.addAttribute("sort", sort);
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

  @GetMapping("/forms/topics")
  public String form(@RequestParam(required = false) Long id, Model model) {
    return formView.show("topics", "Thông tin đề tài", query.topicForm(id), model, this::fields);
  }

  @PostMapping("/forms/topics")
  public String save(
      @Valid @ModelAttribute("form") Forms.Topic form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    return formView.save(
        "topics", "Thông tin đề tài", form, binding, model, redirect,
        () -> topics.save(form), this::fields);
  }

  private List<Field> fields(List<Views.Option> cycleOptions) {
    var fields = new ArrayList<Field>();
    fields.add(field("title", "Tên đề tài", "text"));
    fields.add(field("description", "Mô tả, mục tiêu và sản phẩm dự kiến", "textarea"));
    fields.add(select("cycleId", "Đợt đăng ký", cycleOptions, false));
    fields.add(select("departmentId", "Bộ môn", query.departments(), false));
    fields.add(field("maxGroups", "Số nhóm tối đa", "number"));
    if (actor.faculty())
      fields.add(
          select("supervisorIds", "Giảng viên hướng dẫn (1–2 người)", query.lecturers(), true));
    return fields;
  }
}
