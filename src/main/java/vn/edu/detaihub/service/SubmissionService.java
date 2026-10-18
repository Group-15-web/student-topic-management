package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionService {
  private final DomainRepository db;
  private final ActorService actor;
  private final GroupService groups;
  private final AuditService audit;

  @Value("${app.storage}")
  private String storage;

  public void upload(Long registrationId, MultipartFile file) throws IOException {
    var r = db.lock(TopicRegistration.class, registrationId);
    var g = db.get(StudentGroup.class, r.getGroupId());
    actor.allow(groups.leader(g));
    require(
        r.getStatus().equals("APPROVED") && g.getStatus().equals("LOCKED"),
        "Nhóm chưa được duyệt hoặc đã hoàn tất.");
    var c = db.get(RegistrationCycle.class, g.getCycleId());
    require(
        TopicService.running(c) && TopicService.now().isBefore(c.getSubmissionDeadline()),
        "Đã hết hạn nộp báo cáo.");
    require(
        db.all(Evaluation.class).stream()
            .noneMatch(e -> e.getRegistrationId().equals(registrationId) && e.getScore() != null),
        "Báo cáo đã được chấm; không được thay phiên bản.");
    require(
        !file.isEmpty() && file.getSize() <= 20 * 1024 * 1024,
        "File phải có nội dung và không quá 20 MB.");
    String name =
        Optional.ofNullable(file.getOriginalFilename()).orElse("report").replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1);
    require(name.length() < 200, "Tên file quá dài.");
    String extension =
        name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
    require(Set.of(".pdf", ".docx", ".zip").contains(extension), "Chỉ nhận PDF, DOCX hoặc ZIP.");
    byte[] head;
    try (var in = file.getInputStream()) {
      head = in.readNBytes(5);
    }
    require(
        extension.equals(".pdf")
            ? new String(head, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-")
            : head.length > 1 && head[0] == 'P' && head[1] == 'K',
        "Nội dung file không khớp định dạng.");
    var dir = Path.of(storage).toAbsolutePath().normalize();
    Files.createDirectories(dir);
    String key = UUID.randomUUID() + extension;
    Path target = dir.resolve(key);
    file.transferTo(target);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED)
              try {
                Files.deleteIfExists(target);
              } catch (IOException ignored) {
              }
          }
        });
    var s = new Submission();
    s.setRegistrationId(registrationId);
    s.setVersionNo(
        db.all(Submission.class).stream()
                .filter(x -> x.getRegistrationId().equals(registrationId))
                .mapToInt(Submission::getVersionNo)
                .max()
                .orElse(0)
            + 1);
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
    db.save(s);
    audit.record("UPLOAD_REPORT", "submission", s.getId(), "Phiên bản " + s.getVersionNo());
  }

  public record Download(Path path, String name, String contentType) {}

  @Transactional(readOnly = true)
  public Download download(Long id) {
    var s = db.get(Submission.class, id);
    var r = db.get(TopicRegistration.class, s.getRegistrationId());
    var t = db.get(Topic.class, r.getTopicId());
    actor.allow(
        groups.canView(r)
            && (!actor.departmentHead(t.getDepartmentId())
                || actor.faculty()
                || groups.member(r.getGroupId())
                || db.all(Evaluation.class).stream()
                    .anyMatch(
                        e ->
                            e.getRegistrationId().equals(r.getId())
                                && db.get(Lecturer.class, e.getLecturerId())
                                    .getUserId()
                                    .equals(actor.user().getId()))
                || db.all(TopicSupervisor.class).stream()
                    .anyMatch(
                        x ->
                            x.getTopicId().equals(t.getId())
                                && db.get(Lecturer.class, x.getLecturerId())
                                    .getUserId()
                                    .equals(actor.user().getId()))));
    var path = Path.of(storage).toAbsolutePath().normalize().resolve(s.getStorageKey()).normalize();
    require(
        path.startsWith(Path.of(storage).toAbsolutePath().normalize()), "Đường dẫn không hợp lệ.");
    require(Files.exists(path), "File không còn trên storage.");
    return new Download(path, s.getFileName(), s.getContentType());
  }
}
