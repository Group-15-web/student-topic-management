package vn.edu.detaihub.controller.api.v1;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.detaihub.dto.*;
import vn.edu.detaihub.service.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TopicRestController {
  private final QueryService query;
  private final TopicService topics;
  private final GroupService groups;

  @GetMapping("/topics")
  public Views.Page<Views.Topic> list(
      @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "1") int page) {
    return QueryService.page(query.topics(), q, page);
  }

  @GetMapping("/topics/{id}")
  public Views.Topic get(@PathVariable Long id) {
    return query.topic(id);
  }

  @PostMapping("/topics")
  @ResponseStatus(HttpStatus.CREATED)
  public Views.Topic create(@Valid @RequestBody Forms.Topic f) {
    f.setId(null);
    return query.topic(topics.save(f));
  }

  @PutMapping("/topics/{id}")
  public Views.Topic update(@PathVariable Long id, @Valid @RequestBody Forms.Topic f) {
    f.setId(id);
    return query.topic(topics.save(f));
  }

  @DeleteMapping("/topics/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    topics.delete(id);
  }

  @PostMapping("/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Long> register(@Valid @RequestBody Forms.Registration f) {
    return Map.of("id", groups.register(f));
  }

  @GetMapping("/results")
  public Object results() {
    return query.results();
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(org.springframework.security.web.csrf.CsrfToken token) {
    return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
  }
}
