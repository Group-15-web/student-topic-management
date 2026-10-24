package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.security.ActorService;
import vn.edu.detaihub.service.EmailService;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionService {
  private final SubmissionRepository submissionRepo;
  private final TopicRegistrationRepository registrationRepo;
  private final StudentGroupRepository groupRepo;
  private final RegistrationCycleRepository cycleRepo;
  private final EvaluationRepository evaluationRepo;
  private final TopicRepository topicRepo;
  private final TopicSupervisorRepository supervisorRepo;
  private final LecturerRepository lecturerRepo;
  private final ActorService actor;
  private final GroupService groups;
  private final AuditService audit;
  private final EmailService emailService;
  private final StudentRepository studentRepo;
  private final UserAccountRepository userAccountRepo;

  @Value("${app.storage}")
  private String storage;

  @PreAuthorize("hasRole('STUDENT')")
  public void upload(Long registrationId, MultipartFile file) throws IOException {
    actor.need("STUDENT");
    // 1. Khóa Registration bằng Pessimistic Lock
    var r = registrationRepo.findByIdWithLock(registrationId)
        .orElseThrow(() -> new RuleException("Không tìm thấy hồ sơ đăng ký đề tài.", 404));

    // 2. Lấy thông tin nhóm và khóa Pessimistic Write: Chống concurrent complete/disband khi upload
    var g = groupRepo.findByIdWithLock(r.getGroupId())
        .orElseThrow(() -> new RuleException("Không tìm thấy nhóm.", 404));
    actor.allow(groups.leader(g));

    // Ca biên 1: Nhóm phải được duyệt và bị khóa (APPROVED + LOCKED) mới được nộp báo cáo
    require(
        r.getStatus().equals("APPROVED") && g.getStatus().equals("LOCKED"),
        "Nhóm chưa được duyệt hoặc đã hoàn tất.");

    // Ca biên 2: Kiểm tra đợt đang ACTIVE và thời gian nộp bài còn hiệu lực
    var c = cycleRepo.findById(g.getCycleId())
        .orElseThrow(() -> new RuleException("Không tìm thấy đợt đăng ký.", 404));
    require(
        TopicService.running(c) && TopicService.now().isBefore(c.getSubmissionDeadline()),
        "Đã hết hạn nộp báo cáo.");

    // Ca biên 3: Nếu đề tài đã được giảng viên chấm điểm (có score), cấm nộp đè phiên bản mới
    boolean alreadyGraded = evaluationRepo.existsByRegistrationIdAndScoreIsNotNull(registrationId);
    require(!alreadyGraded, "Báo cáo đã được chấm; không được thay phiên bản.");

    // Ca biên 4: Kiểm tra dung lượng file (Không rỗng và tối đa 20MB)
    require(
        file != null && !file.isEmpty() && file.getSize() <= 20 * 1024 * 1024,
        "File phải có nội dung và không quá 20 MB.");

    // Ca biên 5: Chuẩn hóa tên file, chống tấn công Path Traversal, XSS và Header Injection
    String rawName = Optional.ofNullable(file.getOriginalFilename()).orElse("report");
    String name = rawName.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).trim();
    // Sanitize chỉ giữ lại chữ cái, số, gạch dưới, gạch ngang và dấu chấm
    name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
    require(!name.isBlank() && name.length() < 200, "Tên file không hợp lệ hoặc quá dài.");

    String extension =
        name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
    require(Set.of(".pdf", ".docx", ".zip").contains(extension), "Chỉ nhận PDF, DOCX hoặc ZIP.");

    // Ca biên 6: Kiểm tra Magic Bytes thực tế trong luồng nhị phân
    byte[] head;
    try (var in = file.getInputStream()) {
      head = in.readNBytes(5);
    }
    require(
        extension.equals(".pdf")
            ? new String(head, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-")
            : head.length > 1 && head[0] == 'P' && head[1] == 'K',
        "Nội dung file không khớp định dạng thực tế.");

    // 3. Lưu trữ file an toàn bằng UUID Storage Key
    var dir = Path.of(storage).toAbsolutePath().normalize();
    Files.createDirectories(dir);
    String key = UUID.randomUUID() + extension;
    Path target = dir.resolve(key);
    file.transferTo(target);

    // Dọn dẹp file vật lý nếu transaction DB bị rollback
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              try {
                Files.deleteIfExists(target);
              } catch (IOException ignored) {
              }
            }
          }
        });

    // 4. Tối ưu tìm số phiên bản tiếp theo trực tiếp từ DB bằng câu lệnh MAX()
    int maxVersion = submissionRepo.findMaxVersionNoByRegistrationId(registrationId);

    var s = new Submission();
    s.setRegistrationId(registrationId);
    s.setVersionNo(maxVersion + 1);
    s.setStorageKey(key);
    s.setFileName(name);
    s.setContentType(
        extension.equals(".pdf")
            ? "application/pdf"
            : extension.equals(".docx")
                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/zip");
    s.setFileSize(file.getSize());
    s.setSubmittedBy(actor.user().getId());
    submissionRepo.save(s);

    audit.record("UPLOAD_REPORT", "submission", s.getId(), "Phiên bản " + s.getVersionNo());
  }

  public record Download(Path path, String name, String contentType) {}

  @Transactional(readOnly = true)
  public Download download(Long id) {
    var s = submissionRepo.findById(id)
        .orElseThrow(() -> new RuleException("Không tìm thấy file nộp bài.", 404));
    var r = registrationRepo.findById(s.getRegistrationId())
        .orElseThrow(() -> new RuleException("Không tìm thấy đơn đăng ký.", 404));
    var t = topicRepo.findById(r.getTopicId())
        .orElseThrow(() -> new RuleException("Không tìm thấy đề tài.", 404));

    // Kiểm tra quyền download: Khoa, Bộ môn, Thành viên nhóm, GV phản biện, GV hướng dẫn
    var currentLecturer = lecturerRepo.findByUserId(actor.user().getId());
    boolean isAssignedReviewer = currentLecturer
        .map(l -> evaluationRepo.existsByRegistrationIdAndLecturerId(r.getId(), l.getId()))
        .orElse(false);
    boolean isSupervisor = currentLecturer
        .map(l -> supervisorRepo.existsByTopicIdAndLecturerId(t.getId(), l.getId()))
        .orElse(false);

    boolean permitted = actor.faculty()
        || groups.member(r.getGroupId())
        || isSupervisor
        || isAssignedReviewer
        || actor.departmentHead(t.getDepartmentId());
    actor.allow(permitted);

    // Xác thực đường dẫn an toàn tuyệt đối chống Path Traversal
    var baseDir = Path.of(storage).toAbsolutePath().normalize();
    var path = baseDir.resolve(s.getStorageKey()).normalize();
    require(path.startsWith(baseDir), "Đường dẫn không hợp lệ.");
    require(Files.exists(path), "File không còn trên storage.");

    return new Download(path, s.getFileName(), s.getContentType());
  }

  @Transactional(readOnly = true)
  public int remindUpcomingDeadlines(int hoursBefore) {
    var now = TopicService.now();
    var threshold = now.plusHours(hoursBefore);
    int remindedCount = 0;

    var activeCycles = cycleRepo.findAll().stream()
        .filter(c -> "ACTIVE".equals(c.getStatus()))
        .filter(c -> c.getSubmissionDeadline() != null && c.getSubmissionDeadline().isAfter(now) && c.getSubmissionDeadline().isBefore(threshold))
        .toList();

    for (var cycle : activeCycles) {
      var approvedRegistrations = registrationRepo.findByStatusIn(List.of("APPROVED")).stream()
          .filter(r -> {
            var group = groupRepo.findById(r.getGroupId());
            return group.map(g -> g.getCycleId().equals(cycle.getId())).orElse(false);
          })
          .toList();

      for (var reg : approvedRegistrations) {
        // Neu nhom chua co ban nop nao
        boolean hasSubmissions = !submissionRepo.findByRegistrationId(reg.getId()).isEmpty();
        if (!hasSubmissions) {
          var groupOpt = groupRepo.findById(reg.getGroupId());
          var topicOpt = topicRepo.findById(reg.getTopicId());
          if (groupOpt.isPresent() && topicOpt.isPresent()) {
            var group = groupOpt.get();
            var topic = topicOpt.get();
            var leaderStudentOpt = studentRepo.findById(group.getLeaderId());
            if (leaderStudentOpt.isPresent()) {
              var userAccountOpt = userAccountRepo.findById(leaderStudentOpt.get().getUserId());
              if (userAccountOpt.isPresent()) {
                var user = userAccountOpt.get();
                String subject = "[Đề Tài Hub] Nhắc nhở: Sắp hết hạn nộp báo cáo đề tài";
                String body = "Xin chào " + user.getFullName() + " (Trưởng nhóm " + group.getName() + "),\n\n"
                    + "Đề tài '" + topic.getTitle() + "' của nhóm bạn chỉ còn dưới " + hoursBefore + " giờ nữa là đến hạn nộp báo cáo.\n"
                    + "Hạn chót: " + cycle.getSubmissionDeadline() + "\n\n"
                    + "Vui lòng đăng nhập hệ thống Đề Tài Hub và tải lên bản báo cáo đúng hạn.\n\n"
                    + "Trân trọng,\nBan Quản Lý Khóa Luận / Đề Tài Tốt Nghiệp";
                emailService.sendDirect(user.getEmail(), subject, body);
                remindedCount++;
              }
            }
          }
        }
      }
    }
    return remindedCount;
  }
}