package vn.edu.detaihub;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.service.EmailService;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:mailtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "app.mail-dir=target/test-mail",
      "app.mail-worker=false"
    })
class EmailDeliveryTest {
  @Autowired TransactionTemplate tx;
  @Autowired DomainRepository db;
  @Autowired EmailService emails;

  @Test
  void asyncPreviewAndStalePublication() throws Exception {
    Long id =
        tx.execute(
            s -> {
              var r = new EvaluationResult();
              r.setRegistrationId(1L);
              r.setFinalScore(new BigDecimal("8.50"));
              r.setStatus("PUBLISHED");
              r.setPublicationVersion(1);
              r.setPublishedBy(2L);
              r.setPublishedAt(vn.edu.detaihub.service.TopicService.now());
              db.save(r);
              var mail = new EmailDelivery();
              mail.setResultId(r.getId());
              mail.setPublicationVersion(1);
              mail.setRecipientId(1L);
              mail.setRecipient("student@example.invalid");
              mail.setSubject("Kết quả kiểm thử");
              mail.setBody("Điểm trung bình: 8.50 / 10.");
              mail.setStatus("PENDING");
              mail.setAttempts(0);
              db.save(mail);
              return mail.getId();
            });
    emails.deliver(id);
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(tx.<String>execute(s -> db.get(EmailDelivery.class, id).getStatus()))
                    .isEqualTo("PREVIEW"));
    assertThat(Files.readString(Path.of("target/test-mail/" + id + ".eml")))
        .contains("multipart/mixed", "student@example.invalid");
    tx.executeWithoutResult(
        s -> {
          var m = db.get(EmailDelivery.class, id);
          m.setStatus("PENDING");
          db.get(EvaluationResult.class, m.getResultId()).setStatus("WITHDRAWN");
        });
    emails.deliver(id);
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(tx.<String>execute(s -> db.get(EmailDelivery.class, id).getStatus()))
                    .isEqualTo("CANCELLED"));
  }
}
