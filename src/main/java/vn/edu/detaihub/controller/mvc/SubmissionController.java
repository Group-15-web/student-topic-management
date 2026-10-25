package vn.edu.detaihub.controller.mvc;

import static vn.edu.detaihub.controller.support.FormViewSupport.done;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.detaihub.service.SubmissionService;

@Controller
@RequiredArgsConstructor
public class SubmissionController {
  private final SubmissionService submissions;

  @PostMapping("/registrations/{id}/submissions")
  public String upload(
      @PathVariable Long id, @RequestParam MultipartFile file, RedirectAttributes ra)
      throws Exception {
    submissions.upload(id, file);
    return done("/registrations/" + id, ra);
  }

  @GetMapping("/submissions/{id}/download")
  public ResponseEntity<FileSystemResource> download(@PathVariable Long id) {
    var d = submissions.download(id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(d.name(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .contentType(MediaType.parseMediaType(d.contentType()))
        .body(new FileSystemResource(d.path()));
  }
}
