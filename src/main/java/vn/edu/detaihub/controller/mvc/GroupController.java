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
import vn.edu.detaihub.service.GroupService;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class GroupController {
  private final QueryService query;
  private final ActorService actor;
  private final GroupService groups;
  private final FormViewSupport formView;

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

  @GetMapping("/forms/groups")
  public String form(@RequestParam(required = false) Long id, Model model) {
    actor.need("STUDENT");
    return formView.show("groups", "Tạo nhóm thực hiện", new Forms.Group(), model, this::fields);
  }

  @PostMapping("/forms/groups")
  public String save(
      @Valid @ModelAttribute("form") Forms.Group form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    return formView.save(
        "groups", "Tạo nhóm thực hiện", form, binding, model, redirect,
        () -> groups.create(form), this::fields);
  }

  private List<Field> fields(List<Views.Option> cycleOptions) {
    var fields = new ArrayList<Field>();
    fields.add(field("name", "Tên nhóm", "text"));
    fields.add(select("cycleId", "Đợt đăng ký", cycleOptions, false));
    return fields;
  }
}
