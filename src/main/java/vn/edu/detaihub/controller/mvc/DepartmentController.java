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
import vn.edu.detaihub.service.AccountService;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class DepartmentController {
  private final QueryService query;
  private final AccountService accounts;
  private final FormViewSupport formView;

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

  @GetMapping("/forms/departments")
  public String form(@RequestParam(required = false) Long id, Model model) {
    return formView.show("departments", "Thông tin bộ môn", query.departmentForm(id), model, this::fields);
  }

  @PostMapping("/forms/departments")
  public String save(
      @Valid @ModelAttribute("form") Forms.Department form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    return formView.save(
        "departments", "Thông tin bộ môn", form, binding, model, redirect,
        () -> accounts.department(form), this::fields);
  }

  private List<Field> fields(List<Views.Option> cycleOptions) {
    var fields = new ArrayList<Field>();
    fields.add(field("code", "Mã bộ môn", "text"));
    fields.add(field("name", "Tên bộ môn", "text"));
    fields.add(
        select("headUserId", "Trưởng bộ môn (cấp role trước)", query.lecturerUsers(), false));
    return fields;
  }
}
