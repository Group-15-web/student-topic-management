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
import vn.edu.detaihub.service.AnnouncementService;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class AnnouncementController {
  private final QueryService query;
  private final AnnouncementService announcements;
  private final FormViewSupport formView;

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

  @GetMapping("/forms/announcements")
  public String form(@RequestParam(required = false) Long id, Model model) {
    return formView.show("announcements", "Biên soạn thông báo", query.announcementForm(id), model, this::fields);
  }

  @PostMapping("/forms/announcements")
  public String save(
      @Valid @ModelAttribute("form") Forms.Announcement form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    return formView.save(
        "announcements", "Biên soạn thông báo", form, binding, model, redirect,
        () -> announcements.save(form), this::fields);
  }

  private List<Field> fields(List<Views.Option> cycleOptions) {
    var fields = new ArrayList<Field>();
    fields.add(field("title", "Tiêu đề", "text"));
    fields.add(field("content", "Nội dung thông báo", "textarea"));
    fields.add(field("audience", "Đối tượng nhận", "audience"));
    fields.add(
        select("departmentId", "Bộ môn (để trống nếu toàn khoa)", query.departments(), false));
    fields.add(field("status", "Trạng thái", "announcementStatus"));
    return fields;
  }
}
