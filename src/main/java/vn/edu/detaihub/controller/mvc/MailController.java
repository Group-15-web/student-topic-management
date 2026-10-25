package vn.edu.detaihub.controller.mvc;

import static vn.edu.detaihub.controller.support.FormViewSupport.done;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.service.EmailService;
import vn.edu.detaihub.service.QueryService;

@Controller
@RequiredArgsConstructor
public class MailController {
  private final QueryService query;
  private final EmailService mail;

  @GetMapping("/mail")
  public String mails(Model m) {
    m.addAttribute("mails", query.mails());
    return "mail-list";
  }

  @PostMapping("/mail/{id}/retry")
  public String retry(@PathVariable Long id, RedirectAttributes ra) {
    mail.retry(id);
    return done("/mail", ra);
  }

  @GetMapping("/mail/{id}/preview")
  public ResponseEntity<FileSystemResource> preview(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=notification-" + id + ".eml")
        .contentType(MediaType.parseMediaType("message/rfc822"))
        .body(new FileSystemResource(mail.preview(id)));
  }
}
