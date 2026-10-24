package vn.edu.detaihub.config;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.detaihub.service.EmailService;
import vn.edu.detaihub.service.SubmissionService;

@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.mail-worker",
    havingValue = "true",
    matchIfMissing = true)
public class MailWorker {
  private final EmailService mail;
  private final SubmissionService submissions;

  @Scheduled(fixedDelay = 10000, initialDelay = 10000)
  public void dispatch() {
    for (Long id : mail.pending()) mail.deliver(id);
  }

  // Quet va gui email canh bao nhac nho deadline nop bao cao (chay moi 30 phut)
  @Scheduled(fixedDelay = 1800000, initialDelay = 30000)
  public void remindDeadline() {
    submissions.remindUpcomingDeadlines(48); // Nhac nhom con duoi 48h chua nop bai
  }
}