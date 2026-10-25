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
import vn.edu.detaihub.service.AccountService;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class AccountController {
  private final QueryService query;
  private final ActorService actor;
  private final AccountService accounts;
  private final FormViewSupport formView;

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

  @GetMapping("/forms/accounts")
  public String form(@RequestParam(required = false) Long id, Model model) {
    return formView.show("accounts", "Tài khoản & hồ sơ", query.accountForm(id), model, this::fields);
  }

  @PostMapping("/forms/accounts")
  public String save(
      @Valid @ModelAttribute("form") Forms.Account form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    return formView.save(
        "accounts", "Tài khoản & hồ sơ", form, binding, model, redirect,
        () -> accounts.save(form), this::fields);
  }

  private List<Field> fields(List<Views.Option> cycleOptions) {
    var fields = new ArrayList<Field>();
    fields.add(field("fullName", "Họ và tên", "text"));
    fields.add(field("email", "Email đăng nhập / nhận kết quả", "email"));
    if (actor.admin())
      fields.add(field("password", "Mật khẩu (để trống khi không thay đổi)", "password"));
    fields.add(field("role", "Loại hồ sơ / vai trò chính", "role"));
    fields.add(field("code", "MSSV / mã GV (Admin: ADMIN)", "text"));
    fields.add(select("departmentId", "Bộ môn (dành cho GV)", query.departments(), false));
    fields.add(field("intake", "Khóa sinh viên", "text"));
    if (actor.admin()) fields.add(field("enabled", "Tài khoản đang hoạt động", "checkbox"));
    return fields;
  }
}
