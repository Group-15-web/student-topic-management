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

  @Value("${app.storage}")
  private String storage;

  @PreAuthorize("hasRole('STUDENT')")
  public void upload(Long registrationId, MultipartFile file) throws IOException {
    actor.need("STUDENT");
    // 1. Khóa Registration bằng Pessimistic Lock
    var r = registrationRepo.findByIdWithLock(registrationId)
        .orElseThrow(() -> new RuleException("Không tìm thấy hồ sơ đăng ký đề tài.", 404));

    // 2. Lấy thông tin nhóm và kiểm tra quyền: Chỉ Trưởng nhóm mới có quyền nộp bài
    var g = groupRepo.findById(r.getGroupId())
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

    // Ca biên 5: Chuẩn hóa tên file, chống tấn công Path Traversal
    String rawName = Optional.ofNullable(file.getOriginalFilename()).orElse("report");
    String name = rawName.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).trim();
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

    boolean permitted = groups.canView(r)
        && (!actor.departmentHead(t.getDepartmentId())
            || actor.faculty()
            || groups.member(r.getGroupId())
            || isAssignedReviewer
            || isSupervisor);
    actor.allow(permitted);

    // Xác thực đường dẫn an toàn tuyệt đối chống Path Traversal
    var baseDir = Path.of(storage).toAbsolutePath().normalize();
    var path = baseDir.resolve(s.getStorageKey()).normalize();
    require(path.startsWith(baseDir), "Đường dẫn không hợp lệ.");
    require(Files.exists(path), "File không còn trên storage.");

    return new Download(path, s.getFileName(), s.getContentType());
  }
}
