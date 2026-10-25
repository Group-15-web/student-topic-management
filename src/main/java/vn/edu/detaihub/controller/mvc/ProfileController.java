package vn.edu.detaihub.controller.mvc;

import static vn.edu.detaihub.controller.support.FormViewSupport.done;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.service.AccountService;

@Controller
@RequiredArgsConstructor
public class ProfileController {
  private final AccountService accounts;

  @GetMapping("/profile")
  public String profile(Model m) {
    return "profile";
  }

  @PostMapping("/profile")
  public String profileSave(
      @Valid @ModelAttribute Forms.Profile f, BindingResult br, RedirectAttributes ra) {
    if (br.hasErrors())
      throw new vn.edu.detaihub.exception.RuleException("Họ tên không hợp lệ.", 400);
    accounts.profile(f);
    return done("/profile", ra);
  }

  @PostMapping("/profile/password")
  public String password(
      @Valid @ModelAttribute Forms.Password f, BindingResult br, RedirectAttributes ra) {
    if (br.hasErrors())
      throw new vn.edu.detaihub.exception.RuleException("Mật khẩu mới phải có 10–72 ký tự.", 400);
    accounts.password(f);
    return done("/profile", ra);
  }
}
