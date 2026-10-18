package vn.edu.detaihub.config;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.detaihub.service.EmailService;

@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.mail-worker",
    havingValue = "true",
    matchIfMissing = true)
public class MailWorker {
  private final EmailService mail;

  @Scheduled(fixedDelay = 10000, initialDelay = 10000)
  public void dispatch() {
    for (Long id : mail.pending()) mail.deliver(id);
  }
}
