package vn.edu.detaihub.service;

import static vn.edu.detaihub.exception.RuleException.require;

import java.nio.file.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
public class EmailService {
  private final DomainRepository db;
  private final JavaMailSender sender;
  private final TemplateEngine templates;
  private final ActorService actor;

  @Value("${app.mail-mode}")
  private String mode;

  @Value("${app.mail-dir}")
  private String directory;

  @Value("${app.mail-from}")
  private String from;

  @Value("${app.base-url}")
  private String baseUrl;

  @Async
  @Transactional
  public void deliver(Long id) {
    // Serialize delivery against withdrawal/publication of this registration.
    var initial = db.get(EmailDelivery.class, id);
    var initialResult = db.get(EvaluationResult.class, initial.getResultId());
    db.lock(TopicRegistration.class, initialResult.getRegistrationId());
    var mail = db.lock(EmailDelivery.class, id);
    if (!mail.getStatus().equals("PENDING")) return;
    var result = db.lock(EvaluationResult.class, mail.getResultId());
    if (!result.getStatus().equals("PUBLISHED")
        || !result.getPublicationVersion().equals(mail.getPublicationVersion())) {
      mail.setStatus("CANCELLED");
      return;
    }
    mail.setAttempts(mail.getAttempts() + 1);
    try {
      var context = new Context();
      context.setVariable("body", mail.getBody());
      context.setVariable("url", baseUrl + "/results");
      String html = templates.process("mail/result", context);
      var message = sender.createMimeMessage();
      var helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(mail.getRecipient());
      helper.setSubject(mail.getSubject());
      helper.setText(mail.getBody(), html);
      if (mode.equals("smtp")) {
        sender.send(message);
        mail.setStatus("SENT");
      } else if (mode.equals("preview")) {
        var dir = Path.of(directory).toAbsolutePath();
        Files.createDirectories(dir);
        try (var stream = Files.newOutputStream(dir.resolve(id + ".eml"))) {
          message.writeTo(stream);
        }
        mail.setStatus("PREVIEW");
      } else throw new IllegalStateException("Unsupported mail mode");
      mail.setLastError(null);
    } catch (Exception ex) {
      mail.setStatus("FAILED");
      mail.setLastError(
          "Không gửi được email ("
              + ex.getClass().getSimpleName()
              + "). Kiểm tra cấu hình SMTP hoặc storage, sau đó gửi lại.");
    }
  }

  @Transactional(readOnly = true)
  public List<Long> pending() {
    return db.all(EmailDelivery.class).stream()
        .filter(m -> m.getStatus().equals("PENDING"))
        .limit(30)
        .map(EmailDelivery::getId)
        .toList();
  }

  @Transactional
  public void retry(Long id) {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    var m = db.lock(EmailDelivery.class, id);
    require(
        Set.of("FAILED", "PREVIEW").contains(m.getStatus()),
        "Chỉ thử lại email lỗi hoặc bản xem trước.");
    var r = db.get(EvaluationResult.class, m.getResultId());
    require(
        r.getStatus().equals("PUBLISHED")
            && r.getPublicationVersion().equals(m.getPublicationVersion()),
        "Bản công bố không còn hiện hành.");
    m.setStatus("PENDING");
  }

  @Transactional(readOnly = true)
  public Path preview(Long id) {
    var m = db.get(EmailDelivery.class, id);
    actor.allow(actor.faculty() || m.getRecipientId().equals(actor.user().getId()));
    require(m.getStatus().equals("PREVIEW"), "Không có bản xem trước.");
    return Path.of(directory).toAbsolutePath().resolve(id + ".eml");
  }
}
