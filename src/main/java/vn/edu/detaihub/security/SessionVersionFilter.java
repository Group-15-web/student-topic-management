package vn.edu.detaihub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.detaihub.repository.UserAccountRepository;

/** Checks account/session validity without replacing Spring's form authentication. */
@RequiredArgsConstructor
public class SessionVersionFilter extends OncePerRequestFilter {
  public static final String SECURITY_VERSION_ATTRIBUTE = "securityVersion";
  public static final String LOADED_SECURITY_VERSION_ATTRIBUTE = "loadedSecurityVersion";

  private final UserAccountRepository users;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
      var account = users.findByEmailIgnoreCase(authentication.getName()).orElse(null);
      HttpSession session = request.getSession(false);
      Object version = session == null ? null : session.getAttribute(SECURITY_VERSION_ATTRIBUTE);
      if (account == null
          || !account.getEnabled()
          || (version != null && !version.equals(account.getSecurityVersion()))) {
        SecurityContextHolder.clearContext();
        if (session != null) {
          session.invalidate();
        }
      } else {
        Integer loadedVersion =
            session == null
                ? null
                : (Integer) session.getAttribute(LOADED_SECURITY_VERSION_ATTRIBUTE);
        if (loadedVersion == null || !loadedVersion.equals(account.getSecurityVersion())) {
          UserDetails fresh = userDetailsService.loadUserByUsername(authentication.getName());
          SecurityContextHolder.getContext()
              .setAuthentication(
                  new UsernamePasswordAuthenticationToken(
                      fresh, authentication.getCredentials(), fresh.getAuthorities()));
          if (session != null) {
            session.setAttribute(LOADED_SECURITY_VERSION_ATTRIBUTE, account.getSecurityVersion());
          }
        }
      }
    }
    chain.doFilter(request, response);
  }
}
