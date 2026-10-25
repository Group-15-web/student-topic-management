package vn.edu.detaihub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import vn.edu.detaihub.repository.UserAccountRepository;

/** Initializes the session's security version after a successful form login. */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
  private final UserAccountRepository users;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    var account = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
    request
        .getSession()
        .setAttribute(SessionVersionFilter.SECURITY_VERSION_ATTRIBUTE, account.getSecurityVersion());
    response.sendRedirect("/");
  }
}
