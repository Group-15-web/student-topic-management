package vn.edu.detaihub.controller;

import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.dto.*;
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.security.ActorService;
import vn.edu.detaihub.service.*;

@Controller
@RequiredArgsConstructor
public class FormController {
  private final QueryService query;
  private final ActorService actor;
  private final TopicService topics;
  private final GroupService groups;
  private final AccountService accounts;
  private final AssessmentService assessments;
  private final AnnouncementService announcements;

  public record Field(
      String name, String label, String type, List<Views.Option> options, String hint) {}

  private Field f(String name, String label, String type) {
    return new Field(name, label, type, List.of(), "");
  }

  private Field select(String name, String label, List<Views.Option> options, boolean multiple) {
    return new Field(
        name,
        label,
        multiple ? "multiple" : "select",
        options,
        multiple ? "Giữ Ctrl để chọn nhiều người." : "");
  }

  @GetMapping("/forms/{type}")
  public String form(@PathVariable String type, @RequestParam(required = false) Long id, Model m) {
    Object form =
        switch (type) {
          case "topics" -> query.topicForm(id);
          case "cycles" -> query.cycleForm(id);
          case "accounts" -> query.accountForm(id);
          case "departments" -> query.departmentForm(id);
          case "announcements" -> query.announcementForm(id);
          case "groups" -> {
            actor.need("STUDENT");
            yield new Forms.Group();
          }
          case "councils" -> {
            actor.need("FACULTY_MANAGER");
            yield assessments.councilForm(id);
          }
          default -> throw new RuleException("Không tìm thấy biểu mẫu.", 404);
        };
    m.addAttribute("form", form);
    decorate(type, m);
    return "form";
  }

  private void decorate(String type, Model m) {
    var fields = new ArrayList<Field>();
    String title = "";
    var cycleOptions =
        query.cycles().stream().map(c -> new Views.Option(c.id(), c.name())).toList();
    switch (type) {
      case "topics" -> {
        title = "Thông tin đề tài";
        fields.add(f("title", "Tên đề tài", "text"));
        fields.add(f("description", "Mô tả, mục tiêu và sản phẩm dự kiến", "textarea"));
        fields.add(select("cycleId", "Đợt đăng ký", cycleOptions, false));
        fields.add(select("departmentId", "Bộ môn", query.departments(), false));
        fields.add(f("maxGroups", "Số nhóm tối đa", "number"));
        if (actor.faculty())
          fields.add(
              select("supervisorIds", "Giảng viên hướng dẫn (1–2 người)", query.lecturers(), true));
      }
      case "cycles" -> {
        title = "Thiết lập đợt đăng ký";
        fields.add(f("name", "Tên đợt", "text"));
        fields.add(f("cycleType", "Loại đợt", "cycleType"));
        for (var pair :
            List.of(
                new String[] {"lecturerStart", "GV bắt đầu đề xuất"},
                new String[] {"lecturerEnd", "GV hết hạn đề xuất"},
                new String[] {"studentStart", "SV bắt đầu đăng ký"},
                new String[] {"studentEnd", "SV hết hạn đăng ký"},
                new String[] {"submissionDeadline", "Hạn nộp báo cáo"},
                new String[] {"reviewerDeadline", "Hạn GVPB (chỉ TLCN/KLTN)"},
                new String[] {"councilDate", "Ngày hội đồng (chỉ KLTN)"}))
          fields.add(f(pair[0], pair[1], "datetime-local"));
        fields.add(f("minMembers", "Thành viên tối thiểu (3–5)", "number"));
        fields.add(f("maxMembers", "Thành viên tối đa (3–5)", "number"));
        fields.add(f("reviewerCount", "Số giảng viên phản biện (0–5)", "number"));
        fields.add(f("councilRequired", "Yêu cầu đánh giá hội đồng", "checkbox"));
        fields.add(f("status", "Trạng thái đợt", "cycleStatus"));
      }
      case "accounts" -> {
        title = "Tài khoản & hồ sơ";
        fields.add(f("fullName", "Họ và tên", "text"));
        fields.add(f("email", "Email đăng nhập / nhận kết quả", "email"));
        if (actor.admin())
          fields.add(f("password", "Mật khẩu (để trống khi không thay đổi)", "password"));
        fields.add(f("role", "Loại hồ sơ / vai trò chính", "role"));
        fields.add(f("code", "MSSV / mã GV (Admin: ADMIN)", "text"));
        fields.add(select("departmentId", "Bộ môn (dành cho GV)", query.departments(), false));
        fields.add(f("intake", "Khóa sinh viên", "text"));
        if (actor.admin()) fields.add(f("enabled", "Tài khoản đang hoạt động", "checkbox"));
      }
      case "departments" -> {
        title = "Thông tin bộ môn";
        fields.add(f("code", "Mã bộ môn", "text"));
        fields.add(f("name", "Tên bộ môn", "text"));
        fields.add(
            select("headUserId", "Trưởng bộ môn (cấp role trước)", query.lecturerUsers(), false));
      }
      case "groups" -> {
        title = "Tạo nhóm thực hiện";
        fields.add(f("name", "Tên nhóm", "text"));
        fields.add(select("cycleId", "Đợt đăng ký", cycleOptions, false));
      }
      case "councils" -> {
        title = "Thành lập hội đồng";
        fields.add(f("name", "Tên hội đồng", "text"));
        fields.add(select("cycleId", "Đợt đăng ký", cycleOptions, false));
        fields.add(select("presidentId", "Chủ tịch", query.lecturers(), false));
        fields.add(select("secretaryId", "Thư ký", query.lecturers(), false));
        fields.add(select("memberIds", "Thành viên khác (1–3 người)", query.lecturers(), true));
        fields.add(f("reportAt", "Thời gian báo cáo", "datetime-local"));
        fields.add(f("location", "Phòng / địa điểm", "text"));
      }
      case "announcements" -> {
        title = "Biên soạn thông báo";
        fields.add(f("title", "Tiêu đề", "text"));
        fields.add(f("content", "Nội dung thông báo", "textarea"));
        fields.add(f("audience", "Đối tượng nhận", "audience"));
        fields.add(
            select("departmentId", "Bộ môn (để trống nếu toàn khoa)", query.departments(), false));
        fields.add(f("status", "Trạng thái", "announcementStatus"));
      }
    }
    m.addAttribute("fields", fields);
    m.addAttribute("title", title);
    m.addAttribute("type", type);
  }

  private String save(
      String type, Object f, BindingResult br, Model m, RedirectAttributes ra, Runnable work) {
    if (!br.hasErrors())
      try {
        work.run();
        ra.addFlashAttribute("success", "Đã lưu dữ liệu thành công.");
        return "redirect:/" + type;
      } catch (RuleException ex) {
        m.addAttribute("error", ex.getMessage());
      } catch (org.springframework.dao.DataIntegrityViolationException ex) {
        m.addAttribute(
            "error",
            "Dữ liệu bị trùng hoặc đang được tham chiếu. Kiểm tra mã, email và các liên kết.");
      }
    m.addAttribute("form", f);
    decorate(type, m);
    return "form";
  }

  @PostMapping("/forms/topics")
  public String topic(
      @Valid @ModelAttribute("form") Forms.Topic f,
      BindingResult b,
      Model m,
      RedirectAttributes r) {
    return save("topics", f, b, m, r, () -> topics.save(f));
  }

  @PostMapping("/forms/cycles")
  public String cycle(
      @Valid @ModelAttribute("form") Forms.Cycle f,
      BindingResult b,
      Model m,
      RedirectAttributes r) {
    return save("cycles", f, b, m, r, () -> topics.cycle(f));
  }

  @PostMapping("/forms/accounts")
  public String account(
      @Valid @ModelAttribute("form") Forms.Account f,
      BindingResult b,
      Model m,
      RedirectAttributes r) {
    return save("accounts", f, b, m, r, () -> accounts.save(f));
  }

  @PostMapping("/forms/departments")
  public String department(
      @Valid @ModelAttribute("form") Forms.Department f,
      BindingResult b,
      Model m,
      RedirectAttributes r) {
    return save("departments", f, b, m, r, () -> accounts.department(f));
  }

  @PostMapping("/forms/groups")
  public String group(
      @Valid @ModelAttribute("form") Forms.Group f,
      BindingResult b,
      Model m,
      RedirectAttributes r) {
    return save("groups", f, b, m, r, () -> groups.create(f));
  }

  @PostMapping("/forms/councils")
  public String council(
      @Valid @ModelAttribute("form") Forms.Council f,
      BindingResult b,
      Model m,
      RedirectAttributes r) {
    return save("councils", f, b, m, r, () -> assessments.council(f));
  }

  @PostMapping("/forms/announcements")
  public String announcement(
      @Valid @ModelAttribute("form") Forms.Announcement f,
      BindingResult b,
      Model m,
      RedirectAttributes r) {
    return save("announcements", f, b, m, r, () -> announcements.save(f));
  }
}
