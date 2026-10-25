package vn.edu.detaihub.controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.detaihub.exception.RuleException;

/** Preserves the former generic form route for unknown form types. */
@Controller
public class FormFallbackController {
  @GetMapping("/forms/{type}")
  public String unknownForm(
      @PathVariable String type, @RequestParam(required = false) Long id) {
    throw new RuleException("Không tìm thấy biểu mẫu.", 404);
  }
}
