package vn.edu.detaihub.controller.support;

import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.dto.Views;
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.service.QueryService;

/** Shared MVC presentation behavior, not domain validation or authorization. */
@Component
@RequiredArgsConstructor
public class FormViewSupport {
  private final QueryService query;

  public record Field(
      String name, String label, String type, List<Views.Option> options, String hint) {}

  public static Field field(String name, String label, String type) {
    return new Field(name, label, type, List.of(), "");
  }

  public static Field select(
      String name, String label, List<Views.Option> options, boolean multiple) {
    return new Field(
        name, label, multiple ? "multiple" : "select", options,
        multiple ? "Giữ Ctrl để chọn nhiều người." : "");
  }

  public String show(
      String type,
      String title,
      Object form,
      Model model,
      Function<List<Views.Option>, List<Field>> fields) {
    model.addAttribute("form", form);
    var cycleOptions =
        query.cycles().stream().map(cycle -> new Views.Option(cycle.id(), cycle.name())).toList();
    model.addAttribute("fields", fields.apply(cycleOptions));
    model.addAttribute("title", title);
    model.addAttribute("type", type);
    return "form";
  }

  public String save(
      String type,
      String title,
      Object form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect,
      Runnable work,
      Function<List<Views.Option>, List<Field>> fields) {
    if (!binding.hasErrors()) {
      try {
        work.run();
        redirect.addFlashAttribute("success", "Đã lưu dữ liệu thành công.");
        return "redirect:/" + type;
      } catch (RuleException exception) {
        model.addAttribute("error", exception.getMessage());
      } catch (DataIntegrityViolationException exception) {
        model.addAttribute(
            "error", "Dữ liệu bị trùng hoặc đang được tham chiếu. Kiểm tra mã, email và các liên kết.");
      }
    }
    return show(type, title, form, model, fields);
  }

  public static String done(String path, RedirectAttributes redirect) {
    redirect.addFlashAttribute("success", "Đã lưu thay đổi thành công.");
    return "redirect:" + path;
  }
}
