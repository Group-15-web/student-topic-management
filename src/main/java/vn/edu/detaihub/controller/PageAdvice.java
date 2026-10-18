package vn.edu.detaihub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.detaihub.dto.Views;
import vn.edu.detaihub.service.QueryService;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
@RequiredArgsConstructor
public class PageAdvice {
  private final QueryService query;

  @Value("${app.mail-mode}")
  private String mailMode;

  @Value("${app.seed}")
  private boolean demoSeed;

  @ModelAttribute("me")
  public Views.User me(Authentication auth) {
    return auth == null ? null : query.me();
  }

  @ModelAttribute("mailMode")
  public String mailMode() {
    return mailMode;
  }

  @ModelAttribute("demoSeed")
  public boolean demoSeed() {
    return demoSeed;
  }

  @ModelAttribute("path")
  public String path(jakarta.servlet.http.HttpServletRequest request) {
    return request.getRequestURI();
  }
}
