package vn.edu.detaihub.controller.mvc;

import static vn.edu.detaihub.controller.support.FormViewSupport.done;
import static vn.edu.detaihub.controller.support.FormViewSupport.field;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.controller.support.FormViewSupport;
import vn.edu.detaihub.controller.support.FormViewSupport.Field;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.dto.Views;
import vn.edu.detaihub.service.QueryService;
import vn.edu.detaihub.service.TopicService;

@Controller
@RequiredArgsConstructor
public class RegistrationCycleController {
  private final QueryService query;
  private final TopicService topics;
  private final FormViewSupport formView;

  @GetMapping("/cycles")
  public String cycles(Model m) {
    m.addAttribute("cycles", query.cycles());
    return "cycles";
  }

  @PostMapping("/cycles/{id}/import-whitelist")
  public String importWhitelist(
      @PathVariable Long id,
      @RequestParam(name = "file", required = false) MultipartFile file,
      @RequestParam(name = "manualCodes", required = false, defaultValue = "") String manualCodes,
      RedirectAttributes ra) {
    int count = topics.importEligibleStudents(id, file, manualCodes);
    ra.addFlashAttribute("notice", "Đã nạp thành công " + count + " sinh viên đủ điều kiện.");
    return "redirect:/cycles";
  }

  @PostMapping("/cycles/{id}/delete")
  public String deleteCycle(@PathVariable Long id, RedirectAttributes ra) {
    topics.deleteCycle(id);
    return done("/cycles", ra);
  }

  @GetMapping("/forms/cycles")
  public String form(@RequestParam(required = false) Long id, Model model) {
    return formView.show("cycles", "Thiết lập đợt đăng ký", query.cycleForm(id), model, this::fields);
  }

  @PostMapping("/forms/cycles")
  public String save(
      @Valid @ModelAttribute("form") Forms.Cycle form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    return formView.save(
        "cycles", "Thiết lập đợt đăng ký", form, binding, model, redirect,
        () -> topics.cycle(form), this::fields);
  }

  private List<Field> fields(List<Views.Option> cycleOptions) {
    var fields = new ArrayList<Field>();
    fields.add(field("name", "Tên đợt", "text"));
    fields.add(field("cycleType", "Loại đợt", "cycleType"));
    for (var pair :
        List.of(
            new String[] {"lecturerStart", "GV bắt đầu đề xuất"},
            new String[] {"lecturerEnd", "GV hết hạn đề xuất"},
            new String[] {"studentStart", "SV bắt đầu đăng ký"},
            new String[] {"studentEnd", "SV hết hạn đăng ký"},
            new String[] {"submissionDeadline", "Hạn nộp báo cáo"},
            new String[] {"reviewerDeadline", "Hạn GVPB (chỉ TLCN/KLTN)"},
            new String[] {"councilDate", "Ngày hội đồng (chỉ KLTN)"}))
      fields.add(field(pair[0], pair[1], "datetime-local"));
    fields.add(field("minMembers", "Thành viên tối thiểu (3–5)", "number"));
    fields.add(field("maxMembers", "Thành viên tối đa (3–5)", "number"));
    fields.add(field("reviewerCount", "Số giảng viên phản biện (0–5)", "number"));
    fields.add(field("councilRequired", "Yêu cầu đánh giá hội đồng", "checkbox"));
    fields.add(field("status", "Trạng thái đợt", "cycleStatus"));
    return fields;
  }
}
