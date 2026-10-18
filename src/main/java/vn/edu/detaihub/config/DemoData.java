package vn.edu.detaihub.config;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.service.TopicService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed", havingValue = "true")
public class DemoData implements CommandLineRunner {
  private final DomainRepository db;
  private final UserAccountRepository users;
  private final PasswordEncoder encoder;
  private final Map<String, Role> roles = new HashMap<>();

  @Override
  @Transactional
  public void run(String... args) {
    if (users.count() > 0) return;
    for (String code :
        List.of("SYSTEM_ADMIN", "FACULTY_MANAGER", "DEPARTMENT_HEAD", "LECTURER", "STUDENT")) {
      var r = new Role();
      r.setCode(code);
      db.save(r);
      roles.put(code, r);
    }
    var d1 = new Department();
    d1.setCode("CNPM");
    d1.setName("Công nghệ phần mềm");
    db.save(d1);
    var d2 = new Department();
    d2.setCode("HTTT");
    d2.setName("Hệ thống thông tin");
    db.save(d2);
    user("admin@demo.local", "Quản trị hệ thống", "SYSTEM_ADMIN");
    var faculty = user("khoa@demo.local", "TS. Nguyễn Minh Khang", "FACULTY_MANAGER", "LECTURER");
    lecturer(faculty, "GVKHOA", d1.getId());
    var head = user("tbm@demo.local", "TS. Trần Thu Hà", "DEPARTMENT_HEAD", "LECTURER");
    lecturer(head, "GVTBM", d1.getId());
    d1.setHeadUserId(head.getId());
    String[] names = {
      "ThS. Lê Hoàng Nam",
      "TS. Phạm Quỳnh Anh",
      "PGS. Trần Đức Huy",
      "TS. Nguyễn Ngọc Linh",
      "ThS. Võ Thanh Tùng",
      "TS. Đặng Hải Yến"
    };
    var ls = new ArrayList<Lecturer>();
    for (int i = 0; i < 6; i++)
      ls.add(
          lecturer(
              user("gv" + (i + 1) + "@demo.local", names[i], "LECTURER"),
              "GV00" + (i + 1),
              i < 5 ? d1.getId() : d2.getId()));
    String[] sn = {
      "Nguyễn Minh Anh",
      "Trần Gia Bảo",
      "Lê Khánh Linh",
      "Phạm Hoàng Phúc",
      "Võ Ngọc Mai",
      "Đặng Quốc Huy",
      "Bùi Thanh Trúc",
      "Nguyễn Đức Anh",
      "Trần Thảo Vy",
      "Lê Minh Quân",
      "Phan Tuệ Nhi",
      "Đỗ Thành Đạt"
    };
    var ss = new ArrayList<Student>();
    for (int i = 0; i < sn.length; i++) {
      var u = user("sv" + (i + 1) + "@demo.local", sn[i], "STUDENT");
      var s = new Student();
      s.setUserId(u.getId());
      s.setCode(String.format("SV%03d", i + 1));
      s.setIntake("2023");
      db.save(s);
      ss.add(s);
    }
    var proposal =
        cycle("Tiểu luận chuyên ngành · HK2 2026–2027", "SPECIALIZED_PROJECT", true, false);
    var registration =
        cycle("Khóa luận tốt nghiệp · Đợt 1 / 2026", "GRADUATION_THESIS", false, true);
    var course = cycle("Đồ án Lập trình Web · HK1", "COURSE", false, false);
    cycle("Nghiên cứu khoa học sinh viên · 2027", "RESEARCH", true, false);
    topic(
        "Nền tảng học tập cá nhân hóa ứng dụng AI",
        "Xây dựng nền tảng hỗ trợ sinh viên xác định lộ trình học dựa trên năng lực và mục tiêu cá nhân.\n\nMục tiêu\n• Phân tích hành vi học tập và đề xuất tài liệu phù hợp.\n• Thiết kế dashboard theo dõi tiến độ trực quan.\n• Đánh giá chất lượng đề xuất trên bộ dữ liệu thực nghiệm.\n\nSản phẩm dự kiến: website hoạt động, mô hình đề xuất và báo cáo đánh giá.",
        registration,
        d1,
        ls.get(0),
        "PUBLISHED");
    topic(
        "Hệ thống quản lý đề tài sinh viên theo mô hình MVC",
        "Thiết kế ứng dụng Spring Boot quản lý quy trình đề xuất, đăng ký và đánh giá đề tài. Áp dụng DTO, Spring Data JPA và Spring Security; xây dựng giao diện Thymeleaf theo từng vai trò.",
        registration,
        d1,
        ls.get(1),
        "PUBLISHED");
    topic(
        "Phân tích dữ liệu tuyển sinh và dự báo xu hướng",
        "Khảo sát dữ liệu tuyển sinh, xây dựng pipeline làm sạch và phân tích. Trực quan hóa các chỉ số hỗ trợ ra quyết định bằng dashboard, đánh giá mô hình dự báo qua nhiều kỳ.",
        registration,
        d2,
        ls.get(5),
        "PUBLISHED");
    topic(
        "Ứng dụng IoT giám sát môi trường phòng học",
        "Thu thập chỉ số môi trường từ cảm biến, lưu trữ và cảnh báo theo ngưỡng. Thiết kế giao diện theo dõi lịch sử, đánh giá độ tin cậy và thời gian phản hồi.",
        registration,
        d1,
        ls.get(2),
        "PUBLISHED");
    topic(
        "Website thư viện tài nguyên học thuật mở",
        "Tổ chức kho học liệu có tìm kiếm, phân loại và kiểm duyệt tài nguyên. Áp dụng phân quyền và kiểm thử các tình huống truy cập.",
        proposal,
        d1,
        ls.get(0),
        "SUBMITTED");
    topic(
        "Cổng đăng ký hoạt động sinh viên",
        "Xây dựng website quản lý hoạt động, đăng ký tham gia và thống kê. Tập trung vào quy trình đăng ký đồng thời và bảo vệ dữ liệu người dùng.",
        course,
        d1,
        ls.get(0),
        "PUBLISHED");
    var g = group("Nhóm 01 · Beyond the Code", registration, ss.subList(0, 3));
    var work = group("Nhóm 02 · Green Tech", registration, ss.subList(3, 6));
    work.setStatus("LOCKED");
    var t =
        topic(
            "Nền tảng cộng tác nghiên cứu cho sinh viên",
            "Xây dựng không gian cộng tác cho nhóm nghiên cứu: quản lý tài liệu, theo dõi mốc tiến độ và tổng hợp kết quả.\n\nHồ sơ demo này đã được chấp thuận. Nhóm trưởng đăng nhập để nộp báo cáo, sau đó giảng viên thực hiện đánh giá.",
            registration,
            d1,
            ls.get(0),
            "PUBLISHED");
    var r = new TopicRegistration();
    r.setGroupId(work.getId());
    r.setTopicId(t.getId());
    r.setStatus("APPROVED");
    r.setDecidedBy(faculty.getId());
    db.save(r);
    var reviewer = new ReviewerAssignment();
    reviewer.setRegistrationId(r.getId());
    reviewer.setLecturerId(ls.get(1).getId());
    db.save(reviewer);
    evaluation(r.getId(), ls.get(1).getId(), "REVIEWER");
    var council = new ReviewCouncil();
    council.setCycleId(registration.getId());
    council.setName("Hội đồng Công nghệ phần mềm 01");
    council.setReportAt(TopicService.now().plusDays(60));
    council.setLocation("Phòng A.302");
    db.save(council);
    for (int i = 2; i <= 4; i++) {
      var m = new CouncilMember();
      m.setCouncilId(council.getId());
      m.setLecturerId(ls.get(i).getId());
      m.setMemberRole(i == 2 ? "PRESIDENT" : i == 3 ? "SECRETARY" : "MEMBER");
      db.save(m);
      evaluation(r.getId(), ls.get(i).getId(), "COUNCIL");
    }
    var ca = new CouncilAssignment();
    ca.setCouncilId(council.getId());
    ca.setRegistrationId(r.getId());
    db.save(ca);
    announcement(
        "Mở đăng ký đề tài khóa luận tốt nghiệp đợt 1",
        "Khoa CNTT thông báo mở đăng ký đề tài khóa luận tốt nghiệp. Sinh viên thành lập nhóm từ 3–5 thành viên, lựa chọn đề tài đã công bố và gửi đăng ký trong thời gian quy định.\n\nVui lòng kiểm tra thông tin nhóm và email cá nhân trước khi đăng ký.",
        faculty.getId());
    announcement(
        "Hướng dẫn nộp báo cáo và quy trình đánh giá",
        "Nhóm trưởng là người đại diện nộp báo cáo. Hệ thống chấp nhận PDF, DOCX hoặc ZIP tối đa 20 MB. Sau khi có điểm đánh giá, báo cáo sẽ được khóa phiên bản.\n\nKết quả chỉ hiển thị sau khi Khoa công bố.",
        faculty.getId());
  }

  private UserAccount user(String email, String name, String... codes) {
    var u = new UserAccount();
    u.setEmail(email);
    u.setFullName(name);
    u.setPasswordHash(encoder.encode("Demo@2026!"));
    u.setEnabled(true);
    u.setSecurityVersion(0);
    db.save(u);
    for (String code : codes) {
      var ur = new UserRole();
      ur.setUserId(u.getId());
      ur.setRoleId(roles.get(code).getId());
      db.save(ur);
    }
    return u;
  }

  private Lecturer lecturer(UserAccount u, String code, Long departmentId) {
    var l = new Lecturer();
    l.setUserId(u.getId());
    l.setCode(code);
    l.setDepartmentId(departmentId);
    return db.save(l);
  }

  private RegistrationCycle cycle(String name, String type, boolean phase1, boolean council) {
    var n = TopicService.now();
    var c = new RegistrationCycle();
    c.setName(name);
    c.setCycleType(type);
    c.setLecturerStart(n.minusDays(phase1 ? 2 : 20));
    c.setLecturerEnd(n.plusDays(phase1 ? 12 : -2));
    c.setStudentStart(n.plusDays(phase1 ? 13 : -1));
    c.setStudentEnd(n.plusDays(phase1 ? 27 : 14));
    c.setSubmissionDeadline(n.plusDays(45));
    c.setReviewerDeadline(
        Set.of("SPECIALIZED_PROJECT", "GRADUATION_THESIS").contains(type) ? n.plusDays(58) : null);
    c.setCouncilDate(type.equals("GRADUATION_THESIS") ? n.plusDays(60) : null);
    c.setMinMembers(3);
    c.setMaxMembers(5);
    c.setReviewerCount(1);
    c.setCouncilRequired(council);
    c.setStatus("ACTIVE");
    return db.save(c);
  }

  private Topic topic(
      String title, String content, RegistrationCycle c, Department d, Lecturer l, String status) {
    var t = new Topic();
    t.setTitle(title);
    t.setDescription(content);
    t.setCycleId(c.getId());
    t.setDepartmentId(d.getId());
    t.setAuthorId(l.getUserId());
    t.setStatus(status);
    t.setMaxGroups(1);
    db.save(t);
    var s = new TopicSupervisor();
    s.setTopicId(t.getId());
    s.setLecturerId(l.getId());
    db.save(s);
    return t;
  }

  private StudentGroup group(String name, RegistrationCycle c, List<Student> students) {
    var g = new StudentGroup();
    g.setName(name);
    g.setCycleId(c.getId());
    g.setLeaderId(students.getFirst().getId());
    g.setStatus("FORMING");
    db.save(g);
    for (var s : students) {
      s.setActiveGroupId(g.getId());
      var m = new GroupMember();
      m.setGroupId(g.getId());
      m.setStudentId(s.getId());
      m.setStatus("ACTIVE");
      db.save(m);
    }
    return g;
  }

  private void evaluation(Long registration, Long lecturer, String type) {
    var e = new Evaluation();
    e.setRegistrationId(registration);
    e.setLecturerId(lecturer);
    e.setEvaluationType(type);
    e.setStatus("DRAFT");
    db.save(e);
  }

  private void announcement(String title, String text, Long author) {
    var a = new Announcement();
    a.setTitle(title);
    a.setContent(text);
    a.setAuthorId(author);
    a.setAudience("ALL");
    a.setStatus("PUBLISHED");
    db.save(a);
  }
}
